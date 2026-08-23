package equationmatcher.gui;

import equationmatcher.algorithm.EquationMatcherEngine;
import equationmatcher.model.FormulaResult;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

public class MainView {

    private final BorderPane root;

    private final EquationMatcherEngine engine =
            new EquationMatcherEngine();

    private ComboBox<String> sequenceSelector;
    private Spinner<Integer> inputSpinner;
    private Spinner<Integer> termsSpinner;

    private Label expressionsValue;
    private Label patternsValue;
    private Label pathsValue;
    private Label formulasValue;

    private Label statusLabel;
    private Button runButton;

    private TableView<FormulaRow> resultsTable;

    private ProgressIndicator searchProgress;
    private ProgressBar phaseProgressBar;

    private Label phaseLabel;
    private Label phaseMessage;

    private Label visualizationTitle;
    private Label visualizationFormula;
    private Label visualizationExplanation;

    private VBox pathContainer;
    private VBox progressPanel;

    public MainView() {

        root = new BorderPane();

        root.getStyleClass().add(
                "root"
        );

        root.setTop(
                createHeader()
        );

        root.setLeft(
                createSearchPanel()
        );

        root.setCenter(
                createMainContent()
        );
    }

    // ============================================================
    // HEADER
    // ============================================================

    private Parent createHeader() {

        Label logo =
                new Label("∑");

        logo.getStyleClass().add(
                "app-logo"
        );

        Label title =
                new Label(
                        "Equation Matcher"
                );

        title.getStyleClass().add(
                "app-title"
        );

        Label subtitle =
                new Label(
                        "Symbolic formula discovery through exhaustive expression search"
                );

        subtitle.getStyleClass().add(
                "app-subtitle"
        );

        VBox titleBox =
                new VBox(
                        2,
                        title,
                        subtitle
                );

        HBox branding =
                new HBox(
                        14,
                        logo,
                        titleBox
                );

        branding.setAlignment(
                Pos.CENTER_LEFT
        );

        Region spacer =
                new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        statusLabel =
                new Label();

        statusLabel
                .getStyleClass()
                .add("status-label");

        setReadyStatus();

        HBox header =
                new HBox(
                        20,
                        branding,
                        spacer,
                        statusLabel
                );

        header.setAlignment(
                Pos.CENTER_LEFT
        );

        header
                .getStyleClass()
                .add("app-header");

        return header;
    }

    // ============================================================
    // SEARCH PANEL
    // ============================================================

    private Parent createSearchPanel() {

        Label heading =
                createSectionHeading(
                        "SEARCH CONFIGURATION"
                );

        Label sequenceLabel =
                createFieldLabel(
                        "Sequence"
                );

        sequenceSelector =
                new ComboBox<>();

        sequenceSelector
                .getItems()
                .addAll(
                        "Summandial",
                        "Factorial"
                );

        sequenceSelector.setValue(
                "Summandial"
        );

        sequenceSelector.setMaxWidth(
                Double.MAX_VALUE
        );

        Label inputLabel =
                createFieldLabel(
                        "Verification range"
                );

        inputSpinner =
                new Spinner<>(
                        2,
                        10,
                        4
                );

        inputSpinner.setEditable(
                true
        );

        inputSpinner.setMaxWidth(
                Double.MAX_VALUE
        );

        Label termsLabel =
                createFieldLabel(
                        "Maximum terms"
                );

        termsSpinner =
                new Spinner<>(
                        1,
                        6,
                        4
                );

        termsSpinner.setEditable(
                true
        );

        termsSpinner.setMaxWidth(
                Double.MAX_VALUE
        );

        Separator separator =
                new Separator();

        Label operatorsHeading =
                createSectionHeading(
                        "OPERATORS"
                );

        HBox operators =
                new HBox(
                        7,
                        createOperatorBadge("+"),
                        createOperatorBadge("-"),
                        createOperatorBadge("×"),
                        createOperatorBadge("÷"),
                        createOperatorBadge("^")
                );

        runButton =
                new Button(
                        "▶   Run Search"
                );

        runButton
                .getStyleClass()
                .add("run-button");

        runButton.setMaxWidth(
                Double.MAX_VALUE
        );

        runButton.setPrefHeight(
                46
        );

        runButton.setOnAction(
                event -> runSearch()
        );

        Label searchNote =
                new Label(
                        "The search evaluates expressions left-to-right " +
                                "and tracks valid substitutions across sequence inputs."
                );

        searchNote.setWrapText(
                true
        );

        searchNote
                .getStyleClass()
                .add("search-note");

        VBox panel =
                new VBox(
                        12,
                        heading,
                        createVerticalSpace(5),
                        sequenceLabel,
                        sequenceSelector,
                        createVerticalSpace(4),
                        inputLabel,
                        inputSpinner,
                        createVerticalSpace(4),
                        termsLabel,
                        termsSpinner,
                        createVerticalSpace(8),
                        separator,
                        createVerticalSpace(5),
                        operatorsHeading,
                        operators,
                        createVerticalSpace(14),
                        runButton,
                        searchNote
                );

        panel.setPrefWidth(
                280
        );

        panel
                .getStyleClass()
                .add("search-panel");

        return panel;
    }

