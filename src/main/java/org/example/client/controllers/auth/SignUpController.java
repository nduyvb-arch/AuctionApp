package org.example.client.controllers.auth;

import org.example.client.ClientApp;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.example.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class SignUpController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(SignUpController.class);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Button signUpButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        errorLabel.setText("");
        logger.debug("Initializing SignUpController");
        // Đăng ký handler để xử lý phản hồi từ server ngay khi controller được tạo
        ClientApp.setServerMessageHandler(this::handleServerMessage);
    }

    @FXML
    public void onSignUpButtonClicked() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String role = "bidder";

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Vui lòng điền đầy đủ thông tin");
            logger.warn("Sign up attempt with missing fields (username present? {}).", !username.isEmpty());
            return;
        }
        if (username.length() < 3) {
            showError("Tên đăng nhập phải có ít nhất 3 ký tự");
            logger.warn("Username too short: '{}'", username);
            return;
        }
        if (password.length() < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự");
            logger.warn("Password too short for user '{}'", username);
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không khớp");
            logger.warn("Password and confirmation do not match for user '{}'", username);
            return;
        }

        signUpButton.setDisable(true);
        signUpButton.setText("Đang xử lý...");
        logger.info("Starting sign up process for username='{}'", username);

        // Chạy kết nối và gửi tin nhắn trên luồng nền
        new Thread(() -> {
            try {
                // ClientApp sẽ tự quản lý việc có cần kết nối mới hay không
                ClientApp.connectToServer();
                logger.debug("Connected to server (or confirmed existing connection) for user '{}'", username);

                String[] regData = {username, password, role};
                ClientApp.sendMessage(new Message("REGISTER", regData));
                logger.debug("Sent REGISTER message for user '{}'", username);

            } catch (Exception e) {
                logger.error("Error connecting to server while registering user '{}': {}", username, e.getMessage(), e);
                Platform.runLater(() -> {
                    showError("Lỗi kết nối tới Server!");
                    resetSignUpButton();
                });
            }
        }).start();
    }

    private void handleServerMessage(Message message) {
        if (!"REGISTER_RESPONSE".equals(message.getAction())) {
            logger.debug("Received non-register message in SignUpController: action={}", message.getAction());
            return;
        }

        Object payload = message.getPayload();
        String result = payload != null ? payload.toString() : "";
        logger.info("Received REGISTER_RESPONSE: {}", result);

        if (result.contains("thành công")) {
            logger.info("Registration success response received.");
            Platform.runLater(() -> {
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Thành công");
                successAlert.setHeaderText("Tài khoản đã tạo thành công!");
                successAlert.setContentText("Bạn sẽ được chuyển đến trang đăng nhập...");
                successAlert.show();

                PauseTransition pause = new PauseTransition(Duration.seconds(1));
                pause.setOnFinished(e -> {
                    try {
                        ClientApp.switchToLogin();
                        logger.debug("Switched to login view after successful registration.");
                    } catch (Exception ex) {
                        logger.error("Error switching to login view after registration: {}", ex.getMessage(), ex);
                    }
                });
                pause.play();
            });
        } else {
            logger.warn("Registration failed or returned error: {}", result);
            showError(result); // Hiển thị lỗi từ server
            resetSignUpButton();
        }
    }

    @FXML
    public void onBackToLoginClicked() {
        try {
            logger.debug("Back to login clicked");
            ClientApp.switchToLogin();
        } catch (Exception e) {
            logger.error("Error switching to login: {}", e.getMessage(), e);
        }
    }

    private void resetSignUpButton() {
        Platform.runLater(() -> {
            signUpButton.setDisable(false);
            signUpButton.setText("Đăng Ký");
            logger.debug("Sign up button reset to enabled state");
        });
    }

    private void showError(String message) {
        logger.warn("Showing error to user: {}", message);
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12;");
            PauseTransition pause = new PauseTransition(Duration.seconds(5));
            pause.setOnFinished(event -> errorLabel.setText(""));
            pause.play();
        });
    }
}
