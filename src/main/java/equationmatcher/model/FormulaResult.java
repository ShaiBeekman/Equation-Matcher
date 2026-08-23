package equationmatcher.model;

import java.util.ArrayList;
import java.util.List;

public class FormulaResult {

    private final String formula;
    private final List<String> verificationPath;
    private final List<List<Integer>> incrementHistory;

    public FormulaResult(
            String formula,
            List<String> verificationPath,
            List<List<Integer>> incrementHistory) {

        this.formula = formula;

        this.verificationPath =
                new ArrayList<>(verificationPath);

        this.incrementHistory =
                deepCopyHistory(incrementHistory);
    }

    public String getFormula() {
        return formula;
    }

    public List<String> getVerificationPath() {
        return new ArrayList<>(verificationPath);
    }

    public List<List<Integer>> getIncrementHistory() {
        return deepCopyHistory(incrementHistory);
    }

    private static List<List<Integer>> deepCopyHistory(
            List<List<Integer>> history) {

        List<List<Integer>> copy =
                new ArrayList<>();

        for (List<Integer> termHistory : history) {
            copy.add(new ArrayList<>(termHistory));
        }

        return copy;
    }
}