package org.example.client.controllers.home;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.client.ClientApp;
import org.example.common.model.item.Item;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class HomeListController implements Initializable {
    private static final Logger logger = Logger.getLogger(HomeListController.class.getName());

    @FXML private TextField searchTextField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private Button refreshButton;
    @FXML private FlowPane itemFlowPane;

    private List<Item> items = new ArrayList<>();
    private Runnable onRefresh;
    private Consumer<Item> onOpenAuctionRoom;

    private static final NumberFormat currencyFormat = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter END_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHomeViewFilters();
    }

    public void setup(List<Item> items, Runnable onRefresh, Consumer<Item> onOpenAuctionRoom) {
        this.items = items == null ? new ArrayList<>() : items;
        this.onRefresh = onRefresh;
        this.onOpenAuctionRoom = onOpenAuctionRoom;
        applyHomeFiltersAndSort();
    }

    public void updateData(List<Item> items) {
        this.items = items == null ? new ArrayList<>() : items;
        applyHomeFiltersAndSort();
    }

    public void refreshHomeView() {
        applyHomeFiltersAndSort();
    }

    private void setupHomeViewFilters() {
        ObservableList<String> statuses = FXCollections.observableArrayList(
                "Tất cả",
                "Chờ",
                "Đang diễn ra",
                "Đã kết thúc",
                "Bị hủy"
        );

        filterComboBox.setItems(statuses);
        filterComboBox.setValue("Tất cả");

        ObservableList<String> sorts = FXCollections.observableArrayList(
                "Mặc định",
                "Giá thấp → cao",
                "Giá cao → thấp",
                "Sắp hết hạn"
        );

        sortComboBox.setItems(sorts);
        sortComboBox.setValue("Mặc định");

        filterComboBox.setOnAction(e -> applyHomeFiltersAndSort());
        sortComboBox.setOnAction(e -> applyHomeFiltersAndSort());
        searchTextField.setOnKeyReleased(e -> applyHomeFiltersAndSort());
    }

    @FXML
    public void onRefreshClicked() {
        if (onRefresh != null) {
            onRefresh.run();
        }
    }

    private void applyHomeFiltersAndSort() {
        if (itemFlowPane == null) {
            return;
        }

        String searchText = searchTextField.getText() == null
                ? ""
                : searchTextField.getText().toLowerCase();

        String statusFilter = filterComboBox.getValue();
        String sortOption = sortComboBox.getValue();

        List<Item> filtered = items.stream()
                .filter(item -> item.getItemName() != null
                        && item.getItemName().toLowerCase().contains(searchText))
                .filter(item -> applyStatusFilter(item, statusFilter))
                .collect(Collectors.toList());

        applySorting(filtered, sortOption);
        displayItems(filtered);
    }

    private boolean applyStatusFilter(Item item, String filter) {
        if (filter == null || "Tất cả".equals(filter)) {
            return true;
        }

        String status = item.getStatus() == null ? "" : item.getStatus().name();

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
            case "Giá thấp → cao":
                itemList.sort(Comparator.comparingDouble(Item::getCurrentPrice));
                break;

            case "Giá cao → thấp":
                itemList.sort(Comparator.comparingDouble(Item::getCurrentPrice).reversed());
                break;

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

            default:
                break;
        }
    }

    private void displayItems(List<Item> itemsToDisplay) {
        itemFlowPane.getChildren().clear();

        if (itemsToDisplay.isEmpty()) {
            Label emptyLabel = new Label("Chưa có sản phẩm phù hợp.");
            emptyLabel.setStyle(
                    "-fx-text-fill: #64748b;" +
                            "-fx-font-size: 15;" +
                            "-fx-padding: 30;"
            );
            itemFlowPane.getChildren().add(emptyLabel);
            return;
        }

        for (Item item : itemsToDisplay) {
            itemFlowPane.getChildren().add(createItemCard(item));
        }
    }

    private Node createItemCard(Item item) {
        VBox card = new VBox(12);
        card.setPrefSize(250, 160);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(18));
        card.setStyle(getNormalCardStyle());

        String displayStatus = getDisplayStatus(item);

        Label statusBadge = new Label(getStatusText(displayStatus));
        statusBadge.setStyle(
                "-fx-background-color: " + getStatusColor(displayStatus) + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 4 10;" +
                        "-fx-background-radius: 999;" +
                        "-fx-font-size: 11;" +
                        "-fx-font-weight: bold;"
        );

        Label nameLabel = new Label(item.getItemName());
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(215);
        nameLabel.setStyle(
                "-fx-text-fill: #0f172a;" +
                        "-fx-font-size: 16;" +
                        "-fx-font-weight: bold;"
        );

        Label priceTitle = new Label("Giá cao nhất hiện tại");
        priceTitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");

        Label priceLabel = new Label(currencyFormat.format(item.getCurrentPrice()) + " VNĐ");
        priceLabel.setStyle(
                "-fx-text-fill: #2563eb;" +
                        "-fx-font-size: 18;" +
                        "-fx-font-weight: bold;"
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label hintLabel = new Label("Bấm để xem chi tiết");
        hintLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11;");

        card.getChildren().addAll(
                statusBadge,
                nameLabel,
                priceTitle,
                priceLabel,
                spacer,
                hintLabel
        );

        card.setOnMouseClicked(event -> showItemDetailDialog(item));
        card.setOnMouseEntered(event -> card.setStyle(getHoverCardStyle()));
        card.setOnMouseExited(event -> card.setStyle(getNormalCardStyle()));

        return card;
    }

    private String getNormalCardStyle() {
        return "-fx-background-color: white;" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #e2e8f0;" +
                "-fx-border-radius: 18;" +
                "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.10), 12, 0, 0, 4);" +
                "-fx-cursor: hand;";
    }

    private String getHoverCardStyle() {
        return "-fx-background-color: white;" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #3b82f6;" +
                "-fx-border-radius: 18;" +
                "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.22), 18, 0, 0, 6);" +
                "-fx-cursor: hand;";
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
                return "#6b7280";
        }
    }

    private void showItemDetailDialog(Item item) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết sản phẩm");
        dialog.setHeaderText(null);

        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.setPrefWidth(660);
        root.setStyle("-fx-background-color: #f8fafc;");

        HBox mainContent = new HBox(22);
        mainContent.setAlignment(Pos.TOP_LEFT);

        VBox imageBox = new VBox();
        imageBox.setAlignment(Pos.CENTER);
        imageBox.setPrefSize(230, 230);
        imageBox.setMinSize(230, 230);
        imageBox.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 18;"
        );

        if (item.getImagePath() != null && !item.getImagePath().isBlank()) {
            Task<byte[]> loadImageTask = new Task<>() {
                @Override
                protected byte[] call() throws Exception {
                    return ClientApp.getImageBytes(item.getImagePath());
                }
            };
            loadImageTask.setOnSucceeded(e -> {
                byte[] imageBytes = loadImageTask.getValue();
                if (imageBytes != null && imageBytes.length > 0) {
                    ImageView imageView = new ImageView(new Image(new ByteArrayInputStream(imageBytes)));
                    imageView.setFitWidth(210);
                    imageView.setFitHeight(210);
                    imageView.setPreserveRatio(true);
                    imageView.setSmooth(true);
                    imageBox.getChildren().add(imageView);
                } else {
                    imageBox.getChildren().add(createImagePlaceholder("Không tải được ảnh"));
                }
            });
            loadImageTask.setOnFailed(e -> {
                logger.log(Level.WARNING, "Không tải được ảnh sản phẩm (popup chi tiết): {0}", item.getImagePath());
                imageBox.getChildren().add(createImagePlaceholder("Lỗi tải ảnh"));
            });
            ClientApp.executorService.submit(loadImageTask);
        } else {
            imageBox.getChildren().add(createImagePlaceholder("Chưa có ảnh\nsản phẩm"));
        }

        VBox infoBox = new VBox(12);
        infoBox.setPrefWidth(380);

        Label nameLabel = new Label(item.getItemName());
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(230);
        nameLabel.setStyle(
                "-fx-text-fill: #0f172a;" +
                        "-fx-font-size: 24;" +
                        "-fx-font-weight: bold;"
        );

        String status = getDisplayStatus(item);

        Label countdownLabel = new Label(getCountdownText(item));
        countdownLabel.setStyle(getCountdownStyle(status));

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.TOP_LEFT);

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        titleRow.getChildren().addAll(nameLabel, titleSpacer, countdownLabel);

        Label statusLabel = new Label(getStatusText(status));
        statusLabel.setStyle(
                "-fx-background-color: " + getStatusColor(status) + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 5 12;" +
                        "-fx-background-radius: 999;" +
                        "-fx-font-size: 12;" +
                        "-fx-font-weight: bold;"
        );

        Label typeLabel = createDetailLine("Loại sản phẩm", safeText(item.getType()));
        Label startingPriceLabel = createDetailLine(
                "Giá khởi điểm",
                currencyFormat.format(item.getStartingPrice()) + " VNĐ"
        );
        Label currentPriceLabel = createDetailLine(
                "Giá cao nhất hiện tại",
                currencyFormat.format(item.getCurrentPrice()) + " VNĐ"
        );
        currentPriceLabel.setStyle(
                "-fx-text-fill: #2563eb;" +
                        "-fx-font-size: 15;" +
                        "-fx-font-weight: bold;"
        );

        Label incrementLabel = createDetailLine(
                "Bước giá tối thiểu",
                currencyFormat.format(item.getBidIncrement()) + " VNĐ"
        );

        Label endTimeLabel = createDetailLine(
                "Thời gian kết thúc",
                item.getEndTime() == null ? "Chưa thiết lập" : item.getEndTime().format(END_TIME_FORMATTER)
        );

        Label descriptionTitle = new Label("Mô tả sản phẩm");
        descriptionTitle.setStyle(
                "-fx-text-fill: #334155;" +
                        "-fx-font-size: 13;" +
                        "-fx-font-weight: bold;"
        );

        Label descriptionLabel = new Label(
                item.getDescription() == null || item.getDescription().isBlank()
                        ? "Không có mô tả."
                        : item.getDescription()
        );
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(380);
        descriptionLabel.setStyle(
                "-fx-text-fill: #64748b;" +
                        "-fx-font-size: 13;" +
                        "-fx-background-color: white;" +
                        "-fx-padding: 12;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 12;"
        );

        Button bidButton = new Button("Tham gia phòng đấu giá");
        bidButton.setPrefHeight(42);
        bidButton.setMaxWidth(Double.MAX_VALUE);
        bidButton.setStyle(
                "-fx-background-color: #10b981;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;"
        );

        bidButton.setOnAction(event -> {
            dialog.close();
            Platform.runLater(() -> {
                if (onOpenAuctionRoom != null) {
                    onOpenAuctionRoom.accept(item);
                }
            });
        });

        if (!"ACTIVE".equals(status)) {
            bidButton.setDisable(true);
            bidButton.setText("Chỉ đặt giá khi phiên đang diễn ra");
            bidButton.setStyle(
                    "-fx-background-color: #cbd5e1;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 13;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 10;"
            );
        }

        Timeline countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    String newStatus = getDisplayStatus(item);
                    countdownLabel.setText(getCountdownText(item));
                    countdownLabel.setStyle(getCountdownStyle(newStatus));
                    statusLabel.setText(getStatusText(newStatus));
                    statusLabel.setStyle(
                            "-fx-background-color: " + getStatusColor(newStatus) + ";" +
                                    "-fx-text-fill: white;" +
                                    "-fx-padding: 5 12;" +
                                    "-fx-background-radius: 999;" +
                                    "-fx-font-size: 12;" +
                                    "-fx-font-weight: bold;"
                    );

                    if (!"ACTIVE".equals(newStatus)) {
                        bidButton.setDisable(true);
                        bidButton.setText("Chỉ đặt giá khi phiên đang diễn ra");
                        bidButton.setStyle(
                                "-fx-background-color: #cbd5e1;" +
                                        "-fx-text-fill: white;" +
                                        "-fx-font-size: 13;" +
                                        "-fx-font-weight: bold;" +
                                        "-fx-background-radius: 10;"
                        );
                    }
                })
        );
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
        dialog.setOnHidden(event -> countdownTimeline.stop());

        infoBox.getChildren().addAll(
                titleRow,
                statusLabel,
                typeLabel,
                startingPriceLabel,
                currentPriceLabel,
                incrementLabel,
                endTimeLabel,
                descriptionTitle,
                descriptionLabel,
                bidButton
        );

        mainContent.getChildren().addAll(imageBox, infoBox);
        root.getChildren().add(mainContent);

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private String getDisplayStatus(Item item) {
        if (item == null || item.getStatus() == null) {
            return "";
        }

        String status = item.getStatus().name();

        if ("ACTIVE".equals(status) && item.getEndTime() != null && !LocalDateTime.now().isBefore(item.getEndTime())) {
            return "CLOSED";
        }

        return status;
    }

    private String getCountdownText(Item item) {
        String status = getDisplayStatus(item);

        if (!"ACTIVE".equals(status)) {
            if ("CLOSED".equals(status)) {
                return "Đã kết thúc";
            }

            if ("PENDING".equals(status)) {
                return "Chưa bắt đầu";
            }

            if ("CANCELED".equals(status)) {
                return "Đã hủy";
            }

            return "Không khả dụng";
        }

        if (item.getEndTime() == null) {
            return "Không có thời hạn";
        }

        long secondsLeft = java.time.Duration.between(LocalDateTime.now(), item.getEndTime()).getSeconds();

        if (secondsLeft <= 0) {
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
                    "-fx-padding: 7 12;" +
                    "-fx-background-radius: 999;" +
                    "-fx-font-size: 12;" +
                    "-fx-font-weight: bold;";
        }

        return "-fx-background-color: #e2e8f0;" +
                "-fx-text-fill: #475569;" +
                "-fx-padding: 7 12;" +
                "-fx-background-radius: 999;" +
                "-fx-font-size: 12;" +
                "-fx-font-weight: bold;";
    }

    private Label createImagePlaceholder(String text) {
        Label imagePlaceholder = new Label(text);
        imagePlaceholder.setAlignment(Pos.CENTER);
        imagePlaceholder.setStyle(
                "-fx-text-fill: #94a3b8;" +
                        "-fx-font-size: 14;" +
                        "-fx-font-weight: bold;"
        );
        return imagePlaceholder;
    }

    private Label createDetailLine(String title, String value) {
        Label label = new Label(title + ": " + value);
        label.setWrapText(true);
        label.setStyle(
                "-fx-text-fill: #334155;" +
                        "-fx-font-size: 13;"
        );
        return label;
    }

    private String safeText(String text) {
        return text == null || text.isBlank() ? "Không có" : text;
    }
}
