package equationmatcher;

import equationmatcher.gui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {

        MainView mainView =
                new MainView();

        Scene scene =
                new Scene(
                        mainView.getRoot(),
                        1280,
                        800
                );

        scene.getStylesheets().add(
                getClass()
                        .getResource(
                                "/equationmatcher/styles.css"
                        )
                        .toExternalForm()
        );

        stage.setTitle(
                "Equation Matcher"
        );

        stage.setMinWidth(
                1050
        );

        stage.setMinHeight(
                700
        );

        stage.setScene(
                scene
        );

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}