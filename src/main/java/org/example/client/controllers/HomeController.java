package org.example.client.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.example.client.ClientApp;
import org.example.common.Message;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    // SIDEBAR COMPONENTS
    @FXML
    private VBox sidebarMenu;
    @FXML
    private Label currentRoleLabel;
    @FXML
    private Button roleSwitcherButton;

    // MENU ITEMS
    @FXML
    private Button homeMenuItem;
    @FXML
    private Button watchlistMenuItem;
    @FXML
    private Button bidHistoryMenuItem;
    @FXML
    private Button addItemMenuItem;
    @FXML
    private Button myItemsMenuItem;
    @FXML
    private Button salesHistoryMenuItem;
    @FXML
    private Button accountMenuItem;
    @FXML
    private Button notificationsMenuItem;
    @FXML
    private Button logoutButton;

    // MENU LABELS
    @FXML
    private Label bidderMenuLabel;
    @FXML
    private Label sellerMenuLabel;

    // TOP BAR COMPONENTS
    @FXML
    private Label userInfoLabel;

    // SEARCH & FILTER
    @FXML
    private TextField searchTextField;
    @FXML
    private ComboBox<String> filterComboBox;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private Button refreshButton;

    // ITEMS DISPLAY
    @FXML
    private FlowPane itemFlowPane;

    private List<Item> items = new ArrayList<>();
    private List<Item> filteredItems = new ArrayList<>();
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User currentUser;
    private Task<Void> listenerTask; // Giữ tham chiếu đến Task để có thể hủy nó

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentUser = ClientApp.getCurrentUser();
        if (currentUser != null) {
            userInfoLabel.setText("👤 " + currentUser.getUsername() + " | Role: " + currentUser.getRole());
            updateUIBasedOnRole();
        }

        out = ClientApp.getOutputStream();
        in = ClientApp.getInputStream();

        setupFilterComboBox();
        setupSortComboBox();
        setupSearchListener();
        loadInitialItems();
        startListeningForUpdates();
    }

    private void updateUIBasedOnRole() {
        String role = currentUser.getRole().toLowerCase();
        boolean isSeller = role.contains("seller") || role.contains("người bán");
        boolean isBidder = role.contains("bidder") || role.contains("người đấu giá");

        if (isSeller) {
            currentRoleLabel.setText("🏪 Người bán");
            roleSwitcherButton.setText("🔄 Chuyển sang Người đấu giá");
            sellerMenuLabel.setVisible(true);
            addItemMenuItem.setVisible(true);
            myItemsMenuItem.setVisible(true);
            salesHistoryMenuItem.setVisible(true);
            bidderMenuLabel.setVisible(false);
            watchlistMenuItem.setVisible(false);
            bidHistoryMenuItem.setVisible(false);
        } else if (isBidder) {
            currentRoleLabel.setText("👤 Người đấu giá");
            roleSwitcherButton.setText("🔄 Chuyển sang Người bán");
            bidderMenuLabel.setVisible(true);
            watchlistMenuItem.setVisible(true);
            bidHistoryMenuItem.setVisible(true);
            sellerMenuLabel.setVisible(false);
            addItemMenuItem.setVisible(false);
            myItemsMenuItem.setVisible(false);
            salesHistoryMenuItem.setVisible(false);
        }
    }

    private void setupFilterComboBox() {
        filterComboBox.getItems().addAll("Tất cả", "Đang chạy", "Sắp mở", "Đã kết thúc");
        filterComboBox.setValue("Tất cả");
        filterComboBox.setOnAction(e -> applyFiltersAndSort());
    }

    private void setupSortComboBox() {
        sortComboBox.getItems().addAll("Mới nhất", "Giá: Thấp → Cao", "Giá: Cao → Thấp", "Gần kết thúc");
        sortComboBox.setValue("Mới nhất");
        sortComboBox.setOnAction(e -> applyFiltersAndSort());
    }

    private void setupSearchListener() {
        searchTextField.textProperty().addListener((obs, oldVal, newVal) -> applyFiltersAndSort());
    }

    private void applyFiltersAndSort() {
        String searchTerm = searchTextField.getText().toLowerCase();
        String filterType = filterComboBox.getValue();
        String sortType = sortComboBox.getValue();

        filteredItems = new ArrayList<>();
        for (Item item : items) {
            if (item.getItemName().toLowerCase().contains(searchTerm)) {
                filteredItems.add(item);
            }
        }

        if (!filterType.equals("Tất cả")) {
            filteredItems.removeIf(item -> !getItemStatus(item).equals(filterType));
        }

        switch (sortType) {
            case "Giá: Thấp → Cao":
                filteredItems.sort((a, b) -> Double.compare(a.getCurrentPrice(), b.getCurrentPrice()));
                break;
            case "Giá: Cao → Thấp":
                filteredItems.sort((a, b) -> Double.compare(b.getCurrentPrice(), a.getCurrentPrice()));
                break;
            case "Gần kết thúc":
                filteredItems.sort((a, b) -> {
                    if (a.getEndTime() == null) return 1;
                    if (b.getEndTime() == null) return -1;
                    return a.getEndTime().compareTo(b.getEndTime());
                });
                break;
        }
        refreshItemDisplay();
    }

    private String getItemStatus(Item item) {
        if (item.getEndTime() == null) return "Sắp mở";
        if (item.getEndTime().isBefore(java.time.LocalDateTime.now())) return "Đã kết thúc";
        return "Đang chạy";
    }

    private void loadInitialItems() {
        Task<ArrayList<Item>> loadItemsTask = new Task<>() {
            @Override
            protected ArrayList<Item> call() throws Exception {
                out.writeObject(new Message("GET_ALL_ITEMS", null));
                out.flush();
                Message response = (Message) in.readObject();
                if (response.getAction().equals("GET_ALL_ITEMS_RESPONSE")) {
                    return (ArrayList<Item>) response.getPayload();
                }
                return new ArrayList<>();
            }
        };
        loadItemsTask.setOnSucceeded(event -> {
            items = loadItemsTask.getValue();
            filteredItems = new ArrayList<>(items);
            refreshItemDisplay();
        });
        loadItemsTask.setOnFailed(event -> System.err.println("Lỗi khi tải danh sách vật phẩm: " + loadItemsTask.getException().getMessage()));
        new Thread(loadItemsTask).start();
    }

    private void startListeningForUpdates() {
        listenerTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("Client: Bắt đầu lắng nghe cập nhật từ server...");
                while (!isCancelled()) {
                    try {
                        Message updateMessage = (Message) in.readObject();
                        Platform.runLater(() -> {
                            if ("ITEM_UPDATE".equals(updateMessage.getAction())) {
                                Item updatedItem = (Item) updateMessage.getPayload();
                                System.out.println("Client: Nhận được cập nhật cho vật phẩm " + updatedItem.getItemName());
                                updateItemInUI(updatedItem);
                            }
                        });
                    } catch (Exception e) {
                        if (isCancelled()) {
                            System.out.println("Luồng lắng nghe đã được hủy.");
                            break;
                        }
                        System.err.println("Mất kết nối với server: " + e.getMessage());
                        break;
                    }
                }
                return null;
            }
        };
        Thread listenerThread = new Thread(listenerTask);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void refreshItemDisplay() {
        itemFlowPane.getChildren().clear();
        for (Item item : filteredItems) {
            itemFlowPane.getChildren().add(createItemNode(item));
        }
    }

    private void updateItemInUI(Item updatedItem) {
        items.removeIf(item -> item.getId().equals(updatedItem.getId()));
        items.add(updatedItem);
        applyFiltersAndSort();
    }

    private Node createItemNode(Item item) {
        AnchorPane pane = new AnchorPane();
        pane.setUserData(item.getId());
        pane.setPrefSize(300, 380);
        pane.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 12, 0, 0, 4);");

        String status = getItemStatus(item);
        Label statusBadge = new Label(status);
        statusBadge.setFont(new Font("System Bold", 10));
        statusBadge.setTextFill(Color.WHITE);
        statusBadge.setPadding(new javafx.geometry.Insets(5, 10, 5, 10));
        statusBadge.setStyle(getStatusBadgeStyle(status));
        AnchorPane.setTopAnchor(statusBadge, 12.0);
        AnchorPane.setRightAnchor(statusBadge, 12.0);

        Label nameLabel = new Label(item.getItemName());
        nameLabel.setFont(new Font("System Bold", 16));
        nameLabel.setTextFill(Color.web("#0f172a"));
        nameLabel.setWrapText(true);
        AnchorPane.setTopAnchor(nameLabel, 15.0);
        AnchorPane.setLeftAnchor(nameLabel, 15.0);
        AnchorPane.setRightAnchor(nameLabel, 70.0);

        Label typeLabel = new Label(item.getType());
        typeLabel.setFont(new Font("System", 11));
        typeLabel.setTextFill(Color.web("#64748b"));
        AnchorPane.setTopAnchor(typeLabel, 52.0);
        AnchorPane.setLeftAnchor(typeLabel, 15.0);

        Label descLabel = new Label(item.getDescription());
        descLabel.setFont(new Font("System", 11));
        descLabel.setTextFill(Color.web("#64748b"));
        descLabel.setWrapText(true);
        descLabel.setPrefWidth(270);
        AnchorPane.setTopAnchor(descLabel, 75.0);
        AnchorPane.setLeftAnchor(descLabel, 15.0);

        Label priceLabel = new Label("Giá hiện tại:");
        priceLabel.setFont(new Font("System", 11));
        priceLabel.setTextFill(Color.web("#64748b"));
        AnchorPane.setTopAnchor(priceLabel, 150.0);
        AnchorPane.setLeftAnchor(priceLabel, 15.0);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        Label priceValueLabel = new Label(currencyFormat.format(item.getCurrentPrice()));
        priceValueLabel.setFont(new Font("System Bold", 20));
        priceValueLabel.setTextFill(Color.web("#2563eb"));
        AnchorPane.setTopAnchor(priceValueLabel, 168.0);
        AnchorPane.setLeftAnchor(priceValueLabel, 15.0);

        Label incrementLabel = new Label("Bước giá tối thiểu: " + currencyFormat.format(item.getBidIncrement()));
        incrementLabel.setFont(new Font("System", 10));
        incrementLabel.setTextFill(Color.web("#94a3b8"));
        AnchorPane.setTopAnchor(incrementLabel, 200.0);
        AnchorPane.setLeftAnchor(incrementLabel, 15.0);

        Button bidButton = new Button("💰 Đặt giá");
        bidButton.setFont(new Font("System Bold", 11));
        bidButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 10 15; -fx-background-radius: 6; -fx-cursor: hand;");
        bidButton.setOnAction(e -> showBidDialog(item));
        AnchorPane.setBottomAnchor(bidButton, 12.0);
        AnchorPane.setLeftAnchor(bidButton, 15.0);
        AnchorPane.setRightAnchor(bidButton, 100.0);

        Button viewDetailsButton = new Button("👁 Chi tiết");
        viewDetailsButton.setFont(new Font("System Bold", 11));
        viewDetailsButton.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #0f172a; -fx-padding: 10 15; -fx-background-radius: 6; -fx-cursor: hand;");
        viewDetailsButton.setOnAction(e -> showItemDetails(item));
        AnchorPane.setBottomAnchor(viewDetailsButton, 12.0);
        AnchorPane.setRightAnchor(viewDetailsButton, 15.0);

        Button watchlistButton = new Button("⭐");
        watchlistButton.setFont(new Font("System Bold", 14));
        watchlistButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #f59e0b; -fx-padding: 5; -fx-cursor: hand;");
        watchlistButton.setOnAction(e -> addToWatchlist(item));
        AnchorPane.setTopAnchor(watchlistButton, 12.0);
        AnchorPane.setLeftAnchor(watchlistButton, 12.0);

        pane.getChildren().addAll(statusBadge, nameLabel, typeLabel, descLabel, priceLabel, priceValueLabel, incrementLabel, bidButton, viewDetailsButton, watchlistButton);
        return pane;
    }

    private String getStatusBadgeStyle(String status) {
        return switch (status) {
            case "Đang chạy" -> "-fx-background-color: #10b981; -fx-background-radius: 6;";
            case "Sắp mở" -> "-fx-background-color: #f59e0b; -fx-background-radius: 6;";
            case "Đã kết thúc" -> "-fx-background-color: #6b7280; -fx-background-radius: 6;";
            default -> "-fx-background-color: #6b7280; -fx-background-radius: 6;";
        };
    }

    private void showBidDialog(Item item) { System.out.println("Hiển thị dialog đặt giá cho: " + item.getItemName()); }
    private void showItemDetails(Item item) { System.out.println("Hiển thị chi tiết sản phẩm: " + item.getItemName()); }
    private void addToWatchlist(Item item) { System.out.println("Thêm vào danh sách theo dõi: " + item.getItemName()); }

    @FXML
    public void onRoleSwitcherClicked() {
        // ... (Logic chuyển đổi vai trò)
    }

    @FXML
    public void onMenuItemClicked() {
        // ... (Logic xử lý menu item)
    }

    @FXML
    public void onRefreshClicked() {
        loadInitialItems();
    }

    @FXML
    public void onLogoutClicked() {
        // 1. Hủy luồng lắng nghe để nó không còn đọc stream
        if (listenerTask != null) {
            listenerTask.cancel(true); // true để ngắt các hoạt động blocking như readObject()
        }
        // 2. Đóng kết nối Socket và dọn dẹp stream
        ClientApp.closeConnection();
        // 3. Xóa thông tin người dùng hiện tại
        ClientApp.setCurrentUser(null);
        
        try {
            // 4. Chuyển về màn hình đăng nhập
            ClientApp.switchToLogin();
        } catch (Exception e) {
            System.err.println("Error switching to login: " + e.getMessage());
        }
    }
}
