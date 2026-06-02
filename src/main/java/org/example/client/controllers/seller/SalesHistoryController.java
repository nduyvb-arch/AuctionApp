package org.example.client.controllers.seller;

import java.net.URL;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SalesHistoryController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(SalesHistoryController.class);

    @FXML
    private TextField salesSearchTextField;
    @FXML
    private ComboBox<String> salesStatusComboBox;
    @FXML
    private Label totalSoldLabel;
    @FXML
    private Label revenueLabel;
    @FXML
    private TableView<Item> salesHistoryTableView;
    @FXML
    private TableColumn<Item, String> idColumn;
    @FXML
    private TableColumn<Item, String> nameColumn;
    @FXML
    private TableColumn<Item, String> typeColumn;
    @FXML
    private TableColumn<Item, String> winnerColumn;
    @FXML
    private TableColumn<Item, String> priceColumn;
    @FXML
    private TableColumn<Item, String> statusColumn;
    @FXML
    private TableColumn<Item, String> endTimeColumn;
    private List<Item> items = new ArrayList<>();
    private User currentUser;
    private Runnable onRefreshRequested;
    private static final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi_VN"));
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public SalesHistoryController() {
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.debug("Initializing SalesHistoryController");
        this.salesStatusComboBox.getItems().setAll(new String[]{"Đã bán", "Đang diễn ra", "Tất cả"});
        this.salesStatusComboBox.setValue("Đã bán");

        this.idColumn.setCellValueFactory((data) -> new SimpleStringProperty(((Item)data.getValue()).getId()));
        this.nameColumn.setCellValueFactory((data) -> new SimpleStringProperty(((Item)data.getValue()).getItemName()));
        this.typeColumn.setCellValueFactory((data) -> new SimpleStringProperty(((Item)data.getValue()).getType()));
        this.winnerColumn.setCellValueFactory((data) -> new SimpleStringProperty(((Item)data.getValue()).getCurrentWinnerId() == null ? "-" : ((Item)data.getValue()).getCurrentWinnerId()));
        this.priceColumn.setCellValueFactory((data) -> {
            NumberFormat var10002 = currencyFormat;
            return new SimpleStringProperty(var10002.format(((Item)data.getValue()).getCurrentPrice()) + " VNĐ");
        });
        this.statusColumn.setCellValueFactory((data) -> new SimpleStringProperty(this.getStatusText(((Item)data.getValue()).getStatus().name())));
        this.endTimeColumn.setCellValueFactory((data) -> new SimpleStringProperty(((Item)data.getValue()).getEndTime() == null ? "-" : ((Item)data.getValue()).getEndTime().format(dateFormatter)));

        this.salesSearchTextField.setOnKeyReleased((e) -> {
            logger.debug("Search field key released, refreshing view");
            this.refreshSalesHistoryView();
        });
        this.salesStatusComboBox.setOnAction((e) -> {
            logger.debug("Status combo changed to {}, refreshing view", this.salesStatusComboBox.getValue());
            this.refreshSalesHistoryView();
        });
    }

    public void setup(List<Item> items, User currentUser, Runnable onRefreshRequested) {
        logger.info("Setup SalesHistoryController: itemsCount={}, currentUser={}", items == null ? 0 : items.size(), currentUser == null ? "null" : currentUser.getId());
        this.items = items;
        this.currentUser = currentUser;
        this.onRefreshRequested = onRefreshRequested;
        this.refreshSalesHistoryView();
    }

    public void updateData(List<Item> items) {
        logger.debug("updateData called with {} items", items == null ? 0 : items.size());
        this.items = items;
        this.refreshSalesHistoryView();
    }

    @FXML
    private void onSalesRefreshClicked() {
        logger.info("Sales refresh clicked by user");
        if (this.onRefreshRequested != null) {
            logger.debug("Executing onRefreshRequested runnable");
            try {
                this.onRefreshRequested.run();
            } catch (Exception e) {
                logger.error("Exception while running onRefreshRequested runnable", e);
            }
        } else {
            logger.debug("No onRefreshRequested runnable provided");
        }
        this.refreshSalesHistoryView();
    }

    public void refreshSalesHistoryView() {
        logger.debug("Refreshing sales history view; currentUser={}", this.currentUser == null ? "null" : this.currentUser.getId());
        try {
            if (this.currentUser == null) {
                logger.debug("Current user is null — clearing table and resetting counters");
                this.salesHistoryTableView.setItems(FXCollections.observableArrayList());
                this.totalSoldLabel.setText("0");
                this.revenueLabel.setText("0 VNĐ");
            } else {
                String search = this.salesSearchTextField.getText() == null ? "" : this.salesSearchTextField.getText().toLowerCase();
                String filter = (String)this.salesStatusComboBox.getValue();
                List<Item> filtered = (List)this.items.stream()
                        .filter((item) -> this.currentUser.getId().equals(item.getSellerId()))
                        .filter((item) -> item.getItemName().toLowerCase().contains(search))
                        .filter((item) -> this.applyFilter(item, filter))
                        .collect(Collectors.toList());

                this.salesHistoryTableView.setItems(FXCollections.observableArrayList(filtered));

                long soldCount = filtered.stream()
                        .filter((item) -> "CLOSED".equals(item.getStatus().name()) && item.getCurrentWinnerId() != null)
                        .count();

                double revenue = filtered.stream()
                        .filter((item) -> "CLOSED".equals(item.getStatus().name()) && item.getCurrentWinnerId() != null)
                        .mapToDouble(Item::getCurrentPrice)
                        .sum();

                this.totalSoldLabel.setText(String.valueOf(soldCount));
                this.revenueLabel.setText(currencyFormat.format(revenue) + " VNĐ");

                logger.info("Sales view refreshed: filteredCount={}, soldCount={}, revenue={}", filtered.size(), soldCount, revenue);
            }
        } catch (Exception ex) {
            logger.error("Failed to refresh sales history view", ex);
            // keep UI stable on exception: clear view to a safe state
            try {
                this.salesHistoryTableView.setItems(FXCollections.observableArrayList());
                this.totalSoldLabel.setText("0");
                this.revenueLabel.setText("0 VNĐ");
            } catch (Exception inner) {
                logger.error("Failed to set safe state after refresh error", inner);
            }
        }
    }

    private boolean applyFilter(Item item, String filter) {
        if ("Đã bán".equals(filter)) {
            return "CLOSED".equals(item.getStatus().name()) && item.getCurrentWinnerId() != null;
        } else {
            return "Đang diễn ra".equals(filter) ? "ACTIVE".equals(item.getStatus().name()) : true;
        }
    }

    private String getStatusText(String status) {
        switch (status) {
            case "PENDING":
                return "Chờ";
            case "CANCELED":
                return "Bị hủy";
            case "ACTIVE":
                return "Đang diễn ra";
            case "CLOSED":
                return "Đã kết thúc";
        }

        return status;
    }
}
