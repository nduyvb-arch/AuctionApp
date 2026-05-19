package org.example.client.controllers;

import org.example.client.ClientApp;
import org.example.common.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.example.common.model.user.User;

import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class RoleSelectionController implements Initializable {

    @FXML
    private Label userInfoLabel;

    @FXML
    private Label accountNameLabel;

    @FXML
    private Label balanceLabel;

    @FXML
    private Button accountButton;

    private User currentUser;
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentUser = ClientApp.getCurrentUser();
        updateAccountInfo();
    }

    private void updateAccountInfo() {
        if (currentUser == null) {
            userInfoLabel.setText("Chưa đăng nhập");
            accountNameLabel.setText("Khách");
            balanceLabel.setText("0 VNĐ");
            return;
        }

        userInfoLabel.setText("Xin chào, " + currentUser.getUsername());
        accountNameLabel.setText(currentUser.getUsername());
        balanceLabel.setText(currencyFormat.format(currentUser.getBalance()) + " VNĐ");
    }

    @FXML
    private void onBidderClicked() {
        openHomeWithRole("bidder");
    }

    @FXML
    private void onSellerClicked() {
        openHomeWithRole("seller");
    }

    @FXML
    private void onAccountClicked() {
        try {
            ClientApp.switchToAccount();
        } catch (Exception e) {
            showError("Không thể mở tài khoản", e.getMessage());
        }
    }

    @FXML
    private void onLogoutClicked() {
        try {
            ClientApp.setSelectedRole("bidder");
            ClientApp.setCurrentUser(null);
            ClientApp.closeConnection();
            ClientApp.switchToLogin();
        } catch (Exception e) {
            showError("Không thể đăng xuất", e.getMessage());
        }
    }

    private void openHomeWithRole(String role) {
        // 1. Cài đặt tai nghe đón phản hồi từ Server
        ClientApp.setServerMessageHandler(message -> {
            if ("SWITCH_ROLE_RESPONSE".equals(message.getAction())) {
                Platform.runLater(() -> {
                    if ("success".equals(message.getPayload())) {
                        try {
                            ClientApp.setSelectedRole(role);
                            ClientApp.switchToHome();
                        } catch (Exception e) {
                            showError("Lỗi giao diện", "Không thể mở trang chủ: " + e.getMessage());
                        }
                    } else {
                        showError("Lỗi", "Không thể chuyển vai trò: " + message.getPayload());
                    }
                });
            }
        });

        // 2. Gửi lệnh chuyển vai trò một cách an toàn
        try {
            ClientApp.sendMessage(new Message("SWITCH_ROLE", role));
        } catch (Exception e) {
            showError("Lỗi mạng", "Không thể gửi yêu cầu: " + e.getMessage());
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}