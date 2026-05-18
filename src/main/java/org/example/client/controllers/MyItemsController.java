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
import org.example.common.Message;
import org.example.common.model.item.AuctionStatus;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;

import java.io.ObjectOutputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller cho màn hình "Sản phẩm của tôi".
 *
 * Bản sửa:
 * - Bấm "Bắt đầu đấu giá" sẽ cập nhật ngay trạng thái trên giao diện người bán.
 * - Card sản phẩm có thời gian đếm ngược.
 * - Khi hết giờ, card tự đổi sang "Đã kết thúc" và nút bắt đầu bị mờ.
 */
public class MyItemsController implements Initializable {

    @FXML private TextField myItemsSearchTextField;
    @FXML private ComboBox<String> myItemsStatusComboBox;
    @FXML private ComboBox<String> myItemsSortComboBox;
    @FXML private FlowPane myItemsFlowPane;
    @FXML private Label myItemsSummaryLabel;

    private List<Item> items = new ArrayList<>();
    private ObjectOutputStream out;
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

    public void setup(List<Item> items, ObjectOutputStream out, User currentUser, Runnable onItemsChanged) {
        this.items = items;
        this.out = out;
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
        if (onItemsChanged != null) {
            onItemsChanged.run();
        }

        refreshMyItemsView();
    }

    public void refreshMyItemsView() {
        stopAllTimelines();

        myItemsFlowPane.getChildren().clear();

        if (currentUser == null) {
            myItemsFlowPane.getChildren().add(createEmptyLabel("Bạn chưa đăng nhập."));
            return;
        }

        String search = myItemsSearchTextField.getText() == null
                ? ""
                : myItemsSearchTextField.getText().toLowerCase();

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
            myItemsFlowPane.getChildren().add(createEmptyLabel("📦 Bạn chưa có sản phẩm nào.\nHãy sang mục Đăng sản phẩm mới."));
            return;
        }

