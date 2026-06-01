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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.client.ClientApp;
import org.example.common.Message;
import org.example.common.model.item.AuctionStatus;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(MyItemsController.class);

    @FXML private TextField myItemsSearchTextField;
    @FXML private ComboBox<String> myItemsStatusComboBox;
    @FXML private ComboBox<String> myItemsSortComboBox;
    @FXML private FlowPane myItemsFlowPane;
    @FXML private Label myItemsSummaryLabel;

    private List<Item> items = new ArrayList<>();
    private User currentUser;
    private Runnable onItemsChanged;

    //  TỐI ƯU 1: Chỉ dùng DUY NHẤT 1 Timeline cho toàn bộ màn hình
    private Timeline masterTimeline;

    // TỐI ƯU 2: Danh sách chứa các hàm cập nhật UI của từng thẻ sản phẩm
    private final List<Runnable> uiUpdaters = new ArrayList<>();

    private static final NumberFormat currencyFormat = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.debug("Initializing MyItemsController");

        myItemsStatusComboBox.getItems().setAll("Tất cả", "Chờ", "Đang diễn ra", "Đã kết thúc", "Bị hủy");
        myItemsStatusComboBox.setValue("Tất cả");
        myItemsSortComboBox.getItems().setAll("Mặc định", "Giá thấp → cao", "Giá cao → thấp", "Sắp hết hạn");
        myItemsSortComboBox.setValue("Mặc định");

        myItemsSearchTextField.setOnKeyReleased(e -> {
            logger.debug("Search text changed: {}", myItemsSearchTextField.getText());
            refreshMyItemsView();
        });
        myItemsStatusComboBox.setOnAction(e -> {
            logger.debug("Status filter changed: {}", myItemsStatusComboBox.getValue());
            refreshMyItemsView();
        });
        myItemsSortComboBox.setOnAction(e -> {
            logger.debug("Sort option changed: {}", myItemsSortComboBox.getValue());
            refreshMyItemsView();
        });

        // Khởi tạo Master Timeline (Đập nhịp 1 giây/lần)
        masterTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            // log ở mức trace để tránh ồn nếu không cần
            logger.trace("Master timeline tick - updating {} updaters", uiUpdaters.size());
            for (Runnable updater : uiUpdaters) {
                try {
                    updater.run(); // Cập nhật hàng loạt tất cả các thẻ cùng 1 lúc
                } catch (Exception ex) {
                    logger.error("Updater threw exception", ex);
                }
            }
        }));
        masterTimeline.setCycleCount(Timeline.INDEFINITE);
        masterTimeline.play();
    }

    public void setup(List<Item> items, User currentUser, Runnable onItemsChanged) {
        logger.info("Setup MyItemsController for user id={} with {} items", currentUser == null ? "null" : currentUser.getId(), items == null ? 0 : items.size());
        this.items = items;
        this.currentUser = currentUser;
        this.onItemsChanged = onItemsChanged;
        refreshMyItemsView();
    }

    public void updateData(List<Item> items) {
        logger.debug("updateData called with {} items", items == null ? 0 : items.size());
        this.items = items;
        refreshMyItemsView();
    }

    @FXML
    private void onMyItemsRefreshClicked() {
        logger.info("Manual refresh clicked");
        if (onItemsChanged != null) {
            onItemsChanged.run();
        }
        refreshMyItemsView();
    }

    public void refreshMyItemsView() {
        logger.debug("Refreshing MyItems view");
        uiUpdaters.clear(); // Xóa sạch danh sách cập nhật cũ
        myItemsFlowPane.getChildren().clear();

        if (currentUser == null) {
            logger.warn("refreshMyItemsView called but currentUser is null");
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
        logger.debug("Filtered items count: {}", filtered.size());

        if (filtered.isEmpty()) {
            myItemsFlowPane.getChildren().add(createEmptyLabel(" Bạn chưa có sản phẩm nào.\nHãy sang mục Đăng sản phẩm mới."));
            return;
        }

        for (Item item : filtered) {
            myItemsFlowPane.getChildren().add(createItemCard(item));
        }
    }

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
            case "Sắp hết hạn":
                itemList.sort((a, b) -> {
                    if (a.getEndTime() == null) return 1;
                    if (b.getEndTime() == null) return -1;
                    return a.getEndTime().compareTo(b.getEndTime());
                });
                break;
            case "Giá thấp → cao":
                itemList.sort(Comparator.comparingDouble(Item::getCurrentPrice));
                break;
            case "Giá cao → thấp":
                itemList.sort(Comparator.comparingDouble(Item::getCurrentPrice).reversed());
                break;
        }
    }

    private Node createItemCard(Item item) {
        VBox card = new VBox(12);
        card.setPrefSize(285, 335);
        card.setMinSize(285, 335);
        card.setMaxSize(285, 335);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(16));
        card.setStyle(getMyItemCardStyle());

        String displayStatus = getDisplayStatus(item);

        Label nameLabel = new Label(item.getItemName());
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(165);
        nameLabel.setStyle(
                "-fx-text-fill: #0f172a;" +
                        "-fx-font-size: 16;" +
                        "-fx-font-weight: bold;"
        );

        Label statusBadge = createStatusBadge(displayStatus);

        HBox headerRow = new HBox(10, nameLabel, statusBadge);
        headerRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Label typeLabel = new Label("Loại: " + safeText(item.getType()));
        typeLabel.setStyle(
                "-fx-text-fill: #475569;" +
                        "-fx-font-size: 12;" +
                        "-fx-background-color: #f1f5f9;" +
                        "-fx-padding: 5 9;" +
                        "-fx-background-radius: 999;"
        );

        Label timeLabel = new Label(getCardTimeText(item));
        timeLabel.setWrapText(true);
        timeLabel.setMaxWidth(Double.MAX_VALUE);
        timeLabel.setStyle(getTimeLabelStyle(displayStatus));

        Label descLabel = new Label(item.getDescription() != null && !item.getDescription().isBlank()
                ? item.getDescription()
                : "Không có mô tả");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(Double.MAX_VALUE);
        descLabel.setMinHeight(56);
        descLabel.setPrefHeight(56);
        descLabel.setStyle(
                "-fx-text-fill: #64748b;" +
                        "-fx-font-size: 12;" +
                        "-fx-background-color: #f8fafc;" +
                        "-fx-padding: 10;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 12;"
        );

        VBox priceBox = createInfoBlock(
                "Giá hiện tại",
                currencyFormat.format(item.getCurrentPrice()) + " VNĐ",
                true
        );

        VBox bidCountBox = createInfoBlock(
                "Lượt đặt giá",
                String.valueOf(getEstimatedBidCount(item)),
                false
        );

        HBox statsRow = new HBox(10, priceBox, bidCountBox);
        HBox.setHgrow(priceBox, Priority.ALWAYS);
        HBox.setHgrow(bidCountBox, Priority.ALWAYS);

        Label winnerLabel = new Label(getWinnerText(item));
        winnerLabel.setWrapText(true);
        winnerLabel.setMaxWidth(Double.MAX_VALUE);
        winnerLabel.setStyle(
                "-fx-text-fill: #475569;" +
                        "-fx-font-size: 12;" +
                        "-fx-background-color: #eef2ff;" +
                        "-fx-padding: 8 10;" +
                        "-fx-background-radius: 12;"
        );

        Button startButton = new Button("▶ Bắt đầu đấu giá");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setPrefHeight(40);
        startButton.setDisable(!"PENDING".equals(displayStatus));
        startButton.setOpacity(1.0);
        startButton.setStyle(getStartButtonStyle("PENDING".equals(displayStatus)));
        startButton.setOnAction(e -> startAuction(item));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(
                headerRow,
                typeLabel,
                timeLabel,
                descLabel,
                statsRow,
                winnerLabel,
                spacer,
                startButton
        );

        Runnable updater = () -> {
            String newStatus = getDisplayStatus(item);
            statusBadge.setText(getStatusText(newStatus));
            statusBadge.setStyle(getStatusBadgeStyle(newStatus));
            timeLabel.setText(getCardTimeText(item));
            timeLabel.setStyle(getTimeLabelStyle(newStatus));
            priceBox.getChildren().set(1, createInfoValue(currencyFormat.format(item.getCurrentPrice()) + " VNĐ", true));
            bidCountBox.getChildren().set(1, createInfoValue(String.valueOf(getEstimatedBidCount(item)), false));
            winnerLabel.setText(getWinnerText(item));

            boolean canStart = "PENDING".equals(newStatus);
            startButton.setDisable(!canStart);
            startButton.setOpacity(1.0);
            startButton.setStyle(getStartButtonStyle(canStart));
        };
        uiUpdaters.add(updater);

        return card;
    }

    private String getMyItemCardStyle() {
        return "-fx-background-color: white;" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #e2e8f0;" +
                "-fx-border-radius: 18;" +
                "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.08), 14, 0, 0, 4);";
    }

    private Label createStatusBadge(String status) {
        Label label = new Label(getStatusText(status));
        label.setStyle(getStatusBadgeStyle(status));
        return label;
    }

    private String getStatusBadgeStyle(String status) {
        return "-fx-background-color: " + getStatusColor(status) + ";" +
                "-fx-text-fill: white;" +
                "-fx-padding: 6 11;" +
                "-fx-background-radius: 999;" +
                "-fx-font-size: 11;" +
                "-fx-font-weight: bold;";
    }

    private String getCardTimeText(Item item) {
        String status = getDisplayStatus(item);
        if ("ACTIVE".equals(status)) {
            return "⏱ " + getCountdownText(item);
        }
        if ("PENDING".equals(status)) {
            return "⏱ Chưa bắt đầu phiên đấu giá";
        }
        if ("CLOSED".equals(status)) {
            return "⏱ Phiên đấu giá đã kết thúc";
        }
        if ("CANCELED".equals(status)) {
            return "⏱ Phiên đấu giá đã bị hủy";
        }
        return "⏱ Không khả dụng";
    }

    private String getTimeLabelStyle(String status) {
        if ("ACTIVE".equals(status)) {
            return "-fx-background-color: #dcfce7;" +
                    "-fx-text-fill: #166534;" +
                    "-fx-padding: 7 10;" +
                    "-fx-background-radius: 12;" +
                    "-fx-font-size: 12;" +
                    "-fx-font-weight: bold;";
        }
        return "-fx-background-color: #f1f5f9;" +
                "-fx-text-fill: #475569;" +
                "-fx-padding: 7 10;" +
                "-fx-background-radius: 12;" +
                "-fx-font-size: 12;" +
                "-fx-font-weight: bold;";
    }

    private VBox createInfoBlock(String title, String value, boolean highlight) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");

        Label valueLabel = createInfoValue(value, highlight);

        VBox box = new VBox(4, titleLabel, valueLabel);
        box.setPadding(new Insets(10));
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle(
                "-fx-background-color: #f8fafc;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 12;"
        );
        return box;
    }

    private Label createInfoValue(String value, boolean highlight) {
        Label valueLabel = new Label(value);
        valueLabel.setWrapText(true);
        valueLabel.setStyle(
                "-fx-text-fill: " + (highlight ? "#2563eb" : "#0f172a") + ";" +
                        "-fx-font-size: " + (highlight ? "15" : "14") + ";" +
                        "-fx-font-weight: bold;"
        );
        return valueLabel;
    }

    private String getWinnerText(Item item) {
        return item.getCurrentWinnerId() == null
                ? "Chưa có người đặt giá"
                : "Người thắng hiện tại: #" + item.getCurrentWinnerId();
    }

    private int getEstimatedBidCount(Item item) {
        if (item.getBidIncrement() <= 0) {
            return 0;
        }

        double diff = item.getCurrentPrice() - item.getStartingPrice();
        if (diff <= 0) {
            return 0;
        }

        return Math.max(1, (int) Math.round(diff / item.getBidIncrement()));
    }

    private void startAuction(Item item) {
        if (!"PENDING".equals(getDisplayStatus(item))) {
            logger.info("Attempt to start auction for item {} but status is not PENDING (status={})", item.getId(), getDisplayStatus(item));
            showInfo("Chỉ sản phẩm đang chờ mới có thể bắt đầu đấu giá.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("60");
        dialog.setTitle("Bắt đầu đấu giá");
        dialog.setHeaderText("Bắt đầu phiên đấu giá cho: " + item.getItemName());
        dialog.setContentText("Thời gian đấu giá (phút):");

        dialog.showAndWait().ifPresent(value -> {
            try {
                int duration = Integer.parseInt(value.trim());
                if (duration <= 0) throw new NumberFormatException();

                logger.info("Sending START_AUCTION for itemId={} duration={}min", item.getId(), duration);
                ClientApp.sendMessage(new Message("START_AUCTION", new Object[]{item.getId(), duration}));

                item.setStatus(AuctionStatus.ACTIVE);
                item.setEndTime(LocalDateTime.now().plusMinutes(duration));
                refreshMyItemsView();
                if (onItemsChanged != null) onItemsChanged.run();

                showInfo("Đã bắt đầu đấu giá. Trạng thái sản phẩm đã chuyển sang Đang diễn ra.");
            } catch (NumberFormatException ex) {
                logger.warn("Invalid auction duration entered: {}", value, ex);
                showInfo("Thời gian đấu giá phải là số nguyên lớn hơn 0.");
            } catch (Exception ex) {
                logger.error("Error when sending START_AUCTION for item " + item.getId(), ex);
                showInfo("Lỗi khi gửi yêu cầu: " + ex.getMessage());
            }
        });
    }

    private String getDisplayStatus(Item item) {
        if (item == null || item.getStatus() == null) return "";
        String status = item.getStatus().name();
        if ("ACTIVE".equals(status) && item.getEndTime() != null && !LocalDateTime.now().isBefore(item.getEndTime())) {
            item.setStatus(AuctionStatus.CLOSED);
            logger.info("Auto-updated item {} status to CLOSED (endTime passed)", item.getId());
            return "CLOSED";
        }
        return status;
    }

    private String getCountdownText(Item item) {
        String status = getDisplayStatus(item);
        if ("PENDING".equals(status)) return "Chưa bắt đầu";
        if ("CLOSED".equals(status)) return "Đã kết thúc";
        if ("CANCELED".equals(status)) return "Đã hủy";
        if (!"ACTIVE".equals(status)) return "Không khả dụng";
        if (item.getEndTime() == null) return "Không có thời hạn";

        long secondsLeft = java.time.Duration.between(LocalDateTime.now(), item.getEndTime()).getSeconds();
        if (secondsLeft <= 0) { item.setStatus(AuctionStatus.CLOSED); return "Đã kết thúc"; }

        long days = secondsLeft / 86400; long hours = (secondsLeft % 86400) / 3600;
        long minutes = (secondsLeft % 3600) / 60; long seconds = secondsLeft % 60;
        if (days > 0) return String.format("Còn %dd %02d:%02d:%02d", days, hours, minutes, seconds);
        return String.format("Còn %02d:%02d:%02d", hours, minutes, seconds);
    }

    private String getCountdownStyle(String status) {
        if ("ACTIVE".equals(status)) {
            return "-fx-background-color: #dcfce7;" + "-fx-text-fill: #166534;" + "-fx-padding: 6 10;" +
                    "-fx-background-radius: 999;" + "-fx-font-size: 11;" + "-fx-font-weight: bold;";
        }
        return "-fx-background-color: #e2e8f0;" + "-fx-text-fill: #475569;" + "-fx-padding: 6 10;" +
                "-fx-background-radius: 999;" + "-fx-font-size: 11;" + "-fx-font-weight: bold;";
    }

    private String getStartButtonStyle(boolean enabled) {
        if (enabled) {
            return "-fx-background-color: #10b981;" + "-fx-text-fill: white;" + "-fx-font-weight: bold;" +
                    "-fx-background-radius: 10;" + "-fx-padding: 9 12;" + "-fx-cursor: hand;";
        }
        return "-fx-background-color: #e2e8f0;" + "-fx-text-fill: #64748b;" + "-fx-font-weight: bold;" +
                "-fx-background-radius: 10;" + "-fx-padding: 9 12;" + "-fx-opacity: 1;";
    }

    private Label createEmptyLabel(String text) { Label label = new Label(text); label.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14;"); label.setPadding(new Insets(20)); return label; }

    // Ghi log đồng thời hiển thị alert - giúp dev theo dõi các message gửi tới user
    private void showInfo(String text) {
        logger.info("User message: {}", text);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    private void showError(String text, Throwable t) {
        logger.error(text, t);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    private String getStatusText(String status) {
        switch (status) {
            case "PENDING": return "Chờ";
            case "ACTIVE": return "Đang diễn ra";
            case "CLOSED": return "Đã kết thúc";
            case "CANCELED": return "Bị hủy";
            default: return status == null || status.isBlank() ? "Không rõ" : status;
        }
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "PENDING": return "#94a3b8";
            case "ACTIVE": return "#10b981";
            case "CLOSED": return "#8b5cf6";
            case "CANCELED": return "#ef4444";
            default: return "#64748b";
        }
    }

    private String safeText(String text) { return text == null || text.isBlank() ? "Không có" : text; }
}
