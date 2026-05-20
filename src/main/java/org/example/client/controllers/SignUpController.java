package org.example.client.controllers;

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

import java.net.URL;
import java.util.ResourceBundle;

public class SignUpController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Button signUpButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        errorLabel.setText("");
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
        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không khớp");
            return;
        }

        signUpButton.setDisable(true);
        signUpButton.setText("Đang xử lý...");

        // Chạy kết nối và gửi tin nhắn trên luồng nền
        new Thread(() -> {
            try {
                // ClientApp sẽ tự quản lý việc có cần kết nối mới hay không
                ClientApp.connectToServer();

                String[] regData = {username, password, role};
                ClientApp.sendMessage(new Message("REGISTER", regData));

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Lỗi kết nối tới Server!");
                    resetSignUpButton();
                });
            }
        }).start();
    }

    private void handleServerMessage(Message message) {
        if (!"REGISTER_RESPONSE".equals(message.getAction())) {
            return;
        }

        String result = (String) message.getPayload();

        if (result.contains("thành công")) {
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
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                pause.play();
            });
        } else {
            showError(result); // Hiển thị lỗi từ server
            resetSignUpButton();
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

    private void resetSignUpButton() {
        Platform.runLater(() -> {
            signUpButton.setDisable(false);
            signUpButton.setText("Đăng Ký");
        });
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
