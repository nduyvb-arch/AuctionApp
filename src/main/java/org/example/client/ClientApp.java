package org.example.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.model.user.User;

public class ClientApp extends Application {

    public static Stage primaryStage;
    public static User currentUser;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        switchToLogin();
        stage.setResizable(false);
        stage.show();
    }

    public static void switchToLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/org/example/client/views/LoginMenu.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setTitle("Hệ thống đấu giá - Đăng nhập");
        primaryStage.setScene(scene);
    }

    public static void switchToSignUp() throws Exception {
        System.out.println("Loading SignUpMenu.fxml...");
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/org/example/client/views/SignUpMenu.fxml"));
        if (loader.getLocation() == null) {
            throw new Exception("SignUpMenu.fxml not found");
        }
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setTitle("Hệ thống đấu giá - Đăng ký");
        primaryStage.setScene(scene);
        System.out.println("SignUpMenu loaded successfully.");
    }

    public static void switchToHome() throws Exception {
        System.out.println("Loading HomeMenu.fxml...");
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/org/example/client/views/HomeMenu.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setTitle("Hệ thống đấu giá - Trang chủ");
        primaryStage.setScene(scene);
        System.out.println("HomeMenu loaded successfully.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}