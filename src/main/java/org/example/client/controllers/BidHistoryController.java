package org.example.client.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class BidHistoryController implements Initializable {

    @FXML private TextField              bidHistorySearchTextField;
    @FXML private ComboBox<String>       bidHistoryStatusComboBox;
    @FXML private ComboBox<String>       bidHistorySortComboBox;
    @FXML private Button                 bidHistoryRefreshButton;
    @FXML private TableView<BidHistoryRecord>              bidHistoryTable;
    @FXML private TableColumn<BidHistoryRecord, String>    colItemName;
    @FXML private TableColumn<BidHistoryRecord, String>    colItemType;
    @FXML private TableColumn<BidHistoryRecord, String>    colBidAmount;
    @FXML private TableColumn<BidHistoryRecord, String>    colBidTime;
    @FXML private TableColumn<BidHistoryRecord, String>    colAuctionStatus;
    @FXML private TableColumn<BidHistoryRecord, String>    colResult;

    private List<BidHistoryRecord> bidHistory = new ArrayList<>();

    private static final NumberFormat currencyFormat =
            NumberFormat.getInstance(new Locale("vi_VN"));

    public static class BidHistoryRecord {
        private final String        itemId;
        private final String        itemName;
        private final String        itemType;
        private final double        bidAmount;
        private final LocalDateTime bidTime;
        private final String        auctionStatus;
        private final String        result;

        public BidHistoryRecord(String itemId, String itemName, String itemType,
                                double bidAmount, LocalDateTime bidTime,
                                String auctionStatus, String result) {
            this.itemId        = itemId;
            this.itemName      = itemName;
            this.itemType      = itemType;
            this.bidAmount     = bidAmount;
            this.bidTime       = bidTime;
            this.auctionStatus = auctionStatus;
            this.result        = result;
        }

        public String        getItemId()        { return itemId; }
        public String        getItemName()      { return itemName; }
        public String        getItemType()      { return itemType; }
        public double        getBidAmount()     { return bidAmount; }
        public LocalDateTime getBidTime()       { return bidTime; }
        public String        getAuctionStatus() { return auctionStatus; }
        public String        getResult()        { return result; }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBidHistoryViewFilters();
        setupBidHistoryTableColumns();
    }

    public void setup(List<BidHistoryRecord> bidHistory) {
        this.bidHistory = bidHistory;
        refreshBidHistoryDisplay();
    }

    public void updateData(List<BidHistoryRecord> bidHistory) {
        this.bidHistory = bidHistory;
        refreshBidHistoryDisplay();
    }

    private void setupBidHistoryViewFilters() {
        ObservableList<String> statuses = FXCollections.observableArrayList(
                "Tất cả", "Đang diễn ra", "Thắng", "Thua", "Bị hủy", "Chờ"
        );
        bidHistoryStatusComboBox.setItems(statuses);
        bidHistoryStatusComboBox.setValue("Tất cả");

        ObservableList<String> sorts = FXCollections.observableArrayList(
                "Mới nhất", "Cũ nhất", "Giá cao → thấp", "Giá thấp → cao"
        );
        bidHistorySortComboBox.setItems(sorts);
        bidHistorySortComboBox.setValue("Mới nhất");

        bidHistoryStatusComboBox.setOnAction(e -> refreshBidHistoryDisplay());
        bidHistorySortComboBox.setOnAction(e -> refreshBidHistoryDisplay());
        bidHistorySearchTextField.setOnKeyReleased(e -> refreshBidHistoryDisplay());
    }

    private void setupBidHistoryTableColumns() {
        colItemName.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getItemName()));

        colItemType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getItemType()));

        colBidAmount.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        currencyFormat.format(cellData.getValue().getBidAmount()) + " VNĐ"));

        colBidTime.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getBidTime()
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));

        colAuctionStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAuctionStatus()));

        colResult.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getResult()));

        // Thêm màu sắc cho cột kết quả
        colResult.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setFont(Font.font("System", FontWeight.BOLD, 12));
                    switch (item) {
                        case "Thắng":
                            setTextFill(Color.web("#22c55e")); // Green
                            break;
                        case "Thua":
                            setTextFill(Color.web("#ef4444")); // Red
                            break;
                        default:
                            setTextFill(Color.web("#64748b")); // Slate
                            break;
                    }
                }
            }
        });
    }

    @FXML
    public void onBidHistoryRefreshClicked() {
        refreshBidHistoryDisplay();
    }

    public void refreshBidHistoryView() {
        refreshBidHistoryDisplay();
    }

    private void refreshBidHistoryDisplay() {
        String searchText   = bidHistorySearchTextField.getText().toLowerCase();
        String statusFilter = bidHistoryStatusComboBox.getValue();
        String sortOption   = bidHistorySortComboBox.getValue();

        List<BidHistoryRecord> filtered = bidHistory.stream()
                .filter(r -> r.getItemName().toLowerCase().contains(searchText))
                .filter(r -> applyBidHistoryStatusFilter(r, statusFilter))
                .collect(Collectors.toList());

        applyBidHistorySorting(filtered, sortOption);

        ObservableList<BidHistoryRecord> tableData =
                FXCollections.observableArrayList(filtered);
        bidHistoryTable.setItems(tableData);
    }

    private boolean applyBidHistoryStatusFilter(BidHistoryRecord record, String filter) {
        if (filter == null || "Tất cả".equals(filter)) return true;
        
        // So sánh kết quả đã được tính toán
        if (filter.equals(record.getResult())) return true;
        
        // So sánh trạng thái gốc cho các trường hợp khác
        if (filter.equals(record.getAuctionStatus())) return true;

        // Xử lý cho bộ lọc "Đang diễn ra"
        if ("Đang diễn ra".equals(filter) && "ACTIVE".equalsIgnoreCase(record.getAuctionStatus())) return true;

        return false;
    }

    private void applyBidHistorySorting(List<BidHistoryRecord> recordList, String sortOption) {
        switch (sortOption) {
            case "Mới nhất":
                recordList.sort(Comparator.comparing(BidHistoryRecord::getBidTime).reversed());
                break;
            case "Cũ nhất":
                recordList.sort(Comparator.comparing(BidHistoryRecord::getBidTime));
                break;
            case "Giá cao → thấp":
                recordList.sort(Comparator.comparingDouble(BidHistoryRecord::getBidAmount).reversed());
                break;
            case "Giá thấp → cao":
                recordList.sort(Comparator.comparingDouble(BidHistoryRecord::getBidAmount));
                break;
            default:
                break;
        }
    }
}
