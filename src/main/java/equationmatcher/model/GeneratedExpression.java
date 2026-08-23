package equationmatcher.model;

import java.util.ArrayList;
import java.util.List;

public class GeneratedExpression {

    private final List<Double> terms;
    private final List<String> operators;
    private final double value;

    public GeneratedExpression(
            List<Double> terms,
            List<String> operators,
            double value) {

        this.terms = new ArrayList<>(terms);
        this.operators = new ArrayList<>(operators);
        this.value = value;
    }

    public List<Double> getTerms() {
        return new ArrayList<>(terms);
    }

    public List<String> getOperators() {
        return new ArrayList<>(operators);
    }

    public double getValue() {
        return value;
    }
}