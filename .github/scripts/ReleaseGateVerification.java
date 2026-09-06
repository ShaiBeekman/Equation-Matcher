import equationmatcher.algorithm.EquationMatcherEngine;
import equationmatcher.algorithm.execution.*;
import equationmatcher.target.*;
import equationmatcher.verification.*;
import javax.tools.ToolProvider;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** External test harness. Compiled by the release JDK against the release JAR only. */
public final class ReleaseGateVerification {
    private static final AlgorithmSourceCompiler COMPILER = new AlgorithmSourceCompiler();
    private static Path image;
    private static Path java;
    public static void main(String[] args) throws Exception {
        image = Path.of(args[0]).toRealPath();
        java = image.resolve("runtime/bin/java.exe").toRealPath();
        check(Files.isSameFile(image.resolve("runtime"), Path.of(System.getProperty("java.home"))), "Bundled java.home");
        check(ToolProvider.getSystemJavaCompiler() != null, "ToolProvider.getSystemJavaCompiler() != null");
        for (String module : List.of("java.compiler", "jdk.compiler", "javafx.base", "javafx.graphics", "javafx.controls"))
            check(ModuleLayer.boot().findModule(module).isPresent(), "Runtime module " + module);
        for (String name : List.of("equationmatcher.algorithm.EquationMatcherEngine", "org.fxmisc.richtext.CodeArea",
                "org.reactfx.EventStream", "org.fxmisc.undo.UndoManager", "org.fxmisc.flowless.VirtualFlow",
                "org.fxmisc.wellbehaved.event.InputMap")) {
            Class<?> type = Class.forName(name, false, ReleaseGateVerification.class.getClassLoader());
            Path location = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
            check(location.startsWith(image.resolve("app")), name + " loaded from extracted image: " + location);
        }
        System.out.println("BUNDLED_JAVA=" + java);
        System.out.println("JAVA_VERSION=" + System.getProperty("java.version"));
        var engine = new EquationMatcherEngine();
        check(engine.search(4, 4, EquationMatcherEngine.SequenceType.SUMMANDIAL).size() == 6, "Summandial: six candidates");
        check(engine.search(4, 4, EquationMatcherEngine.SequenceType.FACTORIAL).isEmpty(), "Factorial: completed, zero candidates at defaults");
        var sequence = new TargetSearchService().search(engine, TargetSearchRequest.forSequence("1, 3, 6, 10, 15"), 4);
        check(sequence.formulas().size() == 6, "Entered sequence: six candidates");
        String triangular = "long sum = 0;\nfor (int i = 1; i <= n; i++) { sum += i; }\nreturn sum;";
        try (var target = target(triangular)) {
            check(Arrays.equals(TargetSampler.sample(target, 1, 5), new double[]{1, 3, 6, 10, 15}), "Triangular sampled values");
        }
        var result = new TargetSearchService().search(engine, new TargetSearchRequest(target(triangular), 8, 25), 4);
        check(result.formulas().size() == 6 && result.samples().size() == 33, "Triangular: six candidates, 33 samples");
        check(result.verification().values().stream().allMatch(v -> v.discoveryMatched() == 8
                && v.verificationMatched() == 25 && v.testedThrough() == 33), "Triangular: discovery 8/8, verification 25/25 through n=33");
        check(result.samples().stream().filter(TargetSample::discovery).count() == 8, "Only eight discovery samples");
        squareSearch("Square");
        long workerPid;
        try (var target = target("return ProcessHandle.current().pid();")) {
            workerPid = (long) target.valueAt(1);
            var worker = ProcessHandle.of(workerPid).orElseThrow();
            check(Files.isSameFile(java, Path.of(worker.info().command().orElseThrow())), "AlgorithmWorker uses bundled Java");
            System.out.println("ALGORITHM_WORKER_JAVA=" + worker.info().command().orElseThrow());
        }
        check(!ProcessHandle.of(workerPid).map(ProcessHandle::isAlive).orElse(false), "Worker terminated on close");
        timeout();
        squareSearch("Recovery after timeout");
        check(workers().isEmpty(), "No packaged AlgorithmWorker descendants remain");
        System.out.println("PASS: ALL PACKAGED BACKEND/RUNTIME CHECKS");
    }
    private static AlgorithmTarget target(String source) {
        var compilation = COMPILER.compile(source);
        if (!compilation.isValid()) throw new AssertionError("Compilation failed: " + compilation.diagnostics());
        return new AlgorithmTarget(compilation.compiled());
    }
    private static void squareSearch(String description) throws Exception {
        try (var target = target("return n * n;")) {
            check(Arrays.equals(TargetSampler.sample(target, 1, 5), new double[]{1, 4, 9, 16, 25}), description + ": square sampled values");
        }
        var result = new TargetSearchService().search(new EquationMatcherEngine(),
                new TargetSearchRequest(target("return n * n;"), 8, 25), 4);
        check(!result.formulas().isEmpty() && result.verification().values().stream().anyMatch(CandidateVerification::allMatched),
                description + ": compilation, execution, discovery and independent verification succeeded (" + result.formulas().size() + " candidates)");
    }
    private static void timeout() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try (var target = target("while (true) { }")) {
            Future<Double> pending = executor.submit(() -> target.valueAt(1));
            ProcessHandle worker = null;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (worker == null && System.nanoTime() < deadline && !pending.isDone()) {
                worker = workers().stream().findFirst().orElse(null);
                if (worker == null) Thread.sleep(10);
            }
            check(worker != null, "Observed infinite-loop worker");
            try { pending.get(15, TimeUnit.SECONDS); throw new AssertionError("Infinite loop returned"); }
            catch (ExecutionException failure) {
                check(failure.getCause() instanceof AlgorithmExecutionException error
                        && error.reason() == AlgorithmExecutionException.Reason.TIMEOUT, "Infinite loop times out cleanly");
            }
            check(!worker.isAlive(), "Timed-out child terminated");
        } finally { executor.shutdownNow(); }
    }
    private static List<ProcessHandle> workers() {
        return ProcessHandle.current().descendants().filter(p -> p.info().command().map(c -> {
            try { return Files.isSameFile(java, Path.of(c)); } catch (Exception e) { return false; }
        }).orElse(false)).toList();
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("PASS: " + message);
    }
}
