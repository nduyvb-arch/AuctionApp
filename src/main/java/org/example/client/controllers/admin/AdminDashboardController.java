package org.example.client.controllers.admin;

import org.example.client.ClientApp;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.example.common.Message;
import org.example.common.model.user.User;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private Label adminNameLabel;
    @FXML private Label pageTitleLabel;
    @FXML private StackPane contentPane;

    @FXML private Button overviewButton;
    @FXML private Button usersButton;
    @FXML private Button itemsButton;
    @FXML private Button auctionsButton;
    @FXML private Button statsButton;

    private User currentUser;
    private AdminChildController activeChildController;

    private static final String ACTIVE_BUTTON_STYLE = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 12 16; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;";
    private static final String NORMAL_BUTTON_STYLE = "-fx-background-color: transparent; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 12 16; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentUser = ClientApp.getCurrentUser();

        if (currentUser != null) {
            adminNameLabel.setText("Xin chào, " + currentUser.getUsername());
        } else {
            adminNameLabel.setText("Xin chào, admin");
        }

        ClientApp.setServerMessageHandler(this::handleServerMessage);
        loadOverview();
    }

    @FXML
    private void loadOverview() {
        loadChildView("/org/example/client/views/admin/AdminOverviewView.fxml", "Tổng quan hệ thống", overviewButton);
    }

    @FXML
    private void loadUsers() {
        loadChildView("/org/example/client/views/admin/AdminUsersView.fxml", "Quản lý người dùng", usersButton);
    }

    @FXML
    private void loadItems() {
        loadChildView("/org/example/client/views/admin/AdminItemsView.fxml", "Quản lý sản phẩm", itemsButton);
    }

    @FXML
    private void loadAuctions() {
        loadChildView("/org/example/client/views/admin/AdminAuctionsView.fxml", "Quản lý phiên đấu giá", auctionsButton);
    }

    @FXML
    private void loadStats() {
        loadChildView("/org/example/client/views/admin/AdminStatsView.fxml", "Thống kê", statsButton);
    }

    @FXML
    private void onLogoutClicked() {
        try {
            ClientApp.switchToLogin();
        } catch (Exception e) {
            showError("Đăng xuất", "Không thể đăng xuất: " + e.getMessage());
        }
    }

    private void loadChildView(String resourcePath, String title, Button activeButton) {
        try {
            FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource(resourcePath));
            Parent view = loader.load();
            Object controller = loader.getController();

            if (controller instanceof AdminChildController) {
                activeChildController = (AdminChildController) controller;
                activeChildController.setup(this);
            } else {
                activeChildController = null;
            }

            contentPane.getChildren().setAll(view);
            pageTitleLabel.setText(title);
            setActiveButton(activeButton);
        } catch (Exception e) {
            showError("Quản trị", "Không thể mở màn hình: " + e.getMessage());
            e.printStackTrace(); // In stack trace để debug
        }
    }

    private void setActiveButton(Button activeButton) {
        overviewButton.setStyle(NORMAL_BUTTON_STYLE);
        usersButton.setStyle(NORMAL_BUTTON_STYLE);
        itemsButton.setStyle(NORMAL_BUTTON_STYLE);
        auctionsButton.setStyle(NORMAL_BUTTON_STYLE);
        statsButton.setStyle(NORMAL_BUTTON_STYLE);

        if (activeButton != null) {
            activeButton.setStyle(ACTIVE_BUTTON_STYLE);
        }
    }

    public void sendMessage(Message message) {
        ClientApp.sendMessage(message);
    }

    private void handleServerMessage(Message message) {
        if (message == null) {
            return;
        }

        if ("ERROR".equals(message.getAction())) {
            showError("Server", String.valueOf(message.getPayload()));
            return;
        }

        if ("SYSTEM_NOTIFICATION".equals(message.getAction())) {
            System.out.println("Server: " + message.getPayload());
        }

        if (activeChildController != null) {
            activeChildController.handleServerMessage(message);
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
