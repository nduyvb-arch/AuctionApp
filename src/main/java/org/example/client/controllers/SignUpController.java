package org.example.client.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import org.example.client.ClientApp;
import org.example.manager.UserManager;

import java.net.URL;
import java.util.ResourceBundle;

public class SignUpController implements Initializable {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label errorLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        errorLabel.setText("");
    }

    @FXML
    public void onSignUpButtonClicked() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String role = "bidder"; // Mặc định là bidder

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("⚠️ Vui lòng điền đầy đủ thông tin");
            return;
        }

        if (username.length() < 3) {
            showError("⚠️ Tên đăng nhập phải có ít nhất 3 ký tự");
            return;
        }

        if (password.length() < 6) {
            showError("⚠️ Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("⚠️ Mật khẩu xác nhận không khớp");
            return;
        }

        // Create account
        String result = UserManager.getInstance().createAccount(username, password, role);
        if (result.startsWith("✅")) {
            // Show success notification
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("✅ Thành công");
            successAlert.setHeaderText("Tài khoản đã tạo thành công!");
            successAlert.setContentText("Chào mừng " + username + "!\nBạn sẽ được chuyển đến trang đăng nhập...");
            successAlert.showAndWait();

            showSuccess(result);
            // Switch to login after success
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(event -> {
                try {
                    ClientApp.switchToLogin();
                } catch (Exception e) {
                    System.err.println("Error switching to login: " + e.getMessage());
                }
            });
            pause.play();
        } else {
            showError(result);
        }
    }

    @FXML
    public void onBackToLoginClicked() {
        try {
            ClientApp.switchToLogin();
        } catch (Exception e) {
            System.err.println("Error switching to login: " + e.getMessage());
        }
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
