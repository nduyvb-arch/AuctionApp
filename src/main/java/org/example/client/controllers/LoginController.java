package org.example.client.controllers;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.example.client.ClientApp;
import org.example.common.model.user.User;
import org.example.common.Message;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;
    @FXML
    private CheckBox rememberCheckbox;
    @FXML
    private Button loginButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        errorLabel.setText("");
    }

    @FXML
    public void onLoginButtonClicked() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập tên đăng nhập và mật khẩu");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Đang đăng nhập...");

        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                ClientApp.connectToServer();
                var out = ClientApp.getOutputStream();
                var in = ClientApp.getInputStream();

                String[] loginData = {username, password};
                Message loginMsg = new Message("LOGIN", loginData);
                out.writeObject(loginMsg);
                out.flush();

                Message responseMsg = (Message) in.readObject();

                if (responseMsg != null && "LOGIN_RESPONSE".equals(responseMsg.getAction())) {
                    User user = (User) responseMsg.getPayload();
                    if (user != null) {
                        return user;
                    } else {
                        ClientApp.closeConnection();
                        throw new SecurityException("Tên đăng nhập hoặc mật khẩu không đúng");
                    }
                } else {
                    ClientApp.closeConnection();
                    throw new IOException("Phản hồi từ server không hợp lệ.");
                }
            }
        };

        // Xử lý khi đăng nhập thành công
        loginTask.setOnSucceeded(event -> {
            // SỬA LỖI: Chuyển toàn bộ logic cập nhật UI vào Platform.runLater
            Platform.runLater(() -> {
                User user = loginTask.getValue();
                ClientApp.setCurrentUser(user);
                System.out.println("Đăng nhập thành công: " + user.getUsername());
                try {
                    ClientApp.switchToHome();
                } catch (Exception e) {
                    showError("Lỗi chuyển màn hình: " + e.getMessage());
                    loginButton.setDisable(false);
                    loginButton.setText("Đăng nhập");
                }
            });
        });

        // Xử lý khi đăng nhập thất bại
        loginTask.setOnFailed(event -> {
            // Cập nhật UI cũng cần Platform.runLater để đảm bảo an toàn
            Platform.runLater(() -> {
                Throwable exception = loginTask.getException();
                showError(exception.getMessage());
                loginButton.setDisable(false);
                loginButton.setText("Đăng nhập");
                ClientApp.closeConnection();
            });
        });

        new Thread(loginTask).start();
    }

    @FXML
    public void onSignUpButtonClicked() {
        try {
            ClientApp.switchToSignUp();
        } catch (Exception e) {
            System.err.println("Error switching to sign up: " + e.getMessage());
        }
    }

    @FXML
    public void onForgotPasswordClicked() {
        // ...
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12;");
        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(event -> errorLabel.setText(""));
        pause.play();
    }
}
