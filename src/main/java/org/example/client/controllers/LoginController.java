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
import org.example.network.Message;
import org.example.model.user.User;

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

        // Thêm focus effects
        usernameField.setStyle("-fx-border-color: #667eea; -fx-border-width: 0 0 2 0; -fx-padding: 12; -fx-font-size: 13; -fx-background-radius: 5;");
        usernameField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                usernameField.setStyle("-fx-border-color: #667eea; -fx-border-width: 0 0 2 0; -fx-padding: 12; -fx-font-size: 13; -fx-background-radius: 5;");
            } else {
                usernameField.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 12; -fx-font-size: 13; -fx-background-radius: 5;");
            }
        });

        passwordField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                passwordField.setStyle("-fx-border-color: #667eea; -fx-border-width: 0 0 2 0; -fx-padding: 12; -fx-font-size: 13; -fx-background-radius: 5;");
            } else {
                passwordField.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 12; -fx-font-size: 13; -fx-background-radius: 5;");
            }
        });
    }

    @FXML
    public void onLoginButtonClicked() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập tên đăng nhập và mật khẩu");
            return;
        }

        if (username.length() < 3) {
            showError("Tên đăng nhập phải có ít nhất 3 ký tự");
            return;
        }

        if (password.length() < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }

        // Vô hiệu hóa nút để tránh click nhiều lần và hiển thị trạng thái chờ
        loginButton.setDisable(true);
        loginButton.setText("Đang đăng nhập...");

        // Tạo một Task để thực hiện việc đăng nhập trên một luồng nền (background thread)
        // Điều này giúp giao diện không bị "đơ" trong khi chờ phản hồi từ server
        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                // Giả định ClientApp cung cấp stream để giao tiếp với server
                // Đây là phần code chạy ngầm, không ảnh hưởng giao diện
                var out = ClientApp.getOutputStream();
                var in = ClientApp.getInputStream();

                // 1. Chuẩn bị và gửi yêu cầu đăng nhập đến server
                String[] credentials = {username, password};
                Message loginRequest = new Message("LOGIN", credentials);
                out.writeObject(loginRequest);
                out.flush();

                // 2. Chờ và đọc phản hồi từ server (đây là một hành động blocking)
                Message response = (Message) in.readObject();

                // 3. Xử lý phản hồi
                if (response != null && "LOGIN_RESPONSE".equals(response.getAction()) && response.getPayload() instanceof User) {
                    return (User) response.getPayload(); // Trả về đối tượng User nếu thành công
                } else {
                    // Ném ra một Exception để kích hoạt onFailed()
                    throw new SecurityException("Tên đăng nhập hoặc mật khẩu không đúng");
                }
            }
        };

        // Xử lý khi Task đăng nhập thành công
        loginTask.setOnSucceeded(event -> {
            User user = loginTask.getValue();
            ClientApp.setCurrentUser(user);
            System.out.println("Đăng nhập thành công: " + user.getUsername());
            try {
                ClientApp.switchToHome();
            } catch (Exception e) {
                showError("Lỗi chuyển màn hình: " + e.getMessage());
                loginButton.setDisable(false); // Bật lại nút nếu có lỗi
                loginButton.setText("Đăng nhập");
            }
        });

        // Xử lý khi Task đăng nhập thất bại (do mạng lỗi, sai mật khẩu, v.v.)
        loginTask.setOnFailed(event -> {
            Throwable exception = loginTask.getException();
            showError("Loi:" + exception.getMessage());
            loginButton.setDisable(false);
            loginButton.setText("Đăng nhập");
        });

        // Bắt đầu chạy Task trên một luồng mới
        new Thread(loginTask).start();
    }

    @FXML
    public void onSignUpButtonClicked() {
        System.out.println("Attempting to switch to sign up menu...");
        try {
            ClientApp.switchToSignUp();
            System.out.println("Switched to sign up menu successfully.");
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

        // Tự động xóa thông báo lỗi sau 5 giây
        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(event -> errorLabel.setText(""));
        pause.play();
    }

    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 12;");
    }
}
