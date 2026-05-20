package org.example.client.controllers.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.example.common.Message;
import org.example.common.model.item.AuctionStatus;
import org.example.common.model.item.Item;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminItemsController implements AdminChildController {

    @FXML private TextField searchField;
    @FXML private TableView<Item> itemsTable;
    @FXML private TableColumn<Item, String> idColumn;
    @FXML private TableColumn<Item, String> nameColumn;
    @FXML private TableColumn<Item, String> typeColumn;
    @FXML private TableColumn<Item, String> sellerColumn;
    @FXML private TableColumn<Item, String> priceColumn;
    @FXML private TableColumn<Item, String> statusColumn;
    @FXML private TableColumn<Item, String> winnerColumn;

    private AdminDashboardController dashboardController;
    private final ObservableList<Item> displayedItems = FXCollections.observableArrayList();
    private List<Item> allItems = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    public void setup(AdminDashboardController dashboardController) {
        this.dashboardController = dashboardController;
        setupTable();
        requestItems();
    }

    private void setupTable() {
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getItemName()));
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        sellerColumn.setCellValueFactory(data -> new SimpleStringProperty(nullToDash(data.getValue().getSellerId())));
        priceColumn.setCellValueFactory(data -> new SimpleStringProperty(currencyFormat.format(data.getValue().getCurrentPrice()) + " VNĐ"));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(formatStatus(data.getValue().getStatus())));
        winnerColumn.setCellValueFactory(data -> new SimpleStringProperty(nullToDash(data.getValue().getCurrentWinnerId())));
        itemsTable.setItems(displayedItems);
    }

    @FXML
    private void onRefreshClicked() {
        requestItems();
    }

    @FXML
    private void onSearchChanged() {
        applySearch();
    }

    @FXML
    private void onViewDetailClicked() {
        Item selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            dashboardController.showError("Chi tiết sản phẩm", "Bạn cần chọn một sản phẩm.");
            return;
        }

        String detail = "Mã SP: " + selected.getId()
                + "\nTên: " + selected.getItemName()
                + "\nLoại: " + selected.getType()
                + "\nNgười bán: " + nullToDash(selected.getSellerId())
                + "\nNgười đang dẫn: " + nullToDash(selected.getCurrentWinnerId())
                + "\nGiá khởi điểm: " + currencyFormat.format(selected.getStartingPrice()) + " VNĐ"
                + "\nGiá hiện tại: " + currencyFormat.format(selected.getCurrentPrice()) + " VNĐ"
                + "\nBước giá: " + currencyFormat.format(selected.getBidIncrement()) + " VNĐ"
                + "\nTrạng thái: " + formatStatus(selected.getStatus())
                + "\nMô tả: " + nullToDash(selected.getDescription());

        dashboardController.showInfo("Chi tiết sản phẩm", detail);
    }

    @FXML
    private void onDeleteClicked() {
        Item selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            dashboardController.showError("Xóa sản phẩm", "Bạn cần chọn một sản phẩm.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xóa sản phẩm");
        confirm.setHeaderText("Xóa sản phẩm: " + selected.getItemName());
        confirm.setContentText("Hành động này sẽ xóa sản phẩm khỏi danh sách và xóa lịch sử đặt giá liên quan. Bạn chắc chắn muốn tiếp tục?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            dashboardController.sendMessage(new Message("DELETE_ITEM_ADMIN", selected.getId()));
        }
    }

    @Override
    public void handleServerMessage(Message message) {
        switch (message.getAction()) {
            case "GET_ALL_ITEMS_ADMIN_RESPONSE":
                allItems = castList(message.getPayload());
                applySearch();
                break;
            case "DELETE_ITEM_ADMIN_RESPONSE":
                dashboardController.showInfo("Quản lý sản phẩm", String.valueOf(message.getPayload()));
                requestItems();
                break;
            case "ITEM_UPDATE":
            case "NEW_ITEM_ADDED":
                requestItems();
                break;
            default:
                break;
        }
    }

    private void requestItems() {
        dashboardController.sendMessage(new Message("GET_ALL_ITEMS_ADMIN", null));
    }

    private void applySearch() {
        String keyword = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        List<Item> filtered = allItems.stream()
                .filter(item -> keyword.isEmpty()
                        || item.getId().toLowerCase().contains(keyword)
                        || item.getItemName().toLowerCase().contains(keyword)
                        || item.getType().toLowerCase().contains(keyword)
                        || String.valueOf(item.getSellerId()).toLowerCase().contains(keyword)
                        || item.getStatus().name().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        displayedItems.setAll(filtered);
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
