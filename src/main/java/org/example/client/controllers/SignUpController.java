package org.example.client.controllers;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.example.client.ClientApp;
import org.example.common.Message;
import java.io.IOException;

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

    @FXML
    private Button signUpButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        errorLabel.setText("");
    }

    @FXML
    public void onSignUpButtonClicked() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String role = "bidder";

        // 1. Kiểm tra dữ liệu đầu vào
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

        // 2. Vô hiệu hóa nút và hiển thị trạng thái chờ
        signUpButton.setDisable(true);
        signUpButton.setText("Đang xử lý...");

        // 3. Sử dụng Task để gửi yêu cầu đăng ký trên luồng nền
        Task<String> signUpTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                // Lấy stream đã được thiết lập sẵn từ ClientApp
                var out = ClientApp.getOutputStream();
                var in = ClientApp.getInputStream();

                // Đóng gói dữ liệu và gửi yêu cầu đến server
                String[] regData = {username, password, role};
                Message regMsg = new Message("REGISTER", regData);
                out.writeObject(regMsg);
                out.flush();

                // Chờ và đọc phản hồi từ server
                Message responseMsg = (Message) in.readObject();

                if (responseMsg != null && "REGISTER_RESPONSE".equals(responseMsg.getAction())) {
                    return (String) responseMsg.getPayload();
                } else {
                    throw new IOException("Phản hồi từ server không hợp lệ.");
                }
            }
        };

        // Xử lý khi Task thành công
        signUpTask.setOnSucceeded(event -> {
            String result = signUpTask.getValue();
            if (result.contains("thành công")) {
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Thành công");
                successAlert.setHeaderText("Tài khoản đã tạo thành công!");
                successAlert.setContentText("Chào mừng " + username + "!\nBạn sẽ được chuyển đến trang đăng nhập...");
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
            } else {
                showError(result); // Hiển thị lỗi từ server (VD: Tên đăng nhập đã tồn tại)
                signUpButton.setDisable(false);
                signUpButton.setText("Đăng Ký");
            }
        });

        // Xử lý khi Task thất bại
        signUpTask.setOnFailed(event -> {
            Throwable exception = signUpTask.getException();
            showError("Lỗi: " + exception.getMessage());
            signUpButton.setDisable(false);
            signUpButton.setText("Đăng Ký");
        });

        // Bắt đầu chạy Task
        new Thread(signUpTask).start();
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
        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(event -> errorLabel.setText(""));
        pause.play();
    }
}
