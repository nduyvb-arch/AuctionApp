package org.example.client.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.application.Platform;
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
 * Controller riêng cho màn hình Danh sách theo dõi (Watchlist).
 * Được nhúng vào HomeMenu.fxml qua fx:include.
 * Dữ liệu (items, watchlistItemIds, streams, user) được HomeController
 * truyền vào qua phương thức {@link #setup}.
 */
public class WatchlistController implements Initializable {

    // ===== FXML COMPONENTS =====
    @FXML private TextField  watchlistSearchTextField;
    @FXML private ComboBox<String> watchlistFilterComboBox;
    @FXML private ComboBox<String> watchlistSortComboBox;
    @FXML private Button     watchlistRefreshButton;
    @FXML private FlowPane   watchlistFlowPane;

    // ===== SHARED DATA (được truyền từ HomeController) =====
    private List<Item>          items             = new ArrayList<>();
    private Set<String>         watchlistItemIds  = new HashSet<>();
    private ObjectOutputStream  out;
    private ObjectInputStream   in;
    private User                currentUser;

    private static final NumberFormat currencyFormat =
            NumberFormat.getInstance(new Locale("vi_VN"));

    // ============================================================
    // JAVAFX INITIALIZE
    // ============================================================

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Chỉ khởi tạo ComboBox – dữ liệu thực sẽ được set sau qua setup()
        setupWatchlistViewFilters();
    }

    /**
     * Được gọi bởi HomeController sau khi FXML được load.
     * Truyền toàn bộ dữ liệu dùng chung và làm mới giao diện.
     */
    public void setup(List<Item> items, Set<String> watchlistItemIds,
                      ObjectOutputStream out, ObjectInputStream in, User currentUser) {
        this.items            = items;
        this.watchlistItemIds = watchlistItemIds;
        this.out              = out;
        this.in               = in;
        this.currentUser      = currentUser;
        refreshWatchlistDisplay();
    }

    /**
     * Cập nhật tham chiếu items/watchlistItemIds khi HomeController tải lại dữ liệu,
     * rồi làm mới giao diện (dùng khi server push ITEM_UPDATE).
     */
    public void updateData(List<Item> items, Set<String> watchlistItemIds) {
        this.items            = items;
        this.watchlistItemIds = watchlistItemIds;
        refreshWatchlistDisplay();
    }

    // ============================================================
    // SETUP BỘ LỌC & SẮP XẾP
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

        watchlistFilterComboBox.setOnAction(e -> refreshWatchlistDisplay());
        watchlistSortComboBox.setOnAction(e -> refreshWatchlistDisplay());
        watchlistSearchTextField.setOnKeyReleased(e -> refreshWatchlistDisplay());
    }

    // ============================================================
    // HANDLER NÚT LÀM MỚI
    // ============================================================

    @FXML
    public void onWatchlistRefreshClicked() {
        refreshWatchlistDisplay();
    }

    // ============================================================
    // HIỂN THỊ DANH SÁCH THEO DÕI
    // ============================================================

    /** Được gọi từ HomeController khi chuyển sang màn hình này. */
    public void refreshWatchlistView() {
        refreshWatchlistDisplay();
    }

    private void refreshWatchlistDisplay() {
        watchlistFlowPane.getChildren().clear();

        String searchText   = watchlistSearchTextField.getText().toLowerCase();
        String statusFilter = watchlistFilterComboBox.getValue();
        String sortOption   = watchlistSortComboBox.getValue();

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
    // BỘ LỌC & SẮP XẾP
    // ============================================================

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

    // ============================================================
    // TẠO THẺ SẢN PHẨM (giữ nguyên logic từ HomeController)
    // ============================================================

    private Node createItemCard(Item item) {
        AnchorPane pane = new AnchorPane();
        pane.setStyle("-fx-border-color: #e2e8f0; -fx-border-radius: 12; " +
                "-fx-background-radius: 12; -fx-background-color: white; -fx-padding: 15;");
        pane.setPrefSize(235, 285);
        pane.setMinSize(235, 285);
        pane.setMaxSize(235, 285);

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
        Button viewDetailsButton = new Button("👁");
        viewDetailsButton.setFont(new Font("System Bold", 14));
        viewDetailsButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 8 12; " +
                "-fx-background-radius: 6; -fx-cursor: hand;");
        AnchorPane.setBottomAnchor(viewDetailsButton, 12.0);
        AnchorPane.setRightAnchor(viewDetailsButton, 35.0);

        // Watchlist Toggle Button
        boolean inWatchlist = watchlistItemIds.contains(item.getId());
        Button watchlistButton = new Button(inWatchlist ? "⭐" : "☆");
        watchlistButton.setFont(new Font("System Bold", 14));
        watchlistButton.setStyle("-fx-background-color: transparent; -fx-text-fill: " +
                (inWatchlist ? "#f59e0b" : "#cbd5e1") +
                "; -fx-padding: 8 12; -fx-cursor: hand;");
        watchlistButton.setOnAction(e -> {
            toggleWatchlist(item, watchlistButton);
            // Làm mới lại view sau khi toggle
            refreshWatchlistDisplay();
        });
        AnchorPane.setBottomAnchor(watchlistButton, 12.0);
        AnchorPane.setRightAnchor(watchlistButton, 12.0);

        pane.getChildren().addAll(statusBadge, nameLabel, typeLabel, descLabel,
                priceLabel, priceValueLabel, incrementLabel,
                bidButton, viewDetailsButton, watchlistButton);
        return pane;
    }

    // ============================================================
    // HÀM TIỆN ÍCH TRẠNG THÁI SẢN PHẨM
    // ============================================================

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

    // ============================================================
    // TOGGLE WATCHLIST
    // ============================================================

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

    // ============================================================
    // MỞ DIALOG ĐẶT GIÁ
    // ============================================================

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
                Message request  = new Message("BID", bidData);
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
}
