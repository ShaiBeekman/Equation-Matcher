package equationmatcher.model;

import java.util.ArrayList;
import java.util.List;

public class SubstitutionState {

    private final List<Double> terms;
    private final List<String> operators;
    private final List<List<Integer>> incrementHistory;
    private final List<String> expressionHistory;

    public SubstitutionState(
            List<Double> terms,
            List<String> operators) {

        this.terms = new ArrayList<>(terms);
        this.operators = new ArrayList<>(operators);

        this.incrementHistory = new ArrayList<>();

        for (int i = 0; i < terms.size(); i++) {
            incrementHistory.add(new ArrayList<>());
        }

        this.expressionHistory = new ArrayList<>();
    }

    public SubstitutionState(
            List<Double> terms,
            List<String> operators,
            List<List<Integer>> incrementHistory,
            List<String> expressionHistory) {

        this.terms = new ArrayList<>(terms);
        this.operators = new ArrayList<>(operators);

        this.incrementHistory =
                deepCopyHistory(incrementHistory);

        this.expressionHistory =
                new ArrayList<>(expressionHistory);
    }

    public List<Double> getTerms() {
        return new ArrayList<>(terms);
    }

    public List<String> getOperators() {
        return new ArrayList<>(operators);
    }

    public List<List<Integer>> getIncrementHistory() {
        return deepCopyHistory(incrementHistory);
    }

    public List<String> getExpressionHistory() {
        return new ArrayList<>(expressionHistory);
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