        for (Item item : filtered) {
            myItemsFlowPane.getChildren().add(createItemCard(item));
        }
    }

    private boolean applyStatusFilter(Item item, String filter) {
        if (filter == null || "Tất cả".equals(filter)) {
            return true;
        }

        String status = getDisplayStatus(item);

        if ("Chờ".equals(filter)) {
            return "PENDING".equals(status);
        }

        if ("Đang diễn ra".equals(filter)) {
            return "ACTIVE".equals(status);
        }

        if ("Đã kết thúc".equals(filter)) {
            return "CLOSED".equals(status);
        }

        if ("Bị hủy".equals(filter)) {
            return "CANCELED".equals(status);
        }

        return true;
    }

    private void applySorting(List<Item> itemList, String sortOption) {
        if (sortOption == null) {
            return;
        }

        switch (sortOption) {
            case "Sắp hết hạn":
                itemList.sort((a, b) -> {
                    if (a.getEndTime() == null) {
                        return 1;
                    }

                    if (b.getEndTime() == null) {
                        return -1;
                    }

                    return a.getEndTime().compareTo(b.getEndTime());
                });
                break;

            case "Giá thấp → cao":
                itemList.sort(Comparator.comparingDouble(Item::getCurrentPrice));
                break;

            case "Giá cao → thấp":
                itemList.sort(Comparator.comparingDouble(Item::getCurrentPrice).reversed());
                break;

            default:
                break;
        }
    }

    private Node createItemCard(Item item) {
        AnchorPane pane = new AnchorPane();
        pane.setPrefSize(270, 335);
        pane.setMinSize(270, 335);
        pane.setMaxSize(270, 335);
        pane.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 16;" +
                        "-fx-padding: 15;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.08), 12, 0, 0, 4);"
        );

        String displayStatus = getDisplayStatus(item);

        Label statusBadge = new Label(getStatusText(displayStatus));
        statusBadge.setStyle(
                "-fx-background-color: " + getStatusColor(displayStatus) + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 5 10;" +
                        "-fx-background-radius: 999;" +
                        "-fx-font-size: 10;" +
                        "-fx-font-weight: bold;"
        );
        AnchorPane.setTopAnchor(statusBadge, 12.0);
        AnchorPane.setRightAnchor(statusBadge, 12.0);

        Label countdownLabel = new Label(getCountdownText(item));
        countdownLabel.setAlignment(Pos.CENTER);
        countdownLabel.setStyle(getCountdownStyle(displayStatus));
        AnchorPane.setTopAnchor(countdownLabel, 48.0);
        AnchorPane.setRightAnchor(countdownLabel, 12.0);

        Label nameLabel = new Label(item.getItemName());
        nameLabel.setFont(new Font("System Bold", 15));
        nameLabel.setWrapText(true);
        nameLabel.setStyle("-fx-text-fill: #0f172a;");
        AnchorPane.setTopAnchor(nameLabel, 84.0);
        AnchorPane.setLeftAnchor(nameLabel, 12.0);
        AnchorPane.setRightAnchor(nameLabel, 12.0);

        Label typeLabel = new Label("Loại: " + safeText(item.getType()));
        typeLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");
        AnchorPane.setTopAnchor(typeLabel, 132.0);
        AnchorPane.setLeftAnchor(typeLabel, 12.0);

        Label descLabel = new Label(item.getDescription() != null && !item.getDescription().isEmpty()
                ? item.getDescription()
                : "Không có mô tả");
        descLabel.setWrapText(true);
        descLabel.setPrefHeight(45);
        descLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10;");
        AnchorPane.setTopAnchor(descLabel, 154.0);
        AnchorPane.setLeftAnchor(descLabel, 12.0);
        AnchorPane.setRightAnchor(descLabel, 12.0);

        Label priceLabel = new Label("Giá hiện tại");
        priceLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");
        AnchorPane.setTopAnchor(priceLabel, 208.0);
        AnchorPane.setLeftAnchor(priceLabel, 12.0);

        Label priceValueLabel = new Label(currencyFormat.format(item.getCurrentPrice()) + " VNĐ");
        priceValueLabel.setFont(new Font("System Bold", 15));
        priceValueLabel.setStyle("-fx-text-fill: #2563eb;");
        AnchorPane.setTopAnchor(priceValueLabel, 228.0);
        AnchorPane.setLeftAnchor(priceValueLabel, 12.0);

        Label winnerLabel = new Label(item.getCurrentWinnerId() == null
                ? "Chưa có người đặt giá"
                : "Người thắng hiện tại: #" + item.getCurrentWinnerId());
        winnerLabel.setWrapText(true);
        winnerLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");
        AnchorPane.setTopAnchor(winnerLabel, 256.0);
        AnchorPane.setLeftAnchor(winnerLabel, 12.0);
        AnchorPane.setRightAnchor(winnerLabel, 12.0);

        Button startButton = new Button("▶ Bắt đầu đấu giá");
        startButton.setDisable(!"PENDING".equals(displayStatus));
        startButton.setStyle(getStartButtonStyle("PENDING".equals(displayStatus)));
        startButton.setOnAction(e -> startAuction(item));

        AnchorPane.setBottomAnchor(startButton, 12.0);
        AnchorPane.setLeftAnchor(startButton, 12.0);
        AnchorPane.setRightAnchor(startButton, 12.0);

        pane.getChildren().addAll(
                statusBadge,
                countdownLabel,
                nameLabel,
                typeLabel,
                descLabel,
                priceLabel,
                priceValueLabel,
                winnerLabel,
                startButton
        );

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    String newStatus = getDisplayStatus(item);

                    statusBadge.setText(getStatusText(newStatus));
                    statusBadge.setStyle(
                            "-fx-background-color: " + getStatusColor(newStatus) + ";" +
                                    "-fx-text-fill: white;" +
                                    "-fx-padding: 5 10;" +
                                    "-fx-background-radius: 999;" +
                                    "-fx-font-size: 10;" +
                                    "-fx-font-weight: bold;"
                    );

                    countdownLabel.setText(getCountdownText(item));
                    countdownLabel.setStyle(getCountdownStyle(newStatus));

                    boolean canStart = "PENDING".equals(newStatus);
                    startButton.setDisable(!canStart);
                    startButton.setStyle(getStartButtonStyle(canStart));
                })
        );

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        runningTimelines.add(timeline);

        return pane;
    }

    private void startAuction(Item item) {
        if (out == null) {
            showInfo("Chưa có kết nối tới server.");
            return;
        }

        if (!"PENDING".equals(getDisplayStatus(item))) {
            showInfo("Chỉ sản phẩm đang ở trạng thái Chờ mới có thể bắt đầu đấu giá.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("60");
        dialog.setTitle("Bắt đầu đấu giá");
        dialog.setHeaderText("Bắt đầu phiên đấu giá cho: " + item.getItemName());
        dialog.setContentText("Thời gian đấu giá (phút):");

        dialog.showAndWait().ifPresent(value -> {
            try {
                int duration = Integer.parseInt(value.trim());

                if (duration <= 0) {
                    throw new NumberFormatException();
                }

                synchronized (out) {
                    out.writeObject(new Message("START_AUCTION", new Object[]{item.getId(), duration}));
                    out.flush();
                }

                /*
                 * Cập nhật lạc quan ngay phía client để người bán thấy trạng thái mới tức thì.
                 * Server vẫn sẽ phản hồi và gửi ITEM_UPDATE sau đó để đồng bộ dữ liệu thật.
                 */
                item.setStatus(AuctionStatus.ACTIVE);
                item.setEndTime(LocalDateTime.now().plusMinutes(duration));

                refreshMyItemsView();

                if (onItemsChanged != null) {
                    onItemsChanged.run();
                }

                showInfo("Đã bắt đầu đấu giá. Trạng thái sản phẩm đã chuyển sang Đang diễn ra.");

            } catch (NumberFormatException ex) {
                showInfo("Thời gian đấu giá phải là số nguyên lớn hơn 0.");
            } catch (Exception ex) {
                showInfo("Lỗi khi gửi yêu cầu: " + ex.getMessage());
            }
        });
    }

    private String getDisplayStatus(Item item) {
        if (item == null || item.getStatus() == null) {
            return "";
        }

        String status = item.getStatus().name();

        if ("ACTIVE".equals(status)
                && item.getEndTime() != null
                && !LocalDateTime.now().isBefore(item.getEndTime())) {
            item.setStatus(AuctionStatus.CLOSED);
            return "CLOSED";
        }

        return status;
    }

    private String getCountdownText(Item item) {
        String status = getDisplayStatus(item);

        if ("PENDING".equals(status)) {
            return "Chưa bắt đầu";
        }

        if ("CLOSED".equals(status)) {
            return "Đã kết thúc";
        }

        if ("CANCELED".equals(status)) {
            return "Đã hủy";
        }

        if (!"ACTIVE".equals(status)) {
            return "Không khả dụng";
        }

        if (item.getEndTime() == null) {
            return "Không có thời hạn";
        }

        long secondsLeft = java.time.Duration.between(LocalDateTime.now(), item.getEndTime()).getSeconds();

        if (secondsLeft <= 0) {
            item.setStatus(AuctionStatus.CLOSED);
            return "Đã kết thúc";
        }

        long days = secondsLeft / 86400;
        long hours = (secondsLeft % 86400) / 3600;
        long minutes = (secondsLeft % 3600) / 60;
        long seconds = secondsLeft % 60;

        if (days > 0) {
            return String.format("Còn %dd %02d:%02d:%02d", days, hours, minutes, seconds);
        }

        return String.format("Còn %02d:%02d:%02d", hours, minutes, seconds);
    }

    private String getCountdownStyle(String status) {
        if ("ACTIVE".equals(status)) {
            return "-fx-background-color: #dcfce7;" +
                    "-fx-text-fill: #166534;" +
                    "-fx-padding: 6 10;" +
                    "-fx-background-radius: 999;" +
                    "-fx-font-size: 11;" +
                    "-fx-font-weight: bold;";
        }

        return "-fx-background-color: #e2e8f0;" +
                "-fx-text-fill: #475569;" +
                "-fx-padding: 6 10;" +
                "-fx-background-radius: 999;" +
                "-fx-font-size: 11;" +
                "-fx-font-weight: bold;";
    }

    private String getStartButtonStyle(boolean enabled) {
        if (enabled) {
            return "-fx-background-color: #10b981;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 9 12;" +
                    "-fx-cursor: hand;";
        }

        return "-fx-background-color: #cbd5e1;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 9 12;";
    }

    private Label createEmptyLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14;");
        label.setPadding(new Insets(20));
        return label;
    }

    private void showInfo(String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    private String getStatusText(String status) {
        switch (status) {
            case "PENDING":
                return "Chờ";

            case "ACTIVE":
                return "Đang diễn ra";

            case "CLOSED":
                return "Đã kết thúc";

            case "CANCELED":
                return "Bị hủy";

            default:
                return status == null || status.isBlank() ? "Không rõ" : status;
        }
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "PENDING":
                return "#94a3b8";

            case "ACTIVE":
                return "#10b981";

            case "CLOSED":
                return "#8b5cf6";

            case "CANCELED":
                return "#ef4444";

            default:
                return "#64748b";
        }
    }

    private String safeText(String text) {
        return text == null || text.isBlank() ? "Không có" : text;
    }

    private void stopAllTimelines() {
        for (Timeline timeline : runningTimelines) {
            timeline.stop();
        }

        runningTimelines.clear();
    }
}
