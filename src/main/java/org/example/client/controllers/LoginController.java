package org.example.client.controllers;

import org.example.client.ClientApp;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.example.common.Message;
import org.example.common.model.user.User;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private CheckBox rememberCheckbox;
    @FXML private Button loginButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        errorLabel.setText("");
        // Đăng ký handler để xử lý phản hồi từ server
        ClientApp.setServerMessageHandler(this::handleServerMessage);
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

        // Tách việc kết nối và gửi tin nhắn ra một luồng riêng để không block UI
        new Thread(() -> {
            try {
                // Bước 1: Kết nối tới server. ClientApp sẽ tự quản lý việc này.
                ClientApp.connectToServer();

                // Bước 2: Gửi thông tin đăng nhập
                String[] loginData = {username, password};
                ClientApp.sendMessage(new Message("LOGIN", loginData));

            } catch (Exception e) {
                // Nếu kết nối thất bại, hiển thị lỗi trên luồng UI
                Platform.runLater(() -> {
                    showError("Lỗi kết nối: Không thể kết nối tới server.");
                    resetLoginButton();
                });
            }
        }).start();
    }

    /**
     * Xử lý các tin nhắn nhận được từ server, đặc biệt là LOGIN_RESPONSE.
     * Phương thức này được gọi bởi luồng lắng nghe của ClientApp.
     */
    private void handleServerMessage(Message message) {
        if (message == null || !"LOGIN_RESPONSE".equals(message.getAction())) {
            return;
        }

        Object payload = message.getPayload();
        User user = null;

        if (payload instanceof Object[] && ((Object[]) payload).length > 0 && ((Object[]) payload)[0] instanceof User) {
            user = (User) ((Object[]) payload)[0];
        } else if (payload instanceof User) {
            user = (User) payload;
        }

        if (user == null) {
            showError("Tài khoản không tồn tại, sai mật khẩu, hoặc đã bị khóa!");
            resetLoginButton();
            return;
        }

        // Đăng nhập thành công
        ClientApp.setCurrentUser(user);
        System.out.println("Đăng nhập thành công: " + user.getUsername());

        try {
            if (ClientApp.isAdminUser(user)) {
                ClientApp.setSelectedRole("admin");
                ClientApp.switchToAdmin();
            } else {
                ClientApp.setSelectedRole("bidder"); // Mặc định là bidder khi vào màn chọn vai trò
                ClientApp.switchToRoleSelection();
            }
        } catch (Exception e) {
            showError("Lỗi chuyển màn hình: " + e.getMessage());
            resetLoginButton();
        }
    }

    private void resetLoginButton() {
        loginButton.setDisable(false);
        loginButton.setText("Đăng nhập");
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
        showError("Chức năng đang được nâng cấp!");
    }

    private void showError(String message) {
        // Đảm bảo việc cập nhật UI luôn chạy trên luồng chính
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12;");

            PauseTransition pause = new PauseTransition(Duration.seconds(5));
            pause.setOnFinished(event -> errorLabel.setText(""));
            pause.play();
        });
    }
}
