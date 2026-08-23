package equationmatcher.algorithm;

import equationmatcher.model.FormulaResult;
import equationmatcher.model.GeneratedExpression;
import equationmatcher.model.SubstitutionState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EquationMatcherEngine {

    private static final double EPSILON = 1e-9;

    private static final String[] OPERATORS =
            {"-", "+", "*", "/", "^"};

    public enum SequenceType {
        SUMMANDIAL,
        FACTORIAL
    }

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(
                int phase,
                String message,
                double progress
        );
    }

    private int expressionsSearched;
    private int operatorPatternsSearched;
    private int survivingPaths;

    private ProgressListener progressListener;

    // ============================================================
    // PROGRESS
    // ============================================================

    public void setProgressListener(
            ProgressListener progressListener) {

        this.progressListener =
                progressListener;
    }

    private void reportProgress(
            int phase,
            String message,
            double progress) {

        if (progressListener != null) {

            progressListener.onProgress(
                    phase,
                    message,
                    Math.max(
                            0,
                            Math.min(
                                    1,
                                    progress
                            )
                    )
            );
        }
    }

    // ============================================================
    // PUBLIC SEARCH METHOD
    // ============================================================

    public List<FormulaResult> search(
            int input,
            int maximumTerms,
            SequenceType sequenceType) {

        expressionsSearched = 0;
        operatorPatternsSearched = 0;
        survivingPaths = 0;

        int minimumTermValue = 1;
        int maximumTermValue = 4;

        // ========================================================
        // PHASE 1
        // ========================================================

        reportProgress(
                1,
                "Generating expression space...",
                0
        );

        List<List<GeneratedExpression>> allExpressions =
                generateAllExpressions(
                        maximumTerms,
                        minimumTermValue,
                        maximumTermValue
                );

        reportProgress(
                1,
                "Expression space complete",
                1
        );

        // ========================================================
        // PHASE 2
        // ========================================================

        reportProgress(
                2,
                "Matching parto-factoriands...",
                0
        );

        List<List<GeneratedExpression>> solutionsFound =
                findPartoFactoriandSolutions(
                        input,
                        allExpressions,
                        sequenceType
                );

        reportProgress(
                2,
                "Parto-factoriand matching complete",
                1
        );

        // ========================================================
        // PHASE 3
        // ========================================================

        reportProgress(
                3,
                "Exploring substitution paths...",
                0
        );

        List<SubstitutionState> substitutionStates =
                generateAllSubstitutionPaths(
                        solutionsFound,
                        input,
                        sequenceType
                );

        survivingPaths =
                substitutionStates.size();

        reportProgress(
                3,
                "Substitution search complete",
                1
        );

        // ========================================================
        // PHASE 4
        // ========================================================

        reportProgress(
                4,
                "Constructing symbolic formulas...",
                0
        );

        List<FormulaResult> results =
                findSymbolicFormulas(
                        substitutionStates,
                        input
                );

        reportProgress(
                4,
                "Formula construction complete",
                1
        );

        return results;
    }

    // ============================================================
    // SEARCH STATISTICS
    // ============================================================

    public int getExpressionsSearched() {
        return expressionsSearched;
    }

    public int getOperatorPatternsSearched() {
        return operatorPatternsSearched;
    }

    public int getSurvivingPaths() {
        return survivingPaths;
    }

    // ============================================================
    // SEQUENCES
    // ============================================================

    public int calculateSummandial(
            int input) {

        int result = 0;

        for (int i = 1; i <= input; i++) {
            result += i;
        }

        return result;
    }

    public int calculateFactorial(
            int input) {

        int result = 1;

        for (int i = 1; i <= input; i++) {
            result *= i;
        }

        return result;
    }

    private int calculateSequenceValue(
            int input,
            SequenceType sequenceType) {

        switch (sequenceType) {

            case SUMMANDIAL:
                return calculateSummandial(input);

            case FACTORIAL:
                return calculateFactorial(input);

            default:
                throw new IllegalArgumentException(
                        "Unsupported sequence type: "
                                + sequenceType
                );
        }
    }

    // ============================================================
    // PHASE 1
    // ============================================================

    private List<List<GeneratedExpression>>
    generateAllExpressions(
            int maximumTerms,
            int minimumValue,
            int maximumValue) {

        List<List<GeneratedExpression>> allExpressions =
                new ArrayList<>();

        int totalPatterns = 0;

        for (int termCount = 1;
             termCount <= maximumTerms;
             termCount++) {

            totalPatterns +=
                    integerPower(
                            OPERATORS.length,
                            termCount - 1
                    );
        }

        int completedPatterns = 0;

        for (int termCount = 1;
             termCount <= maximumTerms;
             termCount++) {

            List<GeneratedExpression> expressionGroup =
                    new ArrayList<>();

            List<List<String>> operatorPatterns =
                    generateOperatorPatterns(
                            termCount - 1
                    );

            operatorPatternsSearched +=
                    operatorPatterns.size();

            for (List<String> operatorPattern
                    : operatorPatterns) {

                generateNumericStates(
                        termCount,
                        minimumValue,
                        maximumValue,
                        new ArrayList<>(),
                        operatorPattern,
                        expressionGroup
                );

                completedPatterns++;

                reportProgress(
                        1,
                        "Generating expressions: operator pattern "
                                + completedPatterns
                                + " of "
                                + totalPatterns,
                        (double) completedPatterns
                                / totalPatterns
                );
            }

            allExpressions.add(
                    expressionGroup
            );
        }

        return allExpressions;
    }

    private List<List<String>>
    generateOperatorPatterns(
            int operatorCount) {

        List<List<String>> patterns =
                new ArrayList<>();

        generateOperatorPatternsRecursive(
                operatorCount,
                new ArrayList<>(),
                patterns
        );

        return patterns;
    }

    private void generateOperatorPatternsRecursive(
            int operatorCount,
            List<String> currentPattern,
            List<List<String>> patterns) {

        if (currentPattern.size()
                == operatorCount) {

            patterns.add(
                    new ArrayList<>(
                            currentPattern
                    )
            );

            return;
        }

        for (String operator
                : OPERATORS) {

            currentPattern.add(
                    operator
            );

            generateOperatorPatternsRecursive(
                    operatorCount,
                    currentPattern,
                    patterns
            );

            currentPattern.remove(
                    currentPattern.size() - 1
            );
        }
    }

    private void generateNumericStates(
            int termCount,
            int minimumValue,
            int maximumValue,
            List<Double> currentTerms,
            List<String> operators,
            List<GeneratedExpression> expressions) {

        if (currentTerms.size()
                == termCount) {

            double value =
                    evaluateExpression(
                            currentTerms,
                            operators
                    );

            expressions.add(
                    new GeneratedExpression(
                            currentTerms,
                            operators,
                            value
                    )
            );

            expressionsSearched++;

            return;
        }

        for (int value = minimumValue;
             value <= maximumValue;
             value++) {

            currentTerms.add(
                    (double) value
            );

            generateNumericStates(
                    termCount,
                    minimumValue,
                    maximumValue,
                    currentTerms,
                    operators,
                    expressions
            );

            currentTerms.remove(
                    currentTerms.size() - 1
            );
        }
    }

    // ============================================================
    // PHASE 2
    // ============================================================

    private List<List<GeneratedExpression>>
    findPartoFactoriandSolutions(
            int input,
            List<List<GeneratedExpression>> allExpressions,
            SequenceType sequenceType) {

        List<List<GeneratedExpression>> solutionsFound =
                new ArrayList<>();

        for (int c = 1;
             c <= input;
             c++) {

            int target =
                    calculateSequenceValue(
                            c,
                            sequenceType
                    );

            List<GeneratedExpression>
                    partoFactoriandSolutions =
                    new ArrayList<>();

            for (List<GeneratedExpression> group
                    : allExpressions) {

                for (GeneratedExpression expression
                        : group) {

                    if (valuesEqual(
                            expression.getValue(),
                            target)) {

                        partoFactoriandSolutions.add(
                                expression
                        );
                    }
                }
            }

            solutionsFound.add(
                    partoFactoriandSolutions
            );

            reportProgress(
                    2,
                    "Matching parto-factoriand "
                            + c
                            + " of "
                            + input,
                    (double) c / input
            );
        }

        return solutionsFound;
    }

    // ============================================================
    // PHASE 3
    // ============================================================

    private List<SubstitutionState>
    generateAllSubstitutionPaths(
            List<List<GeneratedExpression>> solutionsFound,
            int input,
            SequenceType sequenceType) {

        if (solutionsFound.isEmpty()) {
            return new ArrayList<>();
        }

        List<SubstitutionState> currentStates =
                new ArrayList<>();

        for (GeneratedExpression startingExpression
                : solutionsFound.get(0)) {

            SubstitutionState startingState =
                    new SubstitutionState(
                            startingExpression.getTerms(),
                            startingExpression.getOperators()
                    );

            List<String> history =
                    new ArrayList<>();

            history.add(
                    buildExpression(
                            startingExpression.getTerms(),
                            startingExpression.getOperators()
                    )
            );

            startingState =
                    new SubstitutionState(
                            startingExpression.getTerms(),
                            startingExpression.getOperators(),
                            startingState.getIncrementHistory(),
                            history
                    );

            currentStates.add(
                    startingState
            );
        }

        int transitionCount =
                Math.max(
                        1,
                        input - 1
                );

        for (int c = 1;
             c < input;
             c++) {

            int nextTarget =
                    calculateSequenceValue(
                            c + 1,
                            sequenceType
                    );

            Map<String, SubstitutionState> nextStates =
                    new LinkedHashMap<>();

            for (SubstitutionState state
                    : currentStates) {

                List<SubstitutionState> generatedStates =
                        generateNextStates(
                                state,
                                c,
                                nextTarget
                        );

                for (SubstitutionState generatedState
                        : generatedStates) {

                    nextStates.putIfAbsent(
                            createStateKey(
                                    generatedState
                            ),
                            generatedState
                    );
                }
            }

            currentStates =
                    new ArrayList<>(
                            nextStates.values()
                    );

            reportProgress(
                    3,
                    "Testing substitutions "
                            + c
                            + " → "
                            + (c + 1)
                            + "    •    "
                            + currentStates.size()
                            + " surviving paths",
                    (double) c
                            / transitionCount
            );

            if (currentStates.isEmpty()) {
                break;
            }
        }

        return currentStates;
    }

    private List<SubstitutionState>
    generateNextStates(
            SubstitutionState state,
            int currentPartoFactoriand,
            int targetValue) {

        List<Integer> eligiblePositions =
                new ArrayList<>();

        List<Double> currentTerms =
                state.getTerms();

        for (int position = 0;
             position < currentTerms.size();
             position++) {

            if (valuesEqual(
                    currentTerms.get(position),
                    currentPartoFactoriand)) {

                eligiblePositions.add(
                        position
                );
            }
        }

        List<SubstitutionState> validStates =
                new ArrayList<>();

        int combinationCount =
                1 << eligiblePositions.size();

        for (int mask = 0;
             mask < combinationCount;
             mask++) {

            List<Double> newTerms =
                    new ArrayList<>(
                            currentTerms
                    );

            List<List<Integer>> newHistory =
                    state.getIncrementHistory();

            for (int bit = 0;
                 bit < eligiblePositions.size();
                 bit++) {

                if ((mask & (1 << bit)) == 0) {
                    continue;
                }

                int position =
                        eligiblePositions.get(bit);

                newTerms.set(
                        position,
                        newTerms.get(position) + 1
                );

                newHistory
                        .get(position)
                        .add(
                                currentPartoFactoriand
                        );
            }

            double value =
                    evaluateExpression(
                            newTerms,
                            state.getOperators()
                    );

            if (!valuesEqual(
                    value,
                    targetValue)) {

                continue;
            }

            List<String> expressionHistory =
                    state.getExpressionHistory();

            expressionHistory.add(
                    buildExpression(
                            newTerms,
                            state.getOperators()
                    )
            );

            validStates.add(
                    new SubstitutionState(
                            newTerms,
                            state.getOperators(),
                            newHistory,
                            expressionHistory
                    )
            );
        }

        return validStates;
    }

    // ============================================================
    // PHASE 4
    // ============================================================

    private List<FormulaResult>
    findSymbolicFormulas(
            List<SubstitutionState> states,
            int input) {

        Map<String, FormulaResult> formulas =
                new LinkedHashMap<>();

        int totalStates =
                Math.max(
                        1,
                        states.size()
                );

        int processedStates = 0;

        for (SubstitutionState state
                : states) {

            String formula =
                    buildSymbolicFormula(
                            state,
                            input
                    );

            if (formula != null) {

                formulas.putIfAbsent(
                        formula,
                        new FormulaResult(
                                formula,
                                state.getExpressionHistory(),
                                state.getIncrementHistory()
                        )
                );
            }

            processedStates++;

            /*
             * Avoid flooding the GUI with thousands of tiny
             * progress events. Update periodically instead.
             */
            if (processedStates % 100 == 0
                    || processedStates == totalStates) {

                reportProgress(
                        4,
                        "Building formulas: "
                                + processedStates
                                + " of "
                                + totalStates,
                        (double) processedStates
                                / totalStates
                );
            }
        }

        return new ArrayList<>(
                formulas.values()
        );
    }

    private String buildSymbolicFormula(
            SubstitutionState state,
            int input) {

        List<Integer> requiredHistory =
                new ArrayList<>();

        for (int c = 1;
             c < input;
             c++) {

            requiredHistory.add(c);
        }

        List<String> symbolicTerms =
                new ArrayList<>();

        List<Double> finalTerms =
                state.getTerms();

        List<List<Integer>> histories =
                state.getIncrementHistory();

        for (int i = 0;
             i < histories.size();
             i++) {

            List<Integer> history =
                    histories.get(i);

            if (history.equals(
                    requiredHistory)) {

                symbolicTerms.add("n");

            } else if (history.isEmpty()) {

                symbolicTerms.add(
                        formatNumber(
                                finalTerms.get(i)
                        )
                );

            } else {

                return null;
            }
        }

        return buildSymbolicExpression(
                symbolicTerms,
                state.getOperators()
        );
    }

    // ============================================================
    // UTILITIES
    // ============================================================

    private double evaluateExpression(
            List<Double> terms,
            List<String> operators) {

        if (terms.isEmpty()) {

            throw new IllegalArgumentException(
                    "Expression must contain at least one term."
            );
        }

        double result =
                terms.get(0);

        for (int i = 0;
             i < operators.size();
             i++) {

            result =
                    applyOperator(
                            result,
                            operators.get(i),
                            terms.get(i + 1)
                    );
        }

        return result;
    }

    private double applyOperator(
            double currentValue,
            String operator,
            double newTerm) {

        switch (operator) {

            case "-":
                return currentValue - newTerm;

            case "+":
                return currentValue + newTerm;

            case "*":
                return currentValue * newTerm;

            case "/":
                return currentValue / newTerm;

            case "^":
                return Math.pow(
                        currentValue,
                        newTerm
                );

            default:
                throw new IllegalArgumentException(
                        "Unsupported operator: "
                                + operator
                );
        }
    }

    private String buildExpression(
            List<Double> terms,
            List<String> operators) {

        StringBuilder expression =
                new StringBuilder();

        expression.append(
                formatNumber(
                        terms.get(0)
                )
        );

        for (int i = 0;
             i < operators.size();
             i++) {

            expression
                    .append(" ")
                    .append(operators.get(i))
                    .append(" ")
                    .append(
                            formatNumber(
                                    terms.get(i + 1)
                            )
                    );
        }

        return expression.toString();
    }

    private String buildSymbolicExpression(
            List<String> terms,
            List<String> operators) {

        StringBuilder expression =
                new StringBuilder();

        expression.append(
                terms.get(0)
        );

        for (int i = 0;
             i < operators.size();
             i++) {

            expression
                    .append(" ")
                    .append(operators.get(i))
                    .append(" ")
                    .append(
                            terms.get(i + 1)
                    );
        }

        return expression.toString();
    }

    private String createStateKey(
            SubstitutionState state) {

        return state.getOperators()
                + "|"
                + state.getTerms()
                + "|"
                + state.getIncrementHistory();
    }

    private boolean valuesEqual(
            double first,
            double second) {

        return Math.abs(
                first - second
        ) < EPSILON;
    }

    private String formatNumber(
            double number) {

        if (number == Math.rint(number)) {

            return Long.toString(
                    Math.round(number)
            );
        }

        return Double.toString(
                number
        );
    }

    private int integerPower(
            int base,
            int exponent) {

        int result = 1;

        for (int i = 0;
             i < exponent;
             i++) {

            result *= base;
        }

        return result;
    }
}