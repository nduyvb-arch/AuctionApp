package org.example.client.controllers.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.example.common.Message;
import org.example.common.model.item.AuctionStatus;
import org.example.common.model.item.Item;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminAuctionsController implements AdminChildController {

    @FXML private TableView<Item> auctionsTable;
    @FXML private TableColumn<Item, String> idColumn;
    @FXML private TableColumn<Item, String> nameColumn;
    @FXML private TableColumn<Item, String> priceColumn;
    @FXML private TableColumn<Item, String> winnerColumn;
    @FXML private TableColumn<Item, String> endTimeColumn;
    @FXML private TableColumn<Item, String> statusColumn;
    @FXML private Button cancelButton;
    @FXML private Button endButton;

    private AdminDashboardController dashboardController;
    private final ObservableList<Item> displayedAuctions = FXCollections.observableArrayList();
    private List<Item> allItems = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void setup(AdminDashboardController dashboardController) {
        this.dashboardController = dashboardController;
        setupTable();
        requestAuctions();
    }

    private void setupTable() {
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getItemName()));
        priceColumn.setCellValueFactory(data -> new SimpleStringProperty(currencyFormat.format(data.getValue().getCurrentPrice()) + " VNĐ"));
        winnerColumn.setCellValueFactory(data -> new SimpleStringProperty(nullToDash(data.getValue().getCurrentWinnerId())));
        endTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEndTime() == null ? "-" : data.getValue().getEndTime().format(dateTimeFormatter)));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(formatStatus(data.getValue().getStatus())));
        auctionsTable.setItems(displayedAuctions);
        auctionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> updateActionButtons());
        updateActionButtons();
    }

    @FXML
    private void onRefreshClicked() {
        requestAuctions();
    }

    @FXML
    private void onShowActiveClicked() {
        displayedAuctions.setAll(allItems.stream()
                .filter(item -> item.getStatus() == AuctionStatus.ACTIVE)
                .collect(Collectors.toList()));
        updateActionButtons();
    }

    @FXML
    private void onShowAllClicked() {
        displayedAuctions.setAll(allItems);
        updateActionButtons();
    }

    @FXML
    private void onCancelClicked() {
        Item selected = auctionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Hủy phiên đấu giá");
        confirm.setHeaderText("Hủy phiên: " + selected.getItemName());
        confirm.setContentText("Nếu đã có người dẫn đầu, hệ thống sẽ hoàn tiền cho người đó. Bạn chắc chắn muốn hủy?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            dashboardController.sendMessage(new Message("CANCEL_AUCTION_ADMIN", selected.getId()));
        }
    }

    @FXML
    private void onEndClicked() {
        Item selected = auctionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Kết thúc phiên đấu giá");
        confirm.setHeaderText("Kết thúc phiên: " + selected.getItemName());
        confirm.setContentText("Nếu có người thắng, tiền đang giữ sẽ được chuyển cho người bán. Bạn chắc chắn muốn kết thúc?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            dashboardController.sendMessage(new Message("END_AUCTION_ADMIN", selected.getId()));
        }
    }

    @Override
    public void handleServerMessage(Message message) {
        switch (message.getAction()) {
            case "GET_ALL_ITEMS_ADMIN_RESPONSE":
                allItems = castList(message.getPayload());
                displayedAuctions.setAll(allItems);
                updateActionButtons();
                break;
            case "CANCEL_AUCTION_RESPONSE":
            case "END_AUCTION_ADMIN_RESPONSE":
                dashboardController.showInfo("Quản lý phiên đấu giá", String.valueOf(message.getPayload()));
                requestAuctions();
                break;
            case "ITEM_UPDATE":
            case "NEW_ITEM_ADDED":
                requestAuctions();
                break;
            default:
                break;
        }
    }

    private void requestAuctions() {
        dashboardController.sendMessage(new Message("GET_ALL_ITEMS_ADMIN", null));
    }

    private void updateActionButtons() {
        Item selected = auctionsTable == null ? null : auctionsTable.getSelectionModel().getSelectedItem();
        boolean active = selected != null && selected.getStatus() == AuctionStatus.ACTIVE;
        if (cancelButton != null) {
            cancelButton.setDisable(!active);
        }
        if (endButton != null) {
            endButton.setDisable(!active);
        }
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
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
