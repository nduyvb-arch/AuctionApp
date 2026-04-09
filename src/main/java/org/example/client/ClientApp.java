package org.example.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class ClientApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("views/LoginMenu.fxml"));
        Scene scene = new Scene(loader.load(), 320, 400);
        stage.setScene(scene);
        stage.show();
        stage.setTitle("Hello");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
