package org.example.client.controllers.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.common.Message;
import org.example.common.model.item.AuctionStatus;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminOverviewController implements AdminChildController {

    @FXML private Label totalUsersLabel;
    @FXML private Label totalItemsLabel;
    @FXML private Label activeAuctionsLabel;
    @FXML private Label pendingAuctionsLabel;
    @FXML private Label closedAuctionsLabel;
    @FXML private Label totalBalanceLabel;

    private AdminDashboardController dashboardController;
    private List<User> users = new ArrayList<>();
    private List<Item> items = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    public void setup(AdminDashboardController dashboardController) {
        this.dashboardController = dashboardController;
        requestData();
    }

    @FXML
    private void onRefreshClicked() {
        requestData();
    }

    private void requestData() {
        dashboardController.sendMessage(new Message("GET_ALL_USERS", null));
        dashboardController.sendMessage(new Message("GET_ALL_ITEMS_ADMIN", null));
    }

    @Override
    public void handleServerMessage(Message message) {
        switch (message.getAction()) {
            case "GET_ALL_USERS_RESPONSE":
                users = castList(message.getPayload());
                updateStats();
                break;
            case "GET_ALL_ITEMS_ADMIN_RESPONSE":
                items = castList(message.getPayload());
                updateStats();
                break;
            default:
                break;
        }
    }

    private void updateStats() {
        long active = items.stream().filter(item -> item.getStatus() == AuctionStatus.ACTIVE).count();
        long pending = items.stream().filter(item -> item.getStatus() == AuctionStatus.PENDING).count();
        long closed = items.stream().filter(item -> item.getStatus() == AuctionStatus.CLOSED || item.getStatus() == AuctionStatus.CANCELED).count();
        double totalBalance = users.stream().mapToDouble(User::getBalance).sum();

        totalUsersLabel.setText(String.valueOf(users.size()));
        totalItemsLabel.setText(String.valueOf(items.size()));
        activeAuctionsLabel.setText(String.valueOf(active));
        pendingAuctionsLabel.setText(String.valueOf(pending));
        closedAuctionsLabel.setText(String.valueOf(closed));
        totalBalanceLabel.setText(currencyFormat.format(totalBalance) + " VNĐ");
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> castList(Object payload) {
        if (payload instanceof List<?>) {
            return (List<T>) payload;
        }
        return new ArrayList<>();
    }
}
