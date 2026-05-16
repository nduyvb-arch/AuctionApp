package org.example.client.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.client.ClientApp;
import org.example.common.Message;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller chính của HomeMenu.fxml.
 * Quản lý:
 *   - Sidebar / navigation
 *   - Top bar (tiêu đề trang, thông tin user)
 *   - Home View (danh sách sản phẩm, bộ lọc)
 *   - Dữ liệu dùng chung: items, watchlistItemIds
 *
 * Các màn hình con (Watchlist, BidHistory) được tách thành FXML + Controller riêng
 * và nhúng vào HomeMenu.fxml qua {@code <fx:include>}.
 * HomeController giao tiếp với chúng thông qua {@link WatchlistController}
 * và {@link BidHistoryController}.
 */
public class HomeController implements Initializable {

    // ═══════════════════════════════════════════════════════════
    // SIDEBAR COMPONENTS
    // ═══════════════════════════════════════════════════════════
    @FXML private VBox    sidebarMenu;
    @FXML private Label   currentRoleLabel;
    @FXML private Button  roleSwitcherButton;

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

    // ═══════════════════════════════════════════════════════════
    // TOP BAR COMPONENTS
    // ═══════════════════════════════════════════════════════════
    @FXML private Label pageTitle;
    @FXML private Label userInfoLabel;

    // ═══════════════════════════════════════════════════════════
    // HOME VIEW COMPONENTS
    // ═══════════════════════════════════════════════════════════
    @FXML private VBox              homeView;
    @FXML private TextField         searchTextField;
    @FXML private ComboBox<String>  filterComboBox;
    @FXML private ComboBox<String>  sortComboBox;
    @FXML private Button            refreshButton;
    @FXML private FlowPane          itemFlowPane;

    // ═══════════════════════════════════════════════════════════
    // SUB-VIEW ROOTS (được inject từ fx:include)
    // Quy tắc đặt tên: fx:id="watchlistViewPane"
    //   → @FXML VBox watchlistViewPane          (root node)
    //   → @FXML WatchlistController watchlistViewPaneController (controller)
    // ═══════════════════════════════════════════════════════════
    @FXML private VBox                 watchlistViewPane;
    @FXML private WatchlistController  watchlistViewPaneController;

    @FXML private VBox                  bidHistoryViewPane;
    @FXML private BidHistoryController  bidHistoryViewPaneController;

    @FXML private VBox                   addItemViewPane;
    @FXML private AddItemViewController  addItemViewPaneController;

    @FXML private VBox                   myItemsViewPane;
    @FXML private MyItemsController      myItemsViewPaneController;

    @FXML private VBox                   salesHistoryViewPane;
    @FXML private SalesHistoryController salesHistoryViewPaneController;

    // CONTENT CONTAINER (bọc tất cả views)
    @FXML private VBox contentContainer;

    // ═══════════════════════════════════════════════════════════
    // SHARED DATA
    // ═══════════════════════════════════════════════════════════
    private List<Item>    items            = new ArrayList<>();
    private Set<String>   watchlistItemIds = new HashSet<>();

    // Bid history được quản lý bởi BidHistoryController;
    // HomeController giữ reference để có thể thêm record khi cần.
    private List<BidHistoryController.BidHistoryRecord> bidHistory = new ArrayList<>();

    private ObjectOutputStream  out;
    private ObjectInputStream   in;
    private User                currentUser;
    private boolean             sellerMode;

    private static final NumberFormat currencyFormat =
            NumberFormat.getInstance(new Locale("vi_VN"));

    // ═══════════════════════════════════════════════════════════
    // INITIALIZE
    // ═══════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 1. Thông tin người dùng
        currentUser = ClientApp.getCurrentUser();
        if (currentUser != null) {
            sellerMode = "seller".equalsIgnoreCase(currentUser.getRole());
            updateUserInfoLabel();
            updateUIBasedOnRole();
        }

        // 2. Streams giao tiếp với server
        out = ClientApp.getOutputStream();
        in  = ClientApp.getInputStream();

        // 3. Thiết lập bộ lọc cho Home View
        setupHomeViewFilters();

        // 4. Ẩn các sub-view, chỉ hiện Home View mặc định
        setViewState(homeView, true);
        setViewState(watchlistViewPane, false);
        setViewState(bidHistoryViewPane, false);
        setViewState(addItemViewPane, false);
        setViewState(myItemsViewPane, false);
        setViewState(salesHistoryViewPane, false);

