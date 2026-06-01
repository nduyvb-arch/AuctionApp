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

    @FXML private Label userInfoLabel;
    @FXML private Label accountNameLabel;
    @FXML private Label balanceLabel;
    @FXML private Button accountButton;

    private User currentUser;
    private final NumberFormat currencyFormat = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
    private String selectedRoleForSwitch;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentUser = ClientApp.getCurrentUser();

        if (ClientApp.isAdminUser(currentUser)) {
            Platform.runLater(() -> {
                try {
                    ClientApp.setSelectedRole("admin");
                    ClientApp.switchToAdmin();
                } catch (Exception e) {
                    showError("Không thể mở giao diện admin", e.getMessage());
                }
            });
            return;
        }

        // Đăng ký handler để xử lý các phản hồi từ server
        ClientApp.setServerMessageHandler(this::handleServerMessage);
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

    private void handleServerMessage(Message message) {
        if (!"SWITCH_ROLE_RESPONSE".equals(message.getAction())) {
            return;
        }

        if ("success".equals(message.getPayload())) {
            try {
                // Đồng bộ cả vai trò đang chọn ở client để chat/đặt giá không bị lệch trạng thái.
                ClientApp.setSelectedRole(selectedRoleForSwitch);
                if (currentUser != null) {
                    currentUser.setRole(selectedRoleForSwitch);
                    ClientApp.setCurrentUser(currentUser);
                }
                ClientApp.switchToHome();
            } catch (Exception e) {
                showError("Lỗi giao diện", "Không thể mở trang chủ: " + e.getMessage());
            }
        } else {
            showError("Lỗi", "Không thể chuyển vai trò: " + message.getPayload());
        }
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
            // Mở thẳng màn hình tài khoản riêng, không load qua HomeMenu của Bidder/Seller.
            ClientApp.switchToAccount();
        } catch (Exception e) {
            showError("Không thể mở tài khoản", e.getMessage());
        }
    }

    @FXML
    private void onLogoutClicked() {
        try {
            ClientApp.switchToLogin();
        } catch (Exception e) {
            showError("Không thể đăng xuất", e.getMessage());
        }
    }

    private void openHomeWithRole(String role) {
        // Lưu lại vai trò đã chọn để sử dụng sau khi server phản hồi
        this.selectedRoleForSwitch = role;
        // Gửi yêu cầu chuyển vai trò tới server
        ClientApp.sendMessage(new Message("SWITCH_ROLE", role));
    }

    private void showError(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
