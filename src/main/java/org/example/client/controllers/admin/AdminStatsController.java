package org.example.client.controllers.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.example.common.Message;
import org.example.common.model.item.AuctionStatus;
import org.example.common.model.item.Item;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AdminStatsController implements AdminChildController {

    @FXML private Label totalTransactionLabel;
    @FXML private Label averagePriceLabel;
    @FXML private Label activeRateLabel;
    @FXML private TableView<Item> topItemsTable;
    @FXML private TableColumn<Item, String> nameColumn;
    @FXML private TableColumn<Item, String> priceColumn;
    @FXML private TableColumn<Item, String> statusColumn;

    private AdminDashboardController dashboardController;
    private List<Item> allItems = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    public void setup(AdminDashboardController dashboardController) {
        this.dashboardController = dashboardController;
        setupTable();
        requestData();
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getItemName()));
        priceColumn.setCellValueFactory(data -> new SimpleStringProperty(currencyFormat.format(data.getValue().getCurrentPrice()) + " VNĐ"));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(formatStatus(data.getValue().getStatus())));
    }

    @FXML
    private void onRefreshClicked() {
        requestData();
    }

    private void requestData() {
        dashboardController.sendMessage(new Message("GET_ALL_ITEMS_ADMIN", null));
    }

    @Override
    public void handleServerMessage(Message message) {
        if ("GET_ALL_ITEMS_ADMIN_RESPONSE".equals(message.getAction())) {
            allItems = castList(message.getPayload());
            updateStats();
        }
    }

    private void updateStats() {
        double totalClosed = allItems.stream()
                .filter(item -> item.getStatus() == AuctionStatus.CLOSED)
                .mapToDouble(Item::getCurrentPrice)
                .sum();
        double avg = allItems.isEmpty() ? 0 : allItems.stream().mapToDouble(Item::getCurrentPrice).average().orElse(0);
        long active = allItems.stream().filter(item -> item.getStatus() == AuctionStatus.ACTIVE).count();
        double activeRate = allItems.isEmpty() ? 0 : (active * 100.0 / allItems.size());

        totalTransactionLabel.setText(currencyFormat.format(totalClosed) + " VNĐ");
        averagePriceLabel.setText(currencyFormat.format(avg) + " VNĐ");
        activeRateLabel.setText(String.format(Locale.US, "%.1f%%", activeRate));

        List<Item> topItems = allItems.stream()
                .sorted(Comparator.comparingDouble(Item::getCurrentPrice).reversed())
                .limit(10)
                .collect(Collectors.toList());
        topItemsTable.setItems(FXCollections.observableArrayList(topItems));
    }

    private String formatStatus(AuctionStatus status) {
        if (status == null) {
            return "Không rõ";
        }
        switch (status) {
            case PENDING:
                return "Chờ bắt đầu";
            case ACTIVE:
                return "Đang diễn ra";
            case CLOSED:
                return "Đã kết thúc";
            case CANCELED:
                return "Đã hủy";
            default:
                return status.name();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Item> castList(Object payload) {
        if (payload instanceof List<?>) {
            return (List<Item>) payload;
        }
        return new ArrayList<>();
    }
}
