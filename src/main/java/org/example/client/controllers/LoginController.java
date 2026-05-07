package org.example.client.controllers;

import javafx.animation.PauseTransition;
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
import org.example.manager.UserManager;
import org.example.model.user.User;
import org.example.network.Message;

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

        // Sử dụng Task để đăng nhập trên luồng nền, tránh làm đơ giao diện
        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {

                var out = ClientApp.getOutputStream();
                var in = ClientApp.getInputStream();

                // Đóng gói dữ liệu gửi lên Server
                String[] loginData = {username, password};
                Message loginMsg = new Message("LOGIN", loginData);
                out.writeObject(loginMsg);
                out.flush();

                // Chờ Server (ClientHandler) trả lời
                Message responseMsg = (Message) in.readObject();

                if (responseMsg != null && "LOGIN_RESPONSE".equals(responseMsg.getAction())) {
                    User user = (User) responseMsg.getPayload(); // Ép kiểu payload về User

                    if (user != null) {
                        return user; // Đăng nhập thành công
                    } else {
                        throw new SecurityException("Tên đăng nhập hoặc mật khẩu không đúng");
                    }
                } else {
                    throw new IOException("Phản hồi từ server không hợp lệ.");
                }
            }
        };

        // Xử lý khi đăng nhập thành công
        loginTask.setOnSucceeded(event -> {
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

        // Xử lý khi đăng nhập thất bại
        loginTask.setOnFailed(event -> {
            Throwable exception = loginTask.getException();
            showError(exception.getMessage()); // Hiển thị lỗi "Tên đăng nhập hoặc mật khẩu không đúng"
            loginButton.setDisable(false);
            loginButton.setText("Đăng nhập");
        });

        // Bắt đầu chạy Task
        new Thread(loginTask).start();
    }

    @FXML
    public void onSignUpButtonClicked() {
        try {
            ClientApp.switchToSignUp();
        } catch (Exception e) {
            System.err.println("Error switching to sign up: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void onForgotPasswordClicked() {
        System.out.println("Chuyển đến trang lấy lại mật khẩu...");
        // TODO: Load cảnh reset password
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12;");
        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(event -> errorLabel.setText(""));
        pause.play();
    }
}