    // ============================================================
    // MAIN CONTENT
    // ============================================================

    private Parent createMainContent() {

        VBox content =
                new VBox(
                        18
                );

        content.setPadding(
                new Insets(24)
        );

        Parent statistics =
                createStatisticsRow();

        Parent visualization =
                createSearchVisualization();

        Parent results =
                createResultsPanel();

        content
                .getChildren()
                .addAll(
                        statistics,
                        visualization,
                        results
                );

        VBox.setVgrow(
                visualization,
                Priority.ALWAYS
        );

        ScrollPane scrollPane =
                new ScrollPane(
                        content
                );

        scrollPane.setFitToWidth(
                true
        );

        scrollPane
                .getStyleClass()
                .add("content-scroll");

        return scrollPane;
    }

    // ============================================================
    // STATISTICS
    // ============================================================

    private Parent createStatisticsRow() {

        HBox row =
                new HBox(
                        12
                );

        VBox expressions =
                createStatisticCard(
                        "EXPRESSIONS SEARCHED",
                        "expressions"
                );

        VBox patterns =
                createStatisticCard(
                        "OPERATOR PATTERNS",
                        "patterns"
                );

        VBox paths =
                createStatisticCard(
                        "SURVIVING PATHS",
                        "paths"
                );

        VBox formulas =
                createStatisticCard(
                        "FORMULAS FOUND",
                        "formulas"
                );

        HBox.setHgrow(
                expressions,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                patterns,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                paths,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                formulas,
                Priority.ALWAYS
        );

        row
                .getChildren()
                .addAll(
                        expressions,
                        patterns,
                        paths,
                        formulas
                );

        return row;
    }

    private VBox createStatisticCard(
            String name,
            String type) {

        Label nameLabel =
                new Label(name);

        nameLabel
                .getStyleClass()
                .add("stat-name");

        Label valueLabel =
                new Label("—");

        valueLabel
                .getStyleClass()
                .add("stat-value");

        switch (type) {

            case "expressions":
                expressionsValue =
                        valueLabel;
                break;

            case "patterns":
                patternsValue =
                        valueLabel;
                break;

            case "paths":
                pathsValue =
                        valueLabel;
                break;

            case "formulas":
                formulasValue =
                        valueLabel;
                break;
        }

        VBox card =
                new VBox(
                        7,
                        nameLabel,
                        valueLabel
                );

        card.setPadding(
                new Insets(16)
        );

        card.setMaxWidth(
                Double.MAX_VALUE
        );

        card
                .getStyleClass()
                .add("card");

        return card;
    }

    // ============================================================
    // VISUALIZATION
    // ============================================================

