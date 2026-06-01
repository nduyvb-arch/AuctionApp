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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class RoleSelectionController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(RoleSelectionController.class);

    @FXML private Label userInfoLabel;
    @FXML private Label accountNameLabel;
    @FXML private Label balanceLabel;
    @FXML private Button accountButton;

    private User currentUser;
    private final NumberFormat currencyFormat = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
    private String selectedRoleForSwitch;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.debug("initialize() called");
        currentUser = ClientApp.getCurrentUser();
        logger.debug("Current user: {}", currentUser == null ? "null" : currentUser.getUsername());

        if (ClientApp.isAdminUser(currentUser)) {
            logger.info("Admin user detected ({}). Redirecting to admin UI.", currentUser == null ? "null" : currentUser.getUsername());
            Platform.runLater(() -> {
                try {
                    ClientApp.setSelectedRole("admin");
                    ClientApp.switchToAdmin();
                } catch (Exception e) {
                    logger.error("Failed to open admin UI", e);
                    showError("Không thể mở giao diện admin", e.getMessage());
                }
            });
            return;
        }

        // Đăng ký handler để xử lý các phản hồi từ server
        ClientApp.setServerMessageHandler(this::handleServerMessage);
        logger.debug("Server message handler set");
        updateAccountInfo();
    }

    private void updateAccountInfo() {
        if (currentUser == null) {
            logger.debug("updateAccountInfo: no current user");
            userInfoLabel.setText("Chưa đăng nhập");
            accountNameLabel.setText("Khách");
            balanceLabel.setText("0 VNĐ");
            return;
        }
        logger.debug("updateAccountInfo: username={}, balance={}", currentUser.getUsername(), currentUser.getBalance());
        userInfoLabel.setText("Xin chào, " + currentUser.getUsername());
        accountNameLabel.setText(currentUser.getUsername());
        balanceLabel.setText(currencyFormat.format(currentUser.getBalance()) + " VNĐ");
    }

    private void handleServerMessage(Message message) {
        if (message == null) {
            logger.warn("handleServerMessage received null message");
            return;
        }
        logger.debug("handleServerMessage received: action={}, payload={}", message.getAction(), String.valueOf(message.getPayload()));

        if (!"SWITCH_ROLE_RESPONSE".equals(message.getAction())) {
            logger.trace("Ignoring non SWITCH_ROLE_RESPONSE message: {}", message.getAction());
            return;
        }

        // payload may not be a string; use String.valueOf to avoid NPE
        String payload = String.valueOf(message.getPayload());
        if ("success".equals(payload)) {
            logger.info("Role switch success (selectedRoleForSwitch={})", selectedRoleForSwitch);
            try {
                // Đồng bộ cả vai trò đang chọn ở client để chat/đặt giá không bị lệch trạng thái.
                ClientApp.setSelectedRole(selectedRoleForSwitch);
                if (currentUser != null) {
                    currentUser.setRole(selectedRoleForSwitch);
                    ClientApp.setCurrentUser(currentUser);
                }
                ClientApp.switchToHome();
            } catch (Exception e) {
                logger.error("Error while switching to home after role switch", e);
                showError("Lỗi giao diện", "Không thể mở trang chủ: " + e.getMessage());
            }
        } else {
            logger.warn("Role switch failed: payload={}", payload);
            showError("Lỗi", "Không thể chuyển vai trò: " + payload);
        }
    }

    @FXML
    private void onBidderClicked() {
        logger.info("User clicked: Bidder");
        openHomeWithRole("bidder");
    }

    @FXML
    private void onSellerClicked() {
        logger.info("User clicked: Seller");
        openHomeWithRole("seller");
    }

    @FXML
    private void onAccountClicked() {
        logger.info("User clicked: Account (open account on home load)");
        try {
            // Mở thẳng màn hình tài khoản riêng, không load qua HomeMenu của Bidder/Seller.
            ClientApp.switchToAccount();
        } catch (Exception e) {
            logger.error("Failed to open account/home", e);
            showError("Không thể mở tài khoản", e.getMessage());
        }
    }

    @FXML
    private void onLogoutClicked() {
        logger.info("User clicked: Logout");
        try {
            ClientApp.switchToLogin();
        } catch (Exception e) {
            logger.error("Failed to switch to login", e);
            showError("Không thể đăng xuất", e.getMessage());
        }
    }

    private void openHomeWithRole(String role) {
        if (role == null) {
            logger.warn("openHomeWithRole called with null role");
            return;
        }
        // Lưu lại vai trò đã chọn để sử dụng sau khi server phản hồi
        this.selectedRoleForSwitch = role;
        logger.info("Requesting role switch: {}", role);
        // Gửi yêu cầu chuyển vai trò tới server
        try {
            ClientApp.sendMessage(new Message("SWITCH_ROLE", role));
            logger.debug("SWITCH_ROLE message sent for role={}", role);
        } catch (Exception e) {
            logger.error("Failed to send SWITCH_ROLE message", e);
            showError("Lỗi gửi yêu cầu", "Không thể gửi yêu cầu chuyển vai trò: " + e.getMessage());
        }
    }

    private void showError(String title, String content) {
        logger.warn("Showing error to user: {} - {}", title, content);
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}