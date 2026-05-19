package org.example.client.controllers;
import org.example.client.ClientApp;
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
        try {
            ClientApp.setSelectedRole(role);
            ClientApp.switchToHome();
        } catch (Exception e) {
            showError("Không thể mở trang chủ", e.getMessage());
        }
    }

    private String formatRole(String role) {
        if (role == null || role.isBlank()) {
            return "Không rõ";
        }
        if ("seller".equalsIgnoreCase(role)) {
            return "Người bán";
        }
        if ("bidder".equalsIgnoreCase(role)) {
            return "Người đấu giá";
        }
        return role;
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
