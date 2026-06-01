package org.example.client.controllers;

import org.example.client.ClientApp;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.example.common.Message;
import org.example.common.model.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    // ── Logger ─────────────────────────────────────────────────────────────────
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.debug("LoginController initializing...");
        errorLabel.setText("");
        ClientApp.setServerMessageHandler(this::handleServerMessage);
        logger.info("LoginController initialized. Server message handler registered.");
    }

    @FXML
    public void onLoginButtonClicked() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Log attempt — KHÔNG bao giờ log mật khẩu
        logger.info("Login attempt for username: '{}'", username);

        if (username.isEmpty() || password.isEmpty()) {
            logger.warn("Login validation failed: username or password field is empty.");
            showError("Vui lòng nhập tên đăng nhập và mật khẩu");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Đang đăng nhập...");

        new Thread(() -> {
            try {
                logger.debug("Attempting to connect to server for user: '{}'", username);
                ClientApp.connectToServer();
                logger.info("Connected to server successfully. Sending LOGIN message for user: '{}'", username);

                String[] loginData = {username, password};
                ClientApp.sendMessage(new Message("LOGIN", loginData));

            } catch (Exception e) {
                // Log đầy đủ stack trace ở mức ERROR
                logger.error("Connection error during login attempt for user '{}': {}", username, e.getMessage(), e);

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
        if (message == null) {
            logger.warn("Received a null message from server. Ignoring.");
            return;
        }

        if (!"LOGIN_RESPONSE".equals(message.getAction())) {
            logger.debug("Ignoring non-login message with action: '{}'", message.getAction());
            return;
        }

        logger.debug("Processing LOGIN_RESPONSE from server.");
        Object payload = message.getPayload();
        User user = null;

        if (payload instanceof Object[] && ((Object[]) payload).length > 0 && ((Object[]) payload)[0] instanceof User) {
            user = (User) ((Object[]) payload)[0];
        } else if (payload instanceof User) {
            user = (User) payload;
        } else {
            logger.warn("Unexpected payload type in LOGIN_RESPONSE: {}",
                    payload == null ? "null" : payload.getClass().getName());
        }

        if (user == null) {
            logger.warn("Login failed: invalid credentials or account locked.");
            showError("Tài khoản không tồn tại, sai mật khẩu, hoặc đã bị khóa!");
            resetLoginButton();
            return;
        }

        // Đăng nhập thành công
        logger.info("Login successful for user: '{}' (role: {})",
                user.getUsername(), ClientApp.isAdminUser(user) ? "ADMIN" : "USER");
        ClientApp.setCurrentUser(user);

        try {
            if (ClientApp.isAdminUser(user)) {
                logger.debug("Routing user '{}' to Admin screen.", user.getUsername());
                ClientApp.setSelectedRole("admin");
                ClientApp.switchToAdmin();
            } else {
                logger.debug("Routing user '{}' to Role Selection screen.", user.getUsername());
                ClientApp.setSelectedRole("bidder");
                ClientApp.switchToRoleSelection();
            }
        } catch (Exception e) {
            logger.error("Screen transition failed for user '{}': {}", user.getUsername(), e.getMessage(), e);
            showError("Lỗi chuyển màn hình: " + e.getMessage());
            resetLoginButton();
        }
    }

    private void resetLoginButton() {
        logger.debug("Resetting login button state.");
        loginButton.setDisable(false);
        loginButton.setText("Đăng nhập");
    }

    @FXML
    public void onSignUpButtonClicked() {
        logger.info("User navigating to Sign Up screen.");
        try {
            ClientApp.switchToSignUp();
        } catch (Exception e) {
            logger.error("Failed to switch to Sign Up screen: {}", e.getMessage(), e);
        }
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12;");

            PauseTransition pause = new PauseTransition(Duration.seconds(5));
            pause.setOnFinished(event -> errorLabel.setText(""));
            pause.play();
        });
    }
}