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
        ClientApp.startServerListenerIfNeeded();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập tên đăng nhập và mật khẩu");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Đang đăng nhập...");

        // 1. Giao phó việc nghe ngóng cho "Trưởng phòng"
        ClientApp.setServerMessageHandler(message -> {
            if ("LOGIN_RESPONSE".equals(message.getAction())) {
                User user = (User) message.getPayload();

                // Đẩy dữ liệu về luồng giao diện chính để xử lý UI
                Platform.runLater(() -> {
                    if (user != null) {
                        ClientApp.setCurrentUser(user);
                        ClientApp.setSelectedRole("bidder");
                        System.out.println("Đăng nhập thành công: " + user.getUsername());

                        try {
                            ClientApp.switchToRoleSelection();
                        } catch (Exception e) {
                            showError("Lỗi chuyển màn hình: " + e.getMessage());
                            resetLoginButton();
                        }
                    } else {
                        // Nâng cấp: Báo lỗi chung cho cả sai mật khẩu và bị Admin khóa mõm
                        showError("Tài khoản không tồn tại, sai mật khẩu, hoặc đã bị khóa!");
                        resetLoginButton();
                    }
                });
            }
        });

        // 2. Gửi gói tin đi an toàn bằng ống nước chính quy
        try {
            // Mở kết nối nếu chưa có, và nhớ KÍCH HOẠT luồng nghe ngóng
            if (ClientApp.getOutputStream() == null) {
                ClientApp.connectToServer();
                ClientApp.startServerListenerIfNeeded();
            }

            String[] loginData = {username, password};
            ClientApp.sendMessage(new Message("LOGIN", loginData));

        } catch (Exception e) {
            showError("Lỗi kết nối tới Server: Server chưa mở hoặc mạng rớt!");
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
        // Chức năng quên mật khẩu nếu bạn muốn hoàn thiện sau.
        showError("Chức năng đang được nâng cấp!");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12;");

        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(event -> errorLabel.setText(""));
        pause.play();
    }
}