    private Parent createSearchVisualization() {

        Label heading =
                createSectionHeading(
                        "SUBSTITUTION PATH"
                );

        visualizationTitle =
                new Label(
                        "Ready to discover a pattern"
                );

        visualizationTitle
                .getStyleClass()
                .add(
                        "visualization-title"
                );

        visualizationFormula =
                new Label(
                        "[ 1 ]   +   [ 1 ]   ×   [ 1 ]   ÷   [ 2 ]"
                );

        visualizationFormula
                .getStyleClass()
                .add(
                        "visualization-formula"
                );

        visualizationExplanation =
                new Label(
                        "Run the search to visualize how numerical " +
                                "positions evolve into symbolic variables."
                );

        visualizationExplanation.setWrapText(
                true
        );

        visualizationExplanation.setMaxWidth(
                700
        );

        visualizationExplanation.setAlignment(
                Pos.CENTER
        );

        visualizationExplanation
                .getStyleClass()
                .add(
                        "visualization-explanation"
                );

        pathContainer =
                new VBox(
                        8
                );

        pathContainer.setAlignment(
                Pos.CENTER
        );

        VBox normalContent =
                new VBox(
                        15,
                        visualizationTitle,
                        visualizationFormula,
                        visualizationExplanation,
                        pathContainer
                );

        normalContent.setAlignment(
                Pos.CENTER
        );

        searchProgress =
                new ProgressIndicator();

        searchProgress.setPrefSize(
                48,
                48
        );

        phaseLabel =
                new Label(
                        "PHASE 1 OF 4"
                );

        phaseLabel
                .getStyleClass()
                .add(
                        "phase-label"
                );

        phaseMessage =
                new Label(
                        "Preparing search..."
                );

        phaseMessage
                .getStyleClass()
                .add(
                        "phase-message"
                );

        phaseProgressBar =
                new ProgressBar(
                        0
                );

        phaseProgressBar.setPrefWidth(
                420
        );

        phaseProgressBar
                .getStyleClass()
                .add(
                        "phase-progress"
                );

        progressPanel =
                new VBox(
                        14,
                        searchProgress,
                        phaseLabel,
                        phaseMessage,
                        phaseProgressBar
                );

        progressPanel.setAlignment(
                Pos.CENTER
        );

        progressPanel.setVisible(
                false
        );

        StackPane visualizationPane =
                new StackPane(
                        normalContent,
                        progressPanel
                );

        visualizationPane.setMinHeight(
                300
        );

        VBox card =
                new VBox(
                        15,
                        heading,
                        visualizationPane
                );

        card.setPadding(
                new Insets(20)
        );

        card.setMinHeight(
                370
        );

        card
                .getStyleClass()
                .add("card");

        return card;
    }

    // ============================================================
    // RESULTS
    // ============================================================

