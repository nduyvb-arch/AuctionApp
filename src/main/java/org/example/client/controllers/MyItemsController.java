package org.example.client.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Font;
import javafx.util.Duration;
import org.example.client.ClientApp; // ADDED
import org.example.common.Message;
import org.example.common.model.item.AuctionStatus;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MyItemsController implements Initializable {

    @FXML private TextField myItemsSearchTextField;
    @FXML private ComboBox<String> myItemsStatusComboBox;
    @FXML private ComboBox<String> myItemsSortComboBox;
    @FXML private FlowPane myItemsFlowPane;
    @FXML private Label myItemsSummaryLabel;

    private List<Item> items = new ArrayList<>();
    private User currentUser;
    private Runnable onItemsChanged;

    private final List<Timeline> runningTimelines = new ArrayList<>();
    private static final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        myItemsStatusComboBox.getItems().setAll("Tất cả", "Chờ", "Đang diễn ra", "Đã kết thúc", "Bị hủy");
        myItemsStatusComboBox.setValue("Tất cả");

        myItemsSortComboBox.getItems().setAll("Mặc định", "Giá thấp → cao", "Giá cao → thấp", "Sắp hết hạn");
        myItemsSortComboBox.setValue("Mặc định");

        myItemsSearchTextField.setOnKeyReleased(e -> refreshMyItemsView());
        myItemsStatusComboBox.setOnAction(e -> refreshMyItemsView());
        myItemsSortComboBox.setOnAction(e -> refreshMyItemsView());
    }

    // XÓA THAM SỐ out
    public void setup(List<Item> items, User currentUser, Runnable onItemsChanged) {
        this.items = items;
        this.currentUser = currentUser;
        this.onItemsChanged = onItemsChanged;
        refreshMyItemsView();
    }

    public void updateData(List<Item> items) {
        this.items = items;
        refreshMyItemsView();
    }

    @FXML
    private void onMyItemsRefreshClicked() {
        if (onItemsChanged != null) onItemsChanged.run();
        refreshMyItemsView();
    }

    public void refreshMyItemsView() {
        stopAllTimelines();
        myItemsFlowPane.getChildren().clear();

        if (currentUser == null) {
            myItemsFlowPane.getChildren().add(createEmptyLabel("Bạn chưa đăng nhập."));
            return;
        }

        String search = myItemsSearchTextField.getText() == null ? "" : myItemsSearchTextField.getText().toLowerCase();
        String status = myItemsStatusComboBox.getValue();
        String sort = myItemsSortComboBox.getValue();

        List<Item> filtered = items.stream()
                .filter(item -> String.valueOf(currentUser.getId()).equals(item.getSellerId()))
                .filter(item -> item.getItemName() != null && item.getItemName().toLowerCase().contains(search))
                .filter(item -> applyStatusFilter(item, status))
                .collect(Collectors.toList());

        applySorting(filtered, sort);
        myItemsSummaryLabel.setText("Tổng: " + filtered.size() + " sản phẩm");

        if (filtered.isEmpty()) {
            myItemsFlowPane.getChildren().add(createEmptyLabel("Bạn chưa có sản phẩm nào.\nHãy sang mục Đăng sản phẩm mới."));
            return;
        }

        for (Item item : filtered) {
            myItemsFlowPane.getChildren().add(createItemCard(item));
        }
    }

    // [Các hàm helper lọc, vẽ UI giữ nguyên...]
    private boolean applyStatusFilter(Item item, String filter) {
        if (filter == null || "Tất cả".equals(filter)) return true;
        String status = getDisplayStatus(item);
        if ("Chờ".equals(filter)) return "PENDING".equals(status);
        if ("Đang diễn ra".equals(filter)) return "ACTIVE".equals(status);
        if ("Đã kết thúc".equals(filter)) return "CLOSED".equals(status);
        if ("Bị hủy".equals(filter)) return "CANCELED".equals(status);
        return true;
    }

    private void applySorting(List<Item> itemList, String sortOption) {
        if (sortOption == null) return;
        switch (sortOption) {
            case "Sắp hết hạn": itemList.sort((a, b) -> { if (a.getEndTime() == null) return 1; if (b.getEndTime() == null) return -1; return a.getEndTime().compareTo(b.getEndTime()); }); break;
            case "Giá thấp → cao": itemList.sort(Comparator.comparingDouble(Item::getCurrentPrice)); break;
            case "Giá cao → thấp": itemList.sort(Comparator.comparingDouble(Item::getCurrentPrice).reversed()); break;
        }
    }

    private Node createItemCard(Item item) {
        // [Toàn bộ logic vẽ UI bên trong hàm này em COPY từ file GỐC của em sang đây để giữ UI đẹp nhé]
        // Anh thu gọn chỗ này để không bị đứt đoạn hiển thị.

        AnchorPane pane = new AnchorPane(); // Demo khung
        Button startButton = new Button("▶ Bắt đầu đấu giá");
        startButton.setOnAction(e -> startAuction(item));
        pane.getChildren().add(startButton);
        return pane;
    }

    //  FIX GỬI LỆNH
    private void startAuction(Item item) {
        if (!"PENDING".equals(getDisplayStatus(item))) {
            showInfo("Chỉ sản phẩm đang chờ mới có thể bắt đầu đấu giá.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("60");
        dialog.setTitle("Bắt đầu đấu giá");
        dialog.setHeaderText("Bắt đầu phiên: " + item.getItemName());
        dialog.setContentText("Thời gian (phút):");

        dialog.showAndWait().ifPresent(value -> {
            try {
                int duration = Integer.parseInt(value.trim());
                if (duration <= 0) throw new NumberFormatException();

                // GỌI HÀM AN TOÀN
                ClientApp.sendMessage(new Message("START_AUCTION", new Object[]{item.getId(), duration}));

                item.setStatus(AuctionStatus.ACTIVE);
                item.setEndTime(LocalDateTime.now().plusMinutes(duration));
                refreshMyItemsView();
                if (onItemsChanged != null) onItemsChanged.run();

                showInfo("Đã gửi lệnh bắt đầu đấu giá.");
            } catch (NumberFormatException ex) {
                showInfo("Thời gian phải là số nguyên > 0.");
            }
        });
    }

    // [Các helper cũ giữ nguyên...]
    private String getDisplayStatus(Item item) { if (item == null || item.getStatus() == null) return ""; String status = item.getStatus().name(); if ("ACTIVE".equals(status) && item.getEndTime() != null && !LocalDateTime.now().isBefore(item.getEndTime())) { item.setStatus(AuctionStatus.CLOSED); return "CLOSED"; } return status; }
    private String getCountdownText(Item item) { /* Rút gọn */ return "Rút gọn UI"; }
    private String getCountdownStyle(String status) { /* Rút gọn */ return ""; }
    private String getStartButtonStyle(boolean enabled) { /* Rút gọn */ return ""; }
    private Label createEmptyLabel(String text) { Label l = new Label(text); l.setPadding(new Insets(20)); return l; }
    private void showInfo(String text) { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setContentText(text); a.showAndWait(); }
    private String getStatusText(String status) { return status; }
    private String getStatusColor(String status) { return "black"; }
    private String safeText(String text) { return text == null ? "" : text; }
    private void stopAllTimelines() { for (Timeline t : runningTimelines) t.stop(); runningTimelines.clear(); }
}