package org.example.client.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.client.ClientApp;
import org.example.common.Message;
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

    private List<Item> allItems = new ArrayList<>();
    private User currentUser;
    private Runnable refreshAllItemsCallback;

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

    public void setup(List<Item> allItems, User currentUser, Runnable refreshAllItemsCallback) {
        this.allItems = allItems;
        this.currentUser = currentUser;
        this.refreshAllItemsCallback = refreshAllItemsCallback;
        refreshMyItemsView();
    }

    public void updateData(List<Item> allItems) {
        this.allItems = allItems;
        refreshMyItemsView();
    }

    @FXML
    private void onMyItemsRefreshClicked() {
        if (refreshAllItemsCallback != null) {
            refreshAllItemsCallback.run();
        }
    }

    public void refreshMyItemsView() {
        stopAllTimelines();
        myItemsFlowPane.getChildren().clear();

        if (currentUser == null) {
            myItemsFlowPane.getChildren().add(createEmptyLabel("Bạn chưa đăng nhập."));
            return;
        }

        String search = myItemsSearchTextField.getText() == null ? "" : myItemsSearchTextField.getText().toLowerCase();
        String statusFilter = myItemsStatusComboBox.getValue();
        String sortOption = myItemsSortComboBox.getValue();

        List<Item> myFilteredItems = allItems.stream()
                .filter(item -> String.valueOf(currentUser.getId()).equals(item.getSellerId()))
                .filter(item -> item.getItemName() != null && item.getItemName().toLowerCase().contains(search))
                .filter(item -> applyStatusFilter(item, statusFilter))
                .collect(Collectors.toList());

        applySorting(myFilteredItems, sortOption);
        myItemsSummaryLabel.setText("Tổng: " + myFilteredItems.size() + " sản phẩm");

        if (myFilteredItems.isEmpty()) {
            myItemsFlowPane.getChildren().add(createEmptyLabel("Bạn chưa có sản phẩm nào.\nHãy sang mục Đăng sản phẩm mới."));
            return;
        }

        for (Item item : myFilteredItems) {
            myItemsFlowPane.getChildren().add(createItemCard(item));
        }
    }

    private Node createItemCard(Item item) {
        VBox card = new VBox(10);
        card.setPrefSize(240, 320);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-background-radius: 12; -fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 3);");

        String displayStatus = getDisplayStatus(item);

        Label statusLabel = new Label(getStatusText(displayStatus));
        statusLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: " + getStatusColor(displayStatus) + "; -fx-padding: 4 8; -fx-background-radius: 6;");

        Label nameLabel = new Label(item.getItemName());
        nameLabel.setWrapText(true);
        nameLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label priceLabel = new Label(currencyFormat.format(item.getCurrentPrice()) + " VNĐ");
        priceLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #2563eb;");

        Label countdownLabel = new Label(getCountdownText(item));
        countdownLabel.setStyle(getCountdownStyle(displayStatus));

        Timeline timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
            String newStatus = getDisplayStatus(item);
            countdownLabel.setText(getCountdownText(item));
            countdownLabel.setStyle(getCountdownStyle(newStatus));
            statusLabel.setText(getStatusText(newStatus));
            statusLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: " + getStatusColor(newStatus) + "; -fx-padding: 4 8; -fx-background-radius: 6;");
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        runningTimelines.add(timeline);

        Button startButton = new Button("▶ Bắt đầu đấu giá");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setStyle(getStartButtonStyle("PENDING".equals(displayStatus)));
        startButton.setDisable(!"PENDING".equals(displayStatus));
        startButton.setOnAction(e -> startAuction(item));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(statusLabel, nameLabel, priceLabel, spacer, countdownLabel, startButton);
        return card;
    }

    private void startAuction(Item item) {
        if (!"PENDING".equals(getDisplayStatus(item))) {
            showInfo("Chỉ sản phẩm đang chờ mới có thể bắt đầu đấu giá.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("60");
        dialog.setTitle("Bắt đầu đấu giá");
        dialog.setHeaderText("Bắt đầu phiên cho: " + item.getItemName());
        dialog.setContentText("Nhập thời gian đấu giá (phút):");

        dialog.showAndWait().ifPresent(value -> {
            try {
                int durationMinutes = Integer.parseInt(value.trim());
                if (durationMinutes <= 0) {
                    showInfo("Thời gian phải là một số nguyên dương.");
                    return;
                }
                ClientApp.sendMessage(new Message("START_AUCTION", new Object[]{item.getId(), durationMinutes}));
                showInfo("Đã gửi yêu cầu bắt đầu phiên đấu giá.");
            } catch (NumberFormatException ex) {
                showInfo("Vui lòng nhập một số hợp lệ cho thời gian.");
            }
        });
    }

    private boolean applyStatusFilter(Item item, String filter) {
        if (filter == null || "Tất cả".equals(filter)) return true;
        String itemStatus = getDisplayStatus(item);
        switch (filter) {
            case "Chờ": return "PENDING".equals(itemStatus);
            case "Đang diễn ra": return "ACTIVE".equals(itemStatus);
            case "Đã kết thúc": return "CLOSED".equals(itemStatus);
            case "Bị hủy": return "CANCELED".equals(itemStatus);
            default: return true;
        }
    }

    private void applySorting(List<Item> itemList, String sortOption) {
        if (sortOption == null) return;
        switch (sortOption) {
            case "Giá thấp → cao": itemList.sort(Comparator.comparingDouble(Item::getCurrentPrice)); break;
            case "Giá cao → thấp": itemList.sort(Comparator.comparingDouble(Item::getCurrentPrice).reversed()); break;
            case "Sắp hết hạn":
                itemList.sort(Comparator.comparing(Item::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())));
                break;
            default: // Mặc định
                itemList.sort(Comparator.comparing(Item::getItemName, String.CASE_INSENSITIVE_ORDER));
                break;
        }
    }

    private String getDisplayStatus(Item item) {
        if (item == null || item.getStatus() == null) return "UNKNOWN";
        String status = item.getStatus().name();
        if ("ACTIVE".equals(status) && item.getEndTime() != null && LocalDateTime.now().isAfter(item.getEndTime())) {
            return "CLOSED";
        }
        return status;
    }

    private String getStatusText(String status) {
        switch (status) {
            case "PENDING": return "Chờ duyệt";
            case "ACTIVE": return "Đang diễn ra";
            case "CLOSED": return "Đã kết thúc";
            case "CANCELED": return "Bị hủy";
            default: return "Không rõ";
        }
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "PENDING": return "#fbbf24"; // amber-400
            case "ACTIVE": return "#22c55e"; // green-500
            case "CLOSED": return "#8b5cf6; "; // violet-500
            case "CANCELED": return "#ef4444"; // red-500
            default: return "#64748b"; // slate-500
        }
    }

    private String getCountdownText(Item item) {
        String status = getDisplayStatus(item);
        if (!"ACTIVE".equals(status)) {
            return "Kết thúc: " + (item.getEndTime() != null ? item.getEndTime().toString() : "N/A");
        }
        if (item.getEndTime() == null) return "Không có thời hạn";
        long secondsLeft = java.time.Duration.between(LocalDateTime.now(), item.getEndTime()).toSeconds();
        if (secondsLeft <= 0) return "Đã kết thúc";
        long days = secondsLeft / 86400;
        long hours = (secondsLeft % 86400) / 3600;
        long minutes = (secondsLeft % 3600) / 60;
        long seconds = secondsLeft % 60;
        if (days > 0) return String.format("Còn %d ngày %02d:%02d", days, hours, minutes);
        return String.format("Còn %02d:%02d:%02d", hours, minutes, seconds);
    }

    private String getCountdownStyle(String status) {
        return "-fx-font-size: 12; -fx-text-fill: #475569;";
    }

    private String getStartButtonStyle(boolean enabled) {
        if (enabled) {
            return "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;";
        }
        return "-fx-background-color: #d1d5db; -fx-text-fill: #6b7280; -fx-font-weight: bold; -fx-background-radius: 8;";
    }

    private Label createEmptyLabel(String text) {
        Label label = new Label(text);
        label.setPadding(new Insets(40));
        label.setAlignment(Pos.CENTER);
        label.setStyle("-fx-font-size: 14; -fx-text-fill: #64748b;");
        return label;
    }

    private void showInfo(String text) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText(text);
            alert.showAndWait();
        });
    }

    private void stopAllTimelines() {
        runningTimelines.forEach(Timeline::stop);
        runningTimelines.clear();
    }
}
