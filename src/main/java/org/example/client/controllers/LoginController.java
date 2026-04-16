package org.example.client.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

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
            showError("⚠️ Vui lòng nhập tên đăng nhập và mật khẩu");
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

        System.out.println("✓ Đang đăng nhập: " + username);
        System.out.println("✓ Nhớ mật khẩu: " + rememberCheckbox.isSelected());

        // TODO: Gọi authentication service ở đây
        showSuccess("Đang xác thực...");
    }

    @FXML
    public void onSignUpButtonClicked() {
        System.out.println("Chuyển đến trang đăng ký...");
        // TODO: Load cảnh đăng ký hoặc chuyển hướng
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
