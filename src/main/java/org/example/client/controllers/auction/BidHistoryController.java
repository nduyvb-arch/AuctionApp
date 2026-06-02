package org.example.client.controllers.auction;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller điều khiển màn hình lịch sử đấu giá và biểu đồ xu hướng giá.
 */
public class BidHistoryController implements Initializable {

    @FXML
    private TextField bidHistorySearchTextField;
    @FXML
    private ComboBox<String> bidHistoryStatusComboBox;
    @FXML
    private ComboBox<String> bidHistorySortComboBox;
    @FXML
    private Button bidHistoryRefreshButton;
    @FXML
    private TableView<BidHistoryRecord> bidHistoryTable;
    @FXML
    private TableColumn<BidHistoryRecord, String> colItemName;
    @FXML
    private TableColumn<BidHistoryRecord, String> colItemType;
    @FXML
    private TableColumn<BidHistoryRecord, String> colBidAmount;
    @FXML
    private TableColumn<BidHistoryRecord, String> colBidTime;
    @FXML
    private TableColumn<BidHistoryRecord, String> colAuctionStatus;
    @FXML
    private TableColumn<BidHistoryRecord, String> colResult;

    // === CÁC THÀNH PHẦN BIỂU ĐỒ MỚI ĐƯỢC TÍCH HỢP ===
    @FXML
    private LineChart<String, Number> bidLineChart;
    @FXML
    private CategoryAxis chartXAxis;
    @FXML
    private NumberAxis chartYAxis;

    // Đối tượng quản lý đường biểu diễn giá
    private XYChart.Series<String, Number> priceSeries;
    // Biến lưu trữ ID của sản phẩm hiện đang được người dùng chọn xem biểu đồ
    private String selectedItemId = null;

    private List<BidHistoryRecord> bidHistory = new ArrayList<>();

    private static final NumberFormat currencyFormat =
            NumberFormat.getInstance(new Locale("vi_VN"));

    private static final DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public static class BidHistoryRecord {
        private final String itemId;
        private final String itemName;
        private final String itemType;
        private final double bidAmount;
        private final LocalDateTime bidTime;
        private final String auctionStatus;
        private final String result;

        public BidHistoryRecord(String itemId, String itemName, String itemType,
                                double bidAmount, LocalDateTime bidTime,
                                String auctionStatus, String result) {
            this.itemId = itemId;
            this.itemName = itemName;
            this.itemType = itemType;
            this.bidAmount = bidAmount;
            this.bidTime = bidTime;
            this.auctionStatus = auctionStatus;
            this.result = result;
        }

        public String getItemId() {
            return itemId;
        }

        public String getItemName() {
            return itemName;
        }

        public String getItemType() {
            return itemType;
        }

        public double getBidAmount() {
            return bidAmount;
        }

        public LocalDateTime getBidTime() {
            return bidTime;
        }

        public String getAuctionStatus() {
            return auctionStatus;
        }

        public String getResult() {
            return result;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBidHistoryViewFilters();
        setupBidHistoryTableColumns();
        setupRealtimeChart(); // Khởi tạo cấu hình cho biểu đồ
    }

    public void setup(List<BidHistoryRecord> bidHistory) {
        this.bidHistory = bidHistory;
        refreshBidHistoryDisplay();

        // TỰ ĐỘNG CHỌN DÒNG ĐẦU TIÊN: Hiển thị ngay biểu đồ khi vừa nạp màn hình
        if (!bidHistoryTable.getItems().isEmpty()) {
            bidHistoryTable.getSelectionModel().selectFirst();
        }
    }

    public void updateData(List<BidHistoryRecord> bidHistory) {
        this.bidHistory = bidHistory;
        refreshBidHistoryDisplay();

        // Cập nhật lại biểu đồ dựa trên tập dữ liệu mới nếu đang chọn một sản phẩm
        if (selectedItemId != null) {
            updateChartForProduct(selectedItemId);
        } else if (!bidHistoryTable.getItems().isEmpty()) {
            bidHistoryTable.getSelectionModel().selectFirst();
        }
    }

    /**
     * Khởi tạo cấu hình đồ thị và đăng ký lắng nghe sự kiện TableView click
     */
    private void setupRealtimeChart() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Xu hướng giá đặt");
        bidLineChart.getData().add(priceSeries);

        // Tắt hiệu ứng mặc định để các điểm vẽ đồ thị realtime xuất hiện ngay lập tức
        bidLineChart.setAnimated(false);

        // Không ép mốc đồ thị bắt đầu từ mức giá 0 VNĐ, giúp biểu đồ tự động zoom cận cảnh khoảng giá tranh chấp
        chartYAxis.setForceZeroInRange(false);

        // LẮNG NGHE OOP EVENT: Khi người dùng click chọn 1 dòng sản phẩm trong bảng
        bidHistoryTable.getSelectionModel().selectedItemProperty().addListener((observable, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedItemId = newSelection.getItemId();
                updateChartForProduct(selectedItemId);
            } else {
                selectedItemId = null;
                priceSeries.getData().clear();
            }
        });
    }

    /**
     * Lọc và vẽ lại toàn bộ tiến trình giá của một sản phẩm lên đồ thị (Sắp xếp tăng dần theo thời gian)
     */
    private void updateChartForProduct(String itemId) {
        // Xóa dữ liệu cũ trên biểu đồ
        priceSeries.getData().clear();

        // Lọc ra tất cả các lượt bid của sản phẩm này, sắp xếp theo thời gian tăng dần để vẽ từ trái qua phải
        List<BidHistoryRecord> productHistory = bidHistory.stream()
                .filter(r -> r.getItemId().equals(itemId))
                .sorted(Comparator.comparing(BidHistoryRecord::getBidTime))
                .collect(Collectors.toList());

        // Đẩy toàn bộ các mốc giá cũ vào biểu đồ
        for (BidHistoryRecord record : productHistory) {
            String timeLabel = record.getBidTime().format(timeFormatter);
            priceSeries.getData().add(new XYChart.Data<>(timeLabel, record.getBidAmount()));
        }
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
                new SimpleStringProperty(formatAuctionStatus(cellData.getValue().getAuctionStatus())));

        colResult.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getResult()));

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
                            setTextFill(Color.web("#22c55e"));
                            break;
                        case "Thua":
                        case "Đã bị vượt":
                            setTextFill(Color.web("#ef4444"));
                            break;
                        default:
                            setTextFill(Color.web("#64748b"));
                            break;
                    }
                }
            }
        });
    }


    private String formatAuctionStatus(String status) {
        if (status == null) {
            return "Không rõ";
        }

        switch (status.toUpperCase()) {
            case "ACTIVE":
                return "Đang diễn ra";
            case "CLOSED":
                return "Đã kết thúc";
            case "CANCELED":
                return "Bị hủy";
            case "PENDING":
                return "Chờ";
            default:
                return status;
        }
    }

    @FXML
    public void onBidHistoryRefreshClicked() {
        refreshBidHistoryDisplay();
        if (selectedItemId != null) {
            updateChartForProduct(selectedItemId);
        }
    }

    public void refreshBidHistoryView() {
        refreshBidHistoryDisplay();
    }

    private void refreshBidHistoryDisplay() {
        String searchText = bidHistorySearchTextField.getText().toLowerCase();
        String statusFilter = bidHistoryStatusComboBox.getValue();
        String sortOption = bidHistorySortComboBox.getValue();

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
        if (filter.equals(record.getResult())) return true;
        if (filter.equals(record.getAuctionStatus())) return true;
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