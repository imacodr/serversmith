package dev.perillo.serversmith;

import dev.perillo.serversmith.ui.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {

        MainController mainController = new MainController();
        Scene scene = new Scene(mainController.getView(), 1200, 800);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        stage.getIcons().add(new Image(getClass().getResourceAsStream("app-icon.png")));

        stage.setTitle("ServerSmith");
        stage.setMinWidth(1200);
        stage.setMinHeight(800);
        stage.setScene(scene);

        stage.show();

        stage.setOnCloseRequest(e -> {
            // Cleanup, stop servers?
            // mainController.shutdown();
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
