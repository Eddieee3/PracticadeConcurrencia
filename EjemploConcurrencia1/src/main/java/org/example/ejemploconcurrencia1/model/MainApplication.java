package org.example.ejemploconcurrencia1.model;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                MainApplication.class.getResource("/org/example/ejemploconcurrencia1/ventas-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 720);
        stage.setTitle("Ventas concurrentes");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
