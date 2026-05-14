package org.example.client.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller riêng cho màn hình Lịch sử đấu giá (Bid History).
 * Được nhúng vào HomeMenu.fxml qua fx:include.
 * Dữ liệu (bidHistory) được HomeController truyền vào qua {@link #setup}.
 */
public class BidHistoryController implements Initializable {

    // ===== FXML COMPONENTS =====
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

    // ===== DATA =====
    private List<BidHistoryRecord> bidHistory = new ArrayList<>();

    private static final NumberFormat currencyFormat =
            NumberFormat.getInstance(new Locale("vi_VN"));

    // ============================================================
    // RECORD NỘI TUYẾN – lưu thông tin một lần đấu giá
    // ============================================================

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

    // ============================================================
    // JAVAFX INITIALIZE
    // ============================================================

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBidHistoryViewFilters();
        setupBidHistoryTableColumns();
    }

    /**
     * Được gọi bởi HomeController sau khi FXML load xong.
     * Truyền danh sách lịch sử đấu giá và làm mới bảng.
     */
    public void setup(List<BidHistoryRecord> bidHistory) {
        this.bidHistory = bidHistory;
        refreshBidHistoryDisplay();
    }

    /**
     * Cập nhật lại danh sách rồi làm mới bảng
     * (dùng khi HomeController nhận dữ liệu mới từ server).
     */
    public void updateData(List<BidHistoryRecord> bidHistory) {
        this.bidHistory = bidHistory;
        refreshBidHistoryDisplay();
    }

    // ============================================================
    // SETUP BỘ LỌC, SẮP XẾP & CỘT BẢNG
    // ============================================================

    private void setupBidHistoryViewFilters() {
        ObservableList<String> statuses = FXCollections.observableArrayList(
                "Tất cả", "Đang diễn ra", "Thắng", "Thua", "Chờ"
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
    }

    // ============================================================
    // HANDLER NÚT LÀM MỚI
    // ============================================================

    @FXML
    public void onBidHistoryRefreshClicked() {
        refreshBidHistoryDisplay();
    }

    // ============================================================
    // HIỂN THỊ LỊCH SỬ ĐẤU GIÁ
    // ============================================================

    /** Được gọi từ HomeController khi chuyển sang màn hình này. */
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

    // ============================================================
    // BỘ LỌC & SẮP XẾP
    // ============================================================

    private boolean applyBidHistoryStatusFilter(BidHistoryRecord record, String filter) {
        if ("Tất cả".equals(filter))          return true;
        if ("Đang diễn ra".equals(filter))    return "ACTIVE".equals(record.getAuctionStatus());
        if ("Thắng".equals(filter))           return "Thắng".equals(record.getResult());
        if ("Thua".equals(filter))            return "Thua".equals(record.getResult());
        if ("Chờ".equals(filter))             return "Chờ".equals(record.getResult());
        return true;
    }

    private void applyBidHistorySorting(List<BidHistoryRecord> recordList, String sortOption) {
        switch (sortOption) {
            case "Mới nhất":
                recordList.sort((a, b) -> b.getBidTime().compareTo(a.getBidTime()));
                break;
            case "Cũ nhất":
                recordList.sort(Comparator.comparing(BidHistoryRecord::getBidTime));
                break;
            case "Giá cao → thấp":
                recordList.sort((a, b) -> Double.compare(b.getBidAmount(), a.getBidAmount()));
                break;
            case "Giá thấp → cao":
                recordList.sort(Comparator.comparingDouble(BidHistoryRecord::getBidAmount));
                break;
            default:
                break;
        }
    }
}
