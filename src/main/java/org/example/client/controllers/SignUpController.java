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
//import org.example.network.Message;
import javafx.application.Platform;
import javafx.scene.control.Alert;

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
        String role = "bidder";

        // 1. Dàn code Validate cực xịn của sếp (Giữ nguyên)
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("⚠Vui lòng điền đầy đủ thông tin");
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

        // 2. Tạm thời vô hiệu hóa nút bấm và báo đang xử lý để tránh spam click
        errorLabel.setStyle("-fx-text-fill: #1976d2; -fx-font-size: 12;");
        errorLabel.setText("Đang kết nối đến máy chủ...");

        // 3. Mở luồng chạy ngầm gửi dữ liệu qua Socket
        new Thread(() -> {
            try (java.net.Socket socket = new java.net.Socket("localhost", 8080);
                 java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(socket.getOutputStream());
                 java.io.ObjectInputStream in = new java.io.ObjectInputStream(socket.getInputStream())) {

                // (Tùy chọn) Đọc tin nhắn chào mừng từ Server nếu sếp có cài đặt
                // org.example.shared.Message welcomeMsg = (org.example.shared.Message) in.readObject();

                // Đóng gói dữ liệu gửi đi
                String[] regData = {username, password, role};
                org.example.network.Message regMsg = new org.example.network.Message("REGISTER", regData);

                out.writeObject(regMsg);
                out.flush();

                // Chờ Server (ClientHandler) trả lời
                org.example.network.Message responseMsg = (org.example.network.Message) in.readObject();

                // Cập nhật Giao Diện (Bắt buộc phải dùng Platform.runLater)
                Platform.runLater(() -> {
                    if ("REGISTER_RESPONSE".equals(responseMsg.getAction())) {
                        String result = (String) responseMsg.getPayload();

                        if (result.contains("thành công") || result.startsWith("✅")) {
                            // Gọi lại dàn code báo thành công và chuyển trang của sếp
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Thành công");
                            successAlert.setHeaderText("Tài khoản đã tạo thành công!");
                            successAlert.setContentText("Chào mừng " + username + "!\nBạn sẽ được chuyển đến trang đăng nhập...");
                            successAlert.showAndWait();

                            showSuccess(result);

                            PauseTransition pause = new PauseTransition(Duration.seconds(1));
                            pause.setOnFinished(event -> {
                                try {
                                    ClientApp.switchToLogin();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                            pause.play();
                        } else {
                            // Server báo lỗi (VD: Trùng username)
                            showError("Lỗi: " + result);
                        }
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> showError("Lỗi: Không thể kết nối đến máy chủ!"));
                e.printStackTrace();
            }
        }).start();
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