    private Parent createResultsPanel() {

        Label heading =
                createSectionHeading(
                        "DISCOVERED FORMULAS"
                );

        resultsTable =
                new TableView<>();

        TableColumn<FormulaRow, String> formulaColumn =
                new TableColumn<>(
                        "Formula"
                );

        formulaColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "formula"
                )
        );

        TableColumn<FormulaRow, String> rangeColumn =
                new TableColumn<>(
                        "Verified Range"
                );

        rangeColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "range"
                )
        );

        TableColumn<FormulaRow, String> statusColumn =
                new TableColumn<>(
                        "Status"
                );

        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "status"
                )
        );

        formulaColumn.setPrefWidth(
                400
        );

        rangeColumn.setPrefWidth(
                180
        );

        statusColumn.setPrefWidth(
                130
        );

        resultsTable
                .getColumns()
                .addAll(
                        formulaColumn,
                        rangeColumn,
                        statusColumn
                );

        resultsTable.setPrefHeight(
                220
        );

        resultsTable.setPlaceholder(
                new Label(
                        "No formulas yet — run a search to begin."
                )
        );

        resultsTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable,
                         oldSelection,
                         newSelection) -> {

                            if (newSelection != null) {

                                displayFormulaResult(
                                        newSelection
                                                .getResult()
                                );
                            }
                        }
                );

        VBox card =
                new VBox(
                        14,
                        heading,
                        resultsTable
                );

        card.setPadding(
                new Insets(20)
        );

        card
                .getStyleClass()
                .add("card");

        return card;
    }

    // ============================================================
    // SEARCH
    // ============================================================

    private void runSearch() {

        int input =
                inputSpinner.getValue();

        int maximumTerms =
                termsSpinner.getValue();

        EquationMatcherEngine.SequenceType sequenceType;

        if ("Factorial".equals(
                sequenceSelector.getValue())) {

            sequenceType =
                    EquationMatcherEngine
                            .SequenceType
                            .FACTORIAL;

        } else {

            sequenceType =
                    EquationMatcherEngine
                            .SequenceType
                            .SUMMANDIAL;
        }

        beginSearchUI();

        Task<List<FormulaResult>> searchTask =
                new Task<>() {

                    @Override
                    protected List<FormulaResult> call() {

                        engine.setProgressListener(
                                (phase,
                                 message,
                                 progress) -> {

                                    Platform.runLater(
                                            () ->
                                                    updateSearchProgress(
                                                            phase,
                                                            message,
                                                            progress
                                                    )
                                    );
                                }
                        );

                        return engine.search(
                                input,
                                maximumTerms,
                                sequenceType
                        );
                    }
                };

        searchTask.setOnSucceeded(
                event -> {

                    engine.setProgressListener(
                            null
                    );

                    List<FormulaResult> results =
                            searchTask.getValue();

                    expressionsValue.setText(
                            String.format(
                                    "%,d",
                                    engine
                                            .getExpressionsSearched()
                            )
                    );

                    patternsValue.setText(
                            String.format(
                                    "%,d",
                                    engine
                                            .getOperatorPatternsSearched()
                            )
                    );

                    pathsValue.setText(
                            String.format(
                                    "%,d",
                                    engine
                                            .getSurvivingPaths()
                            )
                    );

                    formulasValue.setText(
                            String.format(
                                    "%,d",
                                    results.size()
                            )
                    );

                    resultsTable.setItems(
                            FXCollections
                                    .observableArrayList(
                                            convertFormulaResults(
                                                    results,
                                                    input
                                            )
                                    )
                    );

                    finishSearchUI(
                            results.size()
                    );

                    if (!resultsTable
                            .getItems()
                            .isEmpty()) {

                        resultsTable
                                .getSelectionModel()
                                .selectFirst();
                    }
                }
        );

        searchTask.setOnFailed(
                event -> {

                    engine.setProgressListener(
                            null
                    );

                    failSearchUI();

                    Throwable exception =
                            searchTask.getException();

                    if (exception != null) {

                        exception.printStackTrace();

                        showError(
                                exception.getMessage() != null
                                        ? exception.getMessage()
                                        : exception.toString()
                        );
                    }
                }
        );

        Thread searchThread =
                new Thread(
                        searchTask,
                        "equation-matcher-search"
                );

        searchThread.setDaemon(
                true
        );

        searchThread.start();
    }

    private void updateSearchProgress(
            int phase,
            String message,
            double progress) {

        phaseLabel.setText(
                "PHASE "
                        + phase
                        + " OF 4"
        );

        phaseMessage.setText(
                message
        );

        phaseProgressBar.setProgress(
                progress
        );
    }

    // ============================================================
    // FORMULA DISPLAY
    // ============================================================

    private void displayFormulaResult(
            FormulaResult result) {

        visualizationTitle.setText(
                "Verified symbolic pattern"
        );

        visualizationFormula.setText(
                result.getFormula()
        );

        visualizationFormula
                .getStyleClass()
                .remove(
                        "visualization-formula"
                );

        if (!visualizationFormula
                .getStyleClass()
                .contains(
                        "visualization-formula-active"
                )) {

            visualizationFormula
                    .getStyleClass()
                    .add(
                            "visualization-formula-active"
                    );
        }

        List<List<Integer>> incrementHistory =
                result.getIncrementHistory();

        List<Integer> variablePositions =
                new ArrayList<>();

        List<Integer> constantPositions =
                new ArrayList<>();

        for (int position = 0;
             position < incrementHistory.size();
             position++) {

            if (incrementHistory
                    .get(position)
                    .isEmpty()) {

                constantPositions.add(
                        position + 1
                );

            } else {

                variablePositions.add(
                        position + 1
                );
            }
        }

        visualizationExplanation.setText(
                "Variable positions: "
                        + variablePositions
                        + "     •     Constant positions: "
                        + constantPositions
        );

        pathContainer
                .getChildren()
                .clear();

        List<String> path =
                result.getVerificationPath();

        for (int i = 0;
             i < path.size();
             i++) {

            VBox stage =
                    createPathStage(
                            i + 1,
                            path.get(i),
                            incrementHistory
                    );

            pathContainer
                    .getChildren()
                    .add(stage);

            if (i < path.size() - 1) {

                Label arrow =
                        new Label("↓");

                arrow
                        .getStyleClass()
                        .add("path-arrow");

                pathContainer
                        .getChildren()
                        .add(arrow);
            }
        }
    }

    private VBox createPathStage(
            int nValue,
            String expression,
            List<List<Integer>> incrementHistory) {

        Label nLabel =
                new Label(
                        "n = " + nValue
                );

        nLabel
                .getStyleClass()
                .add("path-n-label");

        HBox expressionBox =
                buildExpressionVisualization(
                        expression,
                        incrementHistory
                );

        VBox stage =
                new VBox(
                        5,
                        nLabel,
                        expressionBox
                );

        stage.setAlignment(
                Pos.CENTER
        );

        return stage;
    }

    private HBox buildExpressionVisualization(
            String expression,
            List<List<Integer>> incrementHistory) {

        HBox row =
                new HBox(
                        7
                );

        row.setAlignment(
                Pos.CENTER
        );

        String[] tokens =
                expression.split(" ");

        int termPosition = 0;

        for (String token
                : tokens) {

            if (isOperator(token)) {

                Label operator =
                        new Label(
                                displayOperator(
                                        token
                                )
                        );

                operator
                        .getStyleClass()
                        .add(
                                "expression-operator"
                        );

                row.getChildren()
                        .add(operator);

            } else {

                boolean variable =
                        termPosition
                                < incrementHistory.size()
                                && !incrementHistory
                                .get(termPosition)
                                .isEmpty();

                Label term =
                        new Label(
                                token
                        );

                term.setMinWidth(
                        42
                );

                term.setAlignment(
                        Pos.CENTER
                );

                term
                        .getStyleClass()
                        .add(
                                variable
                                        ? "expression-variable"
                                        : "expression-term"
                        );

                row.getChildren()
                        .add(term);

                termPosition++;
            }
        }

        return row;
    }

    // ============================================================
    // SEARCH UI STATES
    // ============================================================

    private void beginSearchUI() {

        setRunningStatus();

        runButton.setDisable(
                true
        );

        runButton.setText(
                "Searching..."
        );

        resultsTable
                .getItems()
                .clear();

        expressionsValue.setText("—");
        patternsValue.setText("—");
        pathsValue.setText("—");
        formulasValue.setText("—");

        visualizationTitle.setVisible(
                false
        );

        visualizationFormula.setVisible(
                false
        );

        visualizationExplanation.setVisible(
                false
        );

        pathContainer.setVisible(
                false
        );

        phaseLabel.setText(
                "PHASE 1 OF 4"
        );

        phaseMessage.setText(
                "Preparing expression search..."
        );

        phaseProgressBar.setProgress(
                0
        );

        progressPanel.setVisible(
                true
        );
    }

    private void finishSearchUI(
            int formulaCount) {

        progressPanel.setVisible(
                false
        );

        visualizationTitle.setVisible(
                true
        );

        visualizationFormula.setVisible(
                true
        );

        visualizationExplanation.setVisible(
                true
        );

        pathContainer.setVisible(
                true
        );

        runButton.setDisable(
                false
        );

        runButton.setText(
                "▶   Run Search"
        );

        setCompleteStatus();

        if (formulaCount == 0) {

            visualizationTitle.setText(
                    "No formula found"
            );

            visualizationFormula.setText(
                    "No surviving symbolic pattern"
            );

            visualizationExplanation.setText(
                    "No candidate remained valid throughout the requested input range."
            );
        }
    }

    private void failSearchUI() {

        progressPanel.setVisible(
                false
        );

        visualizationTitle.setVisible(
                true
        );

        visualizationFormula.setVisible(
                true
        );

        visualizationExplanation.setVisible(
                true
        );

        pathContainer.setVisible(
                true
        );

        runButton.setDisable(
                false
        );

        runButton.setText(
                "▶   Run Search"
        );

        setErrorStatus();

        visualizationTitle.setText(
                "Search interrupted"
        );

        visualizationFormula.setText(
                "The search could not be completed."
        );
    }

    // ============================================================
    // RESULT CONVERSION
    // ============================================================

    private List<FormulaRow>
    convertFormulaResults(
            List<FormulaResult> results,
            int input) {

        return results
                .stream()
                .map(
                        result ->
                                new FormulaRow(
                                        result.getFormula(),
                                        "n = 1..." + input,
                                        "MATCH",
                                        result
                                )
                )
                .toList();
    }

    // ============================================================
    // STATUS
    // ============================================================

    private void setReadyStatus() {
        setStatus(
                "●  READY",
                "status-ready"
        );
    }

    private void setRunningStatus() {
        setStatus(
                "●  SEARCHING",
                "status-searching"
        );
    }

    private void setCompleteStatus() {
        setStatus(
                "●  COMPLETE",
                "status-complete"
        );
    }

    private void setErrorStatus() {
        setStatus(
                "●  ERROR",
                "status-error"
        );
    }

    private void setStatus(
            String text,
            String statusClass) {

        statusLabel.setText(
                text
        );

        statusLabel
                .getStyleClass()
                .removeAll(
                        "status-ready",
                        "status-searching",
                        "status-complete",
                        "status-error"
                );

        statusLabel
                .getStyleClass()
                .add(
                        statusClass
                );
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private Label createSectionHeading(
            String text) {

        Label label =
                new Label(text);

        label
                .getStyleClass()
                .add(
                        "section-heading"
                );

        return label;
    }

    private Label createFieldLabel(
            String text) {

        Label label =
                new Label(text);

        label
                .getStyleClass()
                .add(
                        "field-label"
                );

        return label;
    }

    private Label createOperatorBadge(
            String operator) {

        Label badge =
                new Label(
                        operator
                );

        badge.setMinSize(
                36,
                36
        );

        badge
                .getStyleClass()
                .add(
                        "operator-badge"
                );

        return badge;
    }

    private Region createVerticalSpace(
            double height) {

        Region region =
                new Region();

        region.setPrefHeight(
                height
        );

        return region;
    }

    private void showError(
            String message) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setTitle(
                "Equation Matcher"
        );

        alert.setHeaderText(
                "Search could not be completed"
        );

        alert.setContentText(
                message
        );

        alert.showAndWait();
    }

    private boolean isOperator(
            String token) {

        return token.equals("+")
                || token.equals("-")
                || token.equals("*")
                || token.equals("/")
                || token.equals("^");
    }

    private String displayOperator(
            String operator) {

        switch (operator) {

            case "*":
                return "×";

            case "/":
                return "÷";

            default:
                return operator;
        }
    }

    // ============================================================
    // TABLE MODEL
    // ============================================================

    public static class FormulaRow {

        private final String formula;
        private final String range;
        private final String status;
        private final FormulaResult result;

        public FormulaRow(
                String formula,
                String range,
                String status,
                FormulaResult result) {

            this.formula =
                    formula;

            this.range =
                    range;

            this.status =
                    status;

            this.result =
                    result;
        }

        public String getFormula() {
            return formula;
        }

        public String getRange() {
            return range;
        }

        public String getStatus() {
            return status;
        }

        public FormulaResult getResult() {
            return result;
        }
    }

    public Parent getRoot() {
        return root;
    }
}