        // 5. Khởi động các sub-controllers với dữ liệu dùng chung
        watchlistViewPaneController.setup(items, watchlistItemIds, out, in, currentUser);
        bidHistoryViewPaneController.setup(bidHistory);
        addItemViewPaneController.setup(out, currentUser, this::loadInitialItems);
        myItemsViewPaneController.setup(items, out, currentUser, this::loadInitialItems);
        salesHistoryViewPaneController.setup(items, currentUser, this::loadInitialItems);

        // 6. Lắng nghe cập nhật/phản hồi từ server qua một luồng đọc duy nhất
        listenForServerUpdates();

        // 7. Tải danh sách sản phẩm từ server
        loadInitialItems();
    }

    // ═══════════════════════════════════════════════════════════
    // CHUYỂN MÀN HÌNH
    // ═══════════════════════════════════════════════════════════

    @FXML
    private void switchToHomeView() {
        showView(homeView);
        pageTitle.setText("Trang chủ sàn đấu giá");
        loadInitialItems();
    }

    @FXML
    private void switchToWatchlistView() {
        showView(watchlistViewPane);
        pageTitle.setText("⭐ Danh sách theo dõi");
        // Cập nhật dữ liệu mới nhất trước khi hiển thị
        watchlistViewPaneController.updateData(items, watchlistItemIds);
    }

    @FXML
    private void switchToBidHistoryView() {
        showView(bidHistoryViewPane);
        pageTitle.setText("📊 Lịch sử đấu giá");
        bidHistoryViewPaneController.refreshBidHistoryView();
    }

    @FXML
    private void switchToAddItemView() {
        showView(addItemViewPane);
        pageTitle.setText("➕ Đăng sản phẩm mới");
    }

    @FXML
    private void switchToMyItemsView() {
        showView(myItemsViewPane);
        pageTitle.setText("📦 Sản phẩm của tôi");
        myItemsViewPaneController.updateData(items);
    }

    @FXML
    private void switchToSalesHistoryView() {
        showView(salesHistoryViewPane);
        pageTitle.setText("💰 Lịch sử bán hàng");
        salesHistoryViewPaneController.updateData(items);
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

    /** Ẩn tất cả views con, rồi hiện {@code view}. */
    private void showView(VBox view) {
        hideAllViews();
        setViewState(view, true);
    }

    private void hideAllViews() {
        setViewState(homeView, false);
        setViewState(watchlistViewPane, false);
        setViewState(bidHistoryViewPane, false);
        setViewState(addItemViewPane, false);
        setViewState(myItemsViewPane, false);
        setViewState(salesHistoryViewPane, false);
    }

    /**
     * visible=false chỉ làm node biến mất nhưng vẫn có thể chiếm layout trong VBox.
     * managed=false giúp VBox bỏ qua node đó khi tính toán vị trí/khoảng trống.
     */
    private void setViewState(VBox view, boolean active) {
        if (view == null) return;
        view.setVisible(active);
        view.setManaged(active);
    }

    // ═══════════════════════════════════════════════════════════
    // HOME VIEW – SETUP, TẢI DỮ LIỆU, LỌC & SẮP XẾP
    // ═══════════════════════════════════════════════════════════

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
                synchronized (out) {
                    out.writeObject(new Message("GET_ALL_ITEMS", null));
                    out.flush();
                }
                return null;
            }
        };
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void applyHomeFiltersAndSort() {
        String searchText   = searchTextField.getText().toLowerCase();
        String statusFilter = filterComboBox.getValue();
        String sortOption   = sortComboBox.getValue();

        List<Item> filtered = items.stream()
                .filter(item -> item.getItemName().toLowerCase().contains(searchText))
                .filter(item -> applyStatusFilter(item, statusFilter))
                .collect(Collectors.toList());

        applySorting(filtered, sortOption);
        displayItems(filtered);
    }

    private boolean applyStatusFilter(Item item, String filter) {
        if ("Tất cả".equals(filter))          return true;
        if ("Chờ".equals(filter))             return "PENDING".equals(item.getStatus().name());
        if ("Đang diễn ra".equals(filter))    return "ACTIVE".equals(item.getStatus().name());
        if ("Đã kết thúc".equals(filter))     return "CLOSED".equals(item.getStatus().name());
        if ("Bị hủy".equals(filter))          return "CANCELED".equals(item.getStatus().name());
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
            default:
                break;
        }
    }

    private void displayItems(List<Item> itemsToDisplay) {
        itemFlowPane.getChildren().clear();
        for (Item item : itemsToDisplay) {
            itemFlowPane.getChildren().add(createItemCard(item));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // TẠO THẺ SẢN PHẨM (Home View)
    // ═══════════════════════════════════════════════════════════

    private Node createItemCard(Item item) {
        AnchorPane pane = new AnchorPane();
        pane.setStyle("-fx-border-color: #e2e8f0; -fx-border-radius: 12; " +
                "-fx-background-color: white; -fx-padding: 15;");
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
        Label descLabel = new Label(item.getDescription().isEmpty()
                ? "Không có mô tả" : item.getDescription());
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

        Label priceValueLabel = new Label(
                currencyFormat.format(item.getCurrentPrice()) + " VNĐ");
        priceValueLabel.setFont(new Font("System Bold", 13));
        priceValueLabel.setStyle("-fx-text-fill: #3b82f6;");
        AnchorPane.setTopAnchor(priceValueLabel, 175.0);
        AnchorPane.setLeftAnchor(priceValueLabel, 12.0);

        // Bid Increment
        Label incrementLabel = new Label(
                "Mức tăng: +" + currencyFormat.format(item.getBidIncrement()) + " VNĐ");
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
        viewDetailsButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; " +
                "-fx-padding: 8 12; -fx-background-radius: 6; -fx-cursor: hand;");
        AnchorPane.setBottomAnchor(viewDetailsButton, 12.0);
        AnchorPane.setRightAnchor(viewDetailsButton, 35.0);

        // Watchlist Toggle Button
        Button watchlistButton = new Button(watchlistItemIds.contains(item.getId()) ? "⭐" : "☆");
        watchlistButton.setFont(new Font("System Bold", 14));
        watchlistButton.setStyle("-fx-background-color: transparent; -fx-text-fill: " +
                (watchlistItemIds.contains(item.getId()) ? "#f59e0b" : "#cbd5e1") +
                "; -fx-padding: 8 12; -fx-cursor: hand;");
        watchlistButton.setOnAction(e -> toggleWatchlist(item, watchlistButton));
        AnchorPane.setBottomAnchor(watchlistButton, 12.0);
        AnchorPane.setRightAnchor(watchlistButton, 12.0);

        pane.getChildren().addAll(statusBadge, nameLabel, typeLabel, descLabel,
                priceLabel, priceValueLabel, incrementLabel,
                bidButton, viewDetailsButton, watchlistButton);
        return pane;
    }

    private String getStatusText(String status) {
        switch (status) {
            case "PENDING":  return "Chờ";
            case "ACTIVE":   return "Đang diễn ra";
            case "CLOSED":   return "Đã kết thúc";
            case "CANCELED": return "Bị hủy";
            default:         return status;
        }
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "PENDING":  return "#94a3b8";
            case "ACTIVE":   return "#10b981";
            case "CLOSED":   return "#8b5cf6";
            case "CANCELED": return "#ef4444";
            default:         return "#6b7280";
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ĐẶT GIÁ
    // ═══════════════════════════════════════════════════════════

    private void openBidDialog(Item item) {
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
                synchronized (out) {
                    out.writeObject(new Message("BID", bidData));
                    out.flush();
                }
                return null;
            }
        };
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // ═══════════════════════════════════════════════════════════
    // TOGGLE WATCHLIST (từ Home View)
    // ═══════════════════════════════════════════════════════════

    private void toggleWatchlist(Item item, Button button) {
        if (watchlistItemIds.contains(item.getId())) {
            watchlistItemIds.remove(item.getId());
            button.setText("☆");
            button.setStyle("-fx-background-color: transparent; -fx-text-fill: #cbd5e1; " +
                    "-fx-padding: 8 12; -fx-cursor: hand;");
        } else {
            watchlistItemIds.add(item.getId());
            button.setText("⭐");
            button.setStyle("-fx-background-color: transparent; -fx-text-fill: #f59e0b; " +
                    "-fx-padding: 8 12; -fx-cursor: hand;");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ROLE SWITCHER
    // ═══════════════════════════════════════════════════════════

    private void updateUIBasedOnRole() {
        boolean isSeller = sellerMode;
        boolean isBidder = !sellerMode;

        bidderMenuLabel.setVisible(isBidder);
        bidderMenuLabel.setManaged(isBidder);
        watchlistMenuItem.setVisible(isBidder);
        watchlistMenuItem.setManaged(isBidder);
        bidHistoryMenuItem.setVisible(isBidder);
        bidHistoryMenuItem.setManaged(isBidder);

        sellerMenuLabel.setVisible(isSeller);
        sellerMenuLabel.setManaged(isSeller);
        addItemMenuItem.setVisible(isSeller);
        addItemMenuItem.setManaged(isSeller);
        myItemsMenuItem.setVisible(isSeller);
        myItemsMenuItem.setManaged(isSeller);
        salesHistoryMenuItem.setVisible(isSeller);
        salesHistoryMenuItem.setManaged(isSeller);

        currentRoleLabel.setText(isSeller ? "Người bán" : "Người đấu giá");
        roleSwitcherButton.setText(isSeller
                ? "🔄 Chuyển sang Người đấu giá"
                : "🔄 Chuyển sang Người bán");
        updateUserInfoLabel();
    }

    private void updateUserInfoLabel() {
        if (currentUser == null) return;
        userInfoLabel.setText("👤 " + currentUser.getUsername()
                + " | Role: " + (sellerMode ? "seller" : "bidder"));
    }

    @FXML
    public void onRoleSwitcherClicked() {
        sellerMode = !sellerMode;
        String newRole = sellerMode ? "seller" : "bidder";
        currentUser.setRole(newRole);
        updateUIBasedOnRole();

        if (sellerMode) {
            switchToAddItemView();
        } else {
            switchToHomeView();
        }

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                synchronized (out) {
                    out.writeObject(new Message("SWITCH_ROLE", newRole));
                    out.flush();
                }
                return null;
            }
        };
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // ═══════════════════════════════════════════════════════════
    // LẮNG NGHE CẬP NHẬT TỪ SERVER
    // ═══════════════════════════════════════════════════════════

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
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void handleServerMessage(Message message) {
        switch (message.getAction()) {
            case "GET_ALL_ITEMS_RESPONSE":
                updateItemsFromServer((List<Item>) message.getPayload());
                break;

            case "BID_RESPONSE":
                Alert bidAlert = new Alert(Alert.AlertType.INFORMATION);
                bidAlert.setTitle("Kết quả đặt giá");
                bidAlert.setHeaderText("Phản hồi từ server");
                bidAlert.setContentText(String.valueOf(message.getPayload()));
                bidAlert.showAndWait();
                loadInitialItems();
                break;

            case "ITEM_UPDATE":
                Item updatedItem = (Item) message.getPayload();
                items.stream()
                        .filter(i -> i.getId().equals(updatedItem.getId()))
                        .findFirst()
                        .ifPresent(i -> {
                            items.remove(i);
                            items.add(updatedItem);
                            // Làm mới view đang hiện
                            if (homeView.isVisible()) {
                                applyHomeFiltersAndSort();
                            }
                            if (watchlistViewPane.isVisible()) {
                                // watchlistItemIds đã là reference dùng chung → chỉ cần refresh
                                watchlistViewPaneController.updateData(items, watchlistItemIds);
                            }
                            if (myItemsViewPane.isVisible()) {
                                myItemsViewPaneController.updateData(items);
                            }
                            if (salesHistoryViewPane.isVisible()) {
                                salesHistoryViewPaneController.updateData(items);
                            }
                        });
                break;

            case "NEW_ITEM_ADDED":
                loadInitialItems();
                break;

            case "ADD_ITEM_RESPONSE":
            case "START_AUCTION_RESPONSE":
            case "SWITCH_ROLE_RESPONSE":
                System.out.println("Server: " + message.getPayload());
                loadInitialItems();
                break;

            case "SYSTEM_NOTIFICATION":
                System.out.println("Server: " + message.getPayload());
                break;
        }
    }


    private void updateItemsFromServer(List<Item> fetchedItems) {
        items.clear();
        items.addAll(fetchedItems);

        if (homeView.isVisible()) {
            applyHomeFiltersAndSort();
        }
        if (watchlistViewPane.isVisible()) {
            watchlistViewPaneController.updateData(items, watchlistItemIds);
        }
        if (myItemsViewPane.isVisible()) {
            myItemsViewPaneController.updateData(items);
        }
        if (salesHistoryViewPane.isVisible()) {
            salesHistoryViewPaneController.updateData(items);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ĐĂNG XUẤT
    // ═══════════════════════════════════════════════════════════

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
