package org.example.client.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.client.ClientApp;
import org.example.common.Message;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;
import org.example.common.model.user.Bidder;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class HomeController implements Initializable {

    // SIDEBAR COMPONENTS
    @FXML private VBox sidebarMenu;
    @FXML private Label currentRoleLabel;
    @FXML private Button roleSwitcherButton;

    // MENU ITEMS
    @FXML private Button homeMenuItem;
    @FXML private Button watchlistMenuItem;
    @FXML private Button bidHistoryMenuItem;
    @FXML private Button addItemMenuItem;
    @FXML private Button myItemsMenuItem;
    @FXML private Button salesHistoryMenuItem;
    @FXML private Button accountMenuItem;
    @FXML private Button notificationsMenuItem;
    @FXML private Button logoutButton;

    // MENU LABELS
    @FXML private Label bidderMenuLabel;
    @FXML private Label sellerMenuLabel;

    // TOP BAR COMPONENTS
    @FXML private Label pageTitle;
    @FXML private Label userInfoLabel;

    // ===== HOME VIEW =====
    @FXML private TextField searchTextField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private Button refreshButton;
    @FXML private FlowPane itemFlowPane;

    // ===== WATCHLIST VIEW =====
    @FXML private VBox watchlistView;
    @FXML private TextField watchlistSearchTextField;
    @FXML private ComboBox<String> watchlistFilterComboBox;
    @FXML private ComboBox<String> watchlistSortComboBox;
    @FXML private Button watchlistRefreshButton;
    @FXML private FlowPane watchlistFlowPane;

    // ===== BID HISTORY VIEW =====
    @FXML private VBox bidHistoryView;
    @FXML private TextField bidHistorySearchTextField;
    @FXML private ComboBox<String> bidHistoryStatusComboBox;
    @FXML private ComboBox<String> bidHistorySortComboBox;
    @FXML private Button bidHistoryRefreshButton;
    @FXML private TableView<BidHistoryRecord> bidHistoryTable;
    @FXML private TableColumn<BidHistoryRecord, String> colItemName;
    @FXML private TableColumn<BidHistoryRecord, String> colItemType;
    @FXML private TableColumn<BidHistoryRecord, String> colBidAmount;
    @FXML private TableColumn<BidHistoryRecord, String> colBidTime;
    @FXML private TableColumn<BidHistoryRecord, String> colAuctionStatus;
    @FXML private TableColumn<BidHistoryRecord, String> colResult;

    // ===== VIEW CONTAINERS =====
    @FXML private VBox homeView;
    @FXML private VBox contentContainer;

    // DATA
    private List<Item> items = new ArrayList<>();
    private List<Item> watchlistItems = new ArrayList<>();
    private List<BidHistoryRecord> bidHistory = new ArrayList<>();
    private Set<String> watchlistItemIds = new HashSet<>();

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User currentUser;
    private static final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi_VN"));

    // ===== INNER CLASS FOR BID HISTORY RECORD =====
    public static class BidHistoryRecord {
        private String itemId;
        private String itemName;
        private String itemType;
        private double bidAmount;
        private LocalDateTime bidTime;
        private String auctionStatus;
        private String result;

        public BidHistoryRecord(String itemId, String itemName, String itemType,
                                double bidAmount, LocalDateTime bidTime, String auctionStatus, String result) {
            this.itemId = itemId;
            this.itemName = itemName;
            this.itemType = itemType;
            this.bidAmount = bidAmount;
            this.bidTime = bidTime;
            this.auctionStatus = auctionStatus;
            this.result = result;
        }

        // Getters
        public String getItemId() { return itemId; }
        public String getItemName() { return itemName; }
        public String getItemType() { return itemType; }
        public double getBidAmount() { return bidAmount; }
        public LocalDateTime getBidTime() { return bidTime; }
        public String getAuctionStatus() { return auctionStatus; }
        public String getResult() { return result; }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 1. Lấy thông tin người dùng hiện tại
        currentUser = ClientApp.getCurrentUser();
        if (currentUser != null) {
            userInfoLabel.setText("👤 " + currentUser.getUsername() + " | Role: " + currentUser.getRole());
            updateUIBasedOnRole();
        }

        // 2. Lấy stream để giao tiếp với server
        out = ClientApp.getOutputStream();
        in = ClientApp.getInputStream();

        // 3. Setup filter & sort combobox cho HOME VIEW
        setupHomeViewFilters();

        // 4. Setup filter & sort combobox cho WATCHLIST VIEW
        setupWatchlistViewFilters();

        // 5. Setup filter & sort combobox cho BID HISTORY VIEW
        setupBidHistoryViewFilters();

        // 6. Setup TableView columns cho BID HISTORY
        setupBidHistoryTableColumns();

        // 7. Load items từ server
        loadInitialItems();

        // 8. Lắng nghe server updates
        listenForServerUpdates();
    }

    // ============================================================
    // VIEW SWITCHING METHODS
    // ============================================================

    @FXML
    private void switchToHomeView() {
        showView(homeView);
        pageTitle.setText("🏠 Trang chủ sàn đấu giá");
        loadInitialItems();
    }

    @FXML
    private void switchToWatchlistView() {
        showView(watchlistView);
        pageTitle.setText("⭐ Danh sách theo dõi");
        refreshWatchlistView();
    }

    @FXML
    private void switchToBidHistoryView() {
        showView(bidHistoryView);
        pageTitle.setText("📊 Lịch sử đấu giá");
        refreshBidHistoryView();
    }

    @FXML
    private void switchToAddItemView() {
        pageTitle.setText("➕ Đăng sản phẩm mới");
        hideAllViews();
        // TODO: Implement Add Item View
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Đăng Sản Phẩm");
        alert.setHeaderText("Chức năng này sẽ được cải thiện");
        alert.setContentText("Hiện tại, vui lòng sử dụng dialog.");
        alert.showAndWait();
    }

    @FXML
    private void switchToMyItemsView() {
        pageTitle.setText("📦 Sản phẩm của tôi");
        hideAllViews();
        // TODO: Implement My Items View
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sản Phẩm Của Tôi");
        alert.setHeaderText("Chức năng này sẽ được cải thiện");
        alert.setContentText("Hiện tại, vui lòng xem ở trang chủ.");
        alert.showAndWait();
    }

    @FXML
    private void switchToSalesHistoryView() {
        pageTitle.setText("💰 Lịch sử bán hàng");
        hideAllViews();
        // TODO: Implement Sales History View
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Lịch Sử Bán Hàng");
        alert.setHeaderText("Chức năng này sẽ được cải thiện");
        alert.setContentText("Hiện tại, đang phát triển.");
        alert.showAndWait();
    }

    @FXML
    private void switchToAccountView() {
        pageTitle.setText("👤 Tài khoản");
        hideAllViews();
        // TODO: Implement Account View
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Tài Khoản");
        alert.setHeaderText("Chức năng này sẽ được cải thiện");
        alert.setContentText("Hiện tại, đang phát triển.");
        alert.showAndWait();
    }

    @FXML
    private void switchToNotificationsView() {
        pageTitle.setText("🔔 Thông báo");
        hideAllViews();
        // TODO: Implement Notifications View
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông Báo");
        alert.setHeaderText("Chức năng này sẽ được cải thiện");
        alert.setContentText("Hiện tại, đang phát triển.");
        alert.showAndWait();
    }

    private void showView(VBox view) {
        hideAllViews();
        view.setVisible(true);
    }

    private void hideAllViews() {
        homeView.setVisible(false);
        watchlistView.setVisible(false);
        bidHistoryView.setVisible(false);
    }

    // ============================================================
    // HOME VIEW SETUP & REFRESH
    // ============================================================

    private void setupHomeViewFilters() {
        ObservableList<String> statuses = FXCollections.observableArrayList(
                "Tất cả", "Chờ", "Đang diễn ra", "Đã kết thúc", "Bị hủy"
        );
        filterComboBox.setItems(statuses);
        filterComboBox.setValue("Tất cả");

        ObservableList<String> sorts = FXCollections.observableArrayList(
                "Mặc định", "Giá thấp → cao", "Giá cao → thấp", "Sắp hết hạn"
        );
        sortComboBox.setItems(sorts);
        sortComboBox.setValue("Mặc định");

        // Add listeners
        filterComboBox.setOnAction(e -> applyHomeFiltersAndSort());
        sortComboBox.setOnAction(e -> applyHomeFiltersAndSort());
        searchTextField.setOnKeyReleased(e -> applyHomeFiltersAndSort());
    }

    @FXML
    public void onRefreshClicked() {
        loadInitialItems();
    }

    private void loadInitialItems() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Message request = new Message("GET_ALL_ITEMS", null);
                out.writeObject(request);
                Message response = (Message) in.readObject();

                if ("GET_ALL_ITEMS_RESPONSE".equals(response.getAction())) {
                    List<Item> fetchedItems = (List<Item>) response.getPayload();
                    Platform.runLater(() -> {
                        items = fetchedItems;
                        applyHomeFiltersAndSort();
                    });
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void applyHomeFiltersAndSort() {
        String searchText = searchTextField.getText().toLowerCase();
        String statusFilter = filterComboBox.getValue();
        String sortOption = sortComboBox.getValue();

        List<Item> filtered = items.stream()
                .filter(item -> item.getItemName().toLowerCase().contains(searchText))
                .filter(item -> applyStatusFilter(item, statusFilter))
                .collect(Collectors.toList());

        // Apply sorting
        applySorting(filtered, sortOption);

        // Display items
        displayItems(filtered);
    }

    private boolean applyStatusFilter(Item item, String filter) {
        if ("Tất cả".equals(filter)) return true;
        if ("Chờ".equals(filter)) return "PENDING".equals(item.getStatus().name());
        if ("Đang diễn ra".equals(filter)) return "ACTIVE".equals(item.getStatus().name());
        if ("Đã kết thúc".equals(filter)) return "CLOSED".equals(item.getStatus().name());
        if ("Bị hủy".equals(filter)) return "CANCELED".equals(item.getStatus().name());
        return true;
    }

    private void applySorting(List<Item> itemList, String sortOption) {
        switch (sortOption) {
            case "Giá thấp → cao":
                itemList.sort(Comparator.comparingDouble(Item::getCurrentPrice));
                break;
            case "Giá cao → thấp":
                itemList.sort(Comparator.comparingDouble(Item::getCurrentPrice).reversed());
                break;
            case "Sắp hết hạn":
                itemList.sort((a, b) -> {
                    if (a.getEndTime() == null) return 1;
                    if (b.getEndTime() == null) return -1;
                    return a.getEndTime().compareTo(b.getEndTime());
                });
                break;
            default: // Mặc định - không sắp xếp
                break;
        }
    }

    private void displayItems(List<Item> itemsToDisplay) {
        itemFlowPane.getChildren().clear();
        for (Item item : itemsToDisplay) {
            itemFlowPane.getChildren().add(createItemCard(item));
        }
    }

    private Node createItemCard(Item item) {
        AnchorPane pane = new AnchorPane();
        pane.setStyle("-fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-background-color: white; -fx-padding: 15;");
        pane.setPrefSize(220, 280);

        // Status Badge
        Label statusBadge = new Label(getStatusText(item.getStatus().name()));
        statusBadge.setStyle("-fx-background-color: " + getStatusColor(item.getStatus().name()) +
                "; -fx-text-fill: white; -fx-padding: 5 10; -fx-border-radius: 5; -fx-font-size: 10;");
        AnchorPane.setTopAnchor(statusBadge, 12.0);
        AnchorPane.setRightAnchor(statusBadge, 12.0);

        // Item Name
        Label nameLabel = new Label(item.getItemName());
        nameLabel.setFont(new Font("System Bold", 14));
        nameLabel.setWrapText(true);
        AnchorPane.setTopAnchor(nameLabel, 50.0);
        AnchorPane.setLeftAnchor(nameLabel, 12.0);
        AnchorPane.setRightAnchor(nameLabel, 12.0);

        // Item Type
        Label typeLabel = new Label("Loại: " + item.getType());
        typeLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");
        AnchorPane.setTopAnchor(typeLabel, 95.0);
        AnchorPane.setLeftAnchor(typeLabel, 12.0);

        // Description
        Label descLabel = new Label(item.getDescription().isEmpty() ? "Không có mô tả" : item.getDescription());
        descLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10;");
        descLabel.setWrapText(true);
        descLabel.setPrefHeight(40);
        AnchorPane.setTopAnchor(descLabel, 115.0);
        AnchorPane.setLeftAnchor(descLabel, 12.0);
        AnchorPane.setRightAnchor(descLabel, 12.0);

        // Current Price
        Label priceLabel = new Label("Giá hiện tại:");
        priceLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");
        AnchorPane.setTopAnchor(priceLabel, 160.0);
        AnchorPane.setLeftAnchor(priceLabel, 12.0);

        Label priceValueLabel = new Label(currencyFormat.format(item.getCurrentPrice()) + " VNĐ");
        priceValueLabel.setFont(new Font("System Bold", 13));
        priceValueLabel.setStyle("-fx-text-fill: #3b82f6;");
        AnchorPane.setTopAnchor(priceValueLabel, 175.0);
        AnchorPane.setLeftAnchor(priceValueLabel, 12.0);

        // Bid Increment
        Label incrementLabel = new Label("Mức tăng: +" + currencyFormat.format(item.getBidIncrement()) + " VNĐ");
        incrementLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10;");
        AnchorPane.setTopAnchor(incrementLabel, 195.0);
        AnchorPane.setLeftAnchor(incrementLabel, 12.0);

        // Bid Button
        Button bidButton = new Button("🔨 Đặt giá");
        bidButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 8 12; " +
                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: bold;");
        bidButton.setOnAction(e -> openBidDialog(item));
        AnchorPane.setBottomAnchor(bidButton, 12.0);
        AnchorPane.setLeftAnchor(bidButton, 12.0);
        AnchorPane.setRightAnchor(bidButton, 70.0);

        // View Details Button
        Button viewDetailsButton = new Button("📄");
        viewDetailsButton.setFont(new Font("System Bold", 14));
        viewDetailsButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 8 12; " +
                "-fx-background-radius: 6; -fx-cursor: hand;");
        AnchorPane.setBottomAnchor(viewDetailsButton, 12.0);
        AnchorPane.setRightAnchor(viewDetailsButton, 35.0);

        // Watchlist Button
        Button watchlistButton = new Button(watchlistItemIds.contains(item.getId()) ? "⭐" : "☆");
        watchlistButton.setFont(new Font("System Bold", 14));
        watchlistButton.setStyle("-fx-background-color: transparent; -fx-text-fill: " +
                (watchlistItemIds.contains(item.getId()) ? "#f59e0b" : "#cbd5e1") +
                "; -fx-padding: 8 12; -fx-cursor: hand;");
        watchlistButton.setOnAction(e -> toggleWatchlist(item, watchlistButton));
        AnchorPane.setBottomAnchor(watchlistButton, 12.0);
        AnchorPane.setRightAnchor(watchlistButton, 12.0);

        pane.getChildren().addAll(statusBadge, nameLabel, typeLabel, descLabel, priceLabel,
                priceValueLabel, incrementLabel, bidButton, viewDetailsButton, watchlistButton);
        return pane;
    }

    private String getStatusText(String status) {
        switch (status) {
            case "PENDING": return "Chờ";
            case "ACTIVE": return "Đang diễn ra";
            case "CLOSED": return "Đã kết thúc";
            case "CANCELED": return "Bị hủy";
            default: return status;
        }
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "PENDING": return "#94a3b8";
            case "ACTIVE": return "#10b981";
            case "CLOSED": return "#8b5cf6";
            case "CANCELED": return "#ef4444";
            default: return "#6b7280";
        }
    }

    private void openBidDialog(Item item) {
        // TODO: Implement Bid Dialog
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Đặt Giá - " + item.getItemName());
        dialog.setHeaderText("Nhập mức giá mà bạn muốn đặt");

        TextField amountField = new TextField();
        amountField.setPromptText("Nhập giá tiền...");

        ButtonType submitButton = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButton, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(
                new Label("Giá hiện tại: " + currencyFormat.format(item.getCurrentPrice()) + " VNĐ"),
                new Label("Mức tăng tối thiểu: " + currencyFormat.format(item.getBidIncrement()) + " VNĐ"),
                new Label("Nhập giá của bạn:"),
                amountField
        );

        dialog.getDialogPane().setContent(content);

        Optional<Double> result = dialog.showAndWait();
        result.ifPresent(amount -> submitBid(item.getId(), amount));
    }

    private void submitBid(String itemId, double bidAmount) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Object[] bidData = {itemId, bidAmount, currentUser.getId()};
                Message request = new Message("BID", bidData);
                out.writeObject(request);
                Message response = (Message) in.readObject();

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Kết quả đặt giá");
                    alert.setHeaderText("Phản hồi từ server");
                    alert.setContentText((String) response.getPayload());
                    alert.showAndWait();
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    private void toggleWatchlist(Item item, Button button) {
        if (watchlistItemIds.contains(item.getId())) {
            watchlistItemIds.remove(item.getId());
            button.setText("☆");
            button.setStyle("-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-padding: 8 12; -fx-cursor: hand;");
        } else {
            watchlistItemIds.add(item.getId());
            button.setText("⭐");
            button.setStyle("-fx-background-color: transparent; -fx-text-fill: #f59e0b; -fx-padding: 8 12; -fx-cursor: hand;");
        }
    }

    private void addToWatchlist(Item item) {
        if (!watchlistItemIds.contains(item.getId())) {
            watchlistItemIds.add(item.getId());
        }
    }

    // ============================================================
    // WATCHLIST VIEW SETUP & REFRESH
    // ============================================================

    private void setupWatchlistViewFilters() {
        ObservableList<String> filters = FXCollections.observableArrayList(
                "Tất cả", "Đang diễn ra", "Chờ", "Đã kết thúc"
        );
        watchlistFilterComboBox.setItems(filters);
        watchlistFilterComboBox.setValue("Tất cả");

        ObservableList<String> sorts = FXCollections.observableArrayList(
                "Mặc định", "Giá thấp → cao", "Giá cao → thấp", "Sắp hết hạn"
        );
        watchlistSortComboBox.setItems(sorts);
        watchlistSortComboBox.setValue("Mặc định");

        watchlistFilterComboBox.setOnAction(e -> refreshWatchlistView());
        watchlistSortComboBox.setOnAction(e -> refreshWatchlistView());
        watchlistSearchTextField.setOnKeyReleased(e -> refreshWatchlistView());
    }

    @FXML
    public void onWatchlistRefreshClicked() {
        refreshWatchlistView();
    }

    private void refreshWatchlistView() {
        watchlistFlowPane.getChildren().clear();

        String searchText = watchlistSearchTextField.getText().toLowerCase();
        String statusFilter = watchlistFilterComboBox.getValue();
        String sortOption = watchlistSortComboBox.getValue();

        List<Item> filtered = items.stream()
                .filter(item -> watchlistItemIds.contains(item.getId()))
                .filter(item -> item.getItemName().toLowerCase().contains(searchText))
                .filter(item -> applyStatusFilter(item, statusFilter))
                .collect(Collectors.toList());

        applySorting(filtered, sortOption);

        if (filtered.isEmpty()) {
            Label emptyLabel = new Label("📭 Danh sách theo dõi của bạn trống. Hãy thêm sản phẩm!");
            emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14;");
            watchlistFlowPane.getChildren().add(emptyLabel);
        } else {
            for (Item item : filtered) {
                watchlistFlowPane.getChildren().add(createItemCard(item));
            }
        }
    }

    // ============================================================
    // BID HISTORY VIEW SETUP & REFRESH
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

        bidHistoryStatusComboBox.setOnAction(e -> refreshBidHistoryView());
        bidHistorySortComboBox.setOnAction(e -> refreshBidHistoryView());
        bidHistorySearchTextField.setOnKeyReleased(e -> refreshBidHistoryView());
    }

    private void setupBidHistoryTableColumns() {
        colItemName.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getItemName()));
        colItemType.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getItemType()));
        colBidAmount.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        currencyFormat.format(cellData.getValue().getBidAmount()) + " VNĐ"));
        colBidTime.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getBidTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        colAuctionStatus.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAuctionStatus()));
        colResult.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getResult()));
    }

    @FXML
    public void onBidHistoryRefreshClicked() {
        refreshBidHistoryView();
    }

    private void refreshBidHistoryView() {
        String searchText = bidHistorySearchTextField.getText().toLowerCase();
        String statusFilter = bidHistoryStatusComboBox.getValue();
        String sortOption = bidHistorySortComboBox.getValue();

        List<BidHistoryRecord> filtered = bidHistory.stream()
                .filter(record -> record.getItemName().toLowerCase().contains(searchText))
                .filter(record -> applyBidHistoryStatusFilter(record, statusFilter))
                .collect(Collectors.toList());

        applyBidHistorySorting(filtered, sortOption);

        ObservableList<BidHistoryRecord> tableData = FXCollections.observableArrayList(filtered);
        bidHistoryTable.setItems(tableData);
    }

    private boolean applyBidHistoryStatusFilter(BidHistoryRecord record, String filter) {
        if ("Tất cả".equals(filter)) return true;
        if ("Đang diễn ra".equals(filter)) return "ACTIVE".equals(record.getAuctionStatus());
        if ("Thắng".equals(filter)) return "Thắng".equals(record.getResult());
        if ("Thua".equals(filter)) return "Thua".equals(record.getResult());
        if ("Chờ".equals(filter)) return "Chờ".equals(record.getResult());
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

    // ============================================================
    // UTILITY METHODS
    // ============================================================

    private void updateUIBasedOnRole() {
        String role = currentUser.getRole();
        boolean isBidder = "bidder".equalsIgnoreCase(role);
        boolean isSeller = "seller".equalsIgnoreCase(role);

        bidderMenuLabel.setVisible(isBidder);
        watchlistMenuItem.setVisible(isBidder);
        bidHistoryMenuItem.setVisible(isBidder);

        sellerMenuLabel.setVisible(isSeller);
        addItemMenuItem.setVisible(isSeller);
        myItemsMenuItem.setVisible(isSeller);
        salesHistoryMenuItem.setVisible(isSeller);

        if (isBidder) {
            roleSwitcherButton.setText("🔄 Chuyển sang Người bán");
        } else if (isSeller) {
            roleSwitcherButton.setText("🔄 Chuyển sang Người đấu giá");
        }
    }

    @FXML
    public void onRoleSwitcherClicked() {
        String newRole = "bidder".equalsIgnoreCase(currentUser.getRole()) ? "seller" : "bidder";
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Message request = new Message("SWITCH_ROLE", newRole);
                out.writeObject(request);
                Message response = (Message) in.readObject();

                Platform.runLater(() -> {
                    if ("success".equals(response.getPayload())) {
                        currentUser.setRole(newRole);
                        currentRoleLabel.setText("bidder".equalsIgnoreCase(newRole) ?
                                "Người đấu giá" : "Người bán");
                        updateUIBasedOnRole();

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Thành công");
                        alert.setContentText("Đã chuyển vai trò thành: " + newRole);
                        alert.showAndWait();
                    }
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    private void listenForServerUpdates() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while (true) {
                    Message message = (Message) in.readObject();
                    if (message != null) {
                        Platform.runLater(() -> handleServerMessage(message));
                    }
                }
            }
        };
        new Thread(task).setDaemon(true);
        new Thread(task).start();
    }

    private void handleServerMessage(Message message) {
        switch (message.getAction()) {
            case "ITEM_UPDATE":
                Item updatedItem = (Item) message.getPayload();
                items.stream()
                        .filter(i -> i.getId().equals(updatedItem.getId()))
                        .findFirst()
                        .ifPresent(i -> {
                            items.remove(i);
                            items.add(updatedItem);
                            if (homeView.isVisible()) applyHomeFiltersAndSort();
                            if (watchlistView.isVisible()) refreshWatchlistView();
                        });
                break;
            case "SYSTEM_NOTIFICATION":
                System.out.println("Server: " + message.getPayload());
                break;
        }
    }

    @FXML
    public void onLogoutClicked() {
        ClientApp.setCurrentUser(null);
        try {
            ClientApp.switchToLogin();
        } catch (Exception e) {
            System.err.println("Error switching to login: " + e.getMessage());
        }
    }
}
