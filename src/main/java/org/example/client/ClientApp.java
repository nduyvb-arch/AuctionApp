package org.example.client;

import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Khởi tạo SceneManager
        SceneManager sceneManager = SceneManager.getInstance();
        sceneManager.setPrimaryStage(stage);
        sceneManager.initializeScenes();

        // Hiển thị scene đăng nhập mặc định
        sceneManager.switchToLogin();

        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}