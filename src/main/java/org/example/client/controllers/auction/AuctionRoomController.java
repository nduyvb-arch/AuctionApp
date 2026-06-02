package org.example.client.controllers.auction;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.client.ClientApp;
import org.example.common.model.chat.AuctionChatMessage;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AuctionRoomController {
    private static final Logger logger = Logger.getLogger(AuctionRoomController.class.getName());

    @FXML private VBox auctionRoomRoot;

    private List<Item> items = new ArrayList<>();
    private User currentUser;
    private Runnable onBackToHome;
    private BiConsumer<String, Double> onSubmitBid;
    private Consumer<String> onRequestItemBidHistory;
    private Consumer<String> onRequestAuctionChatHistory;
    private BiConsumer<String, String> onSendAuctionChatMessage;

    private String pendingBidItemId;
    private double pendingBidAmount;
    private String activeBidDialogItemId;
    private String activeBidDialogLastKnownWinnerId;
    private Item activeBidDialogItem;
    private Label activeBidCurrentPriceLabel;
    private Label activeBidMinBidLabel;
    private Label activeBidStatusLabel;
    private TextField activeBidAmountField;
    private Button activeBidSubmitButton;
    private Label activeBidErrorLabel;
    private XYChart.Series<String, Number> activeBidTrendSeries;
    private final List<BidHistoryController.BidHistoryRecord> activeItemBidHistory = new ArrayList<>();

    private Item activeAuctionRoomItem;
    private VBox auctionRoomChatMessagesBox;
    private ScrollPane auctionRoomChatScrollPane;
    private TextField auctionRoomChatField;
    private Button auctionRoomChatSendButton;
    private Label auctionRoomCountdownLabel;
    private Label auctionRoomStatusBadge;
    private boolean auctionRoomChatHasMessages;
    private Timeline auctionRoomCountdownTimeline;

    private static final NumberFormat currencyFormat = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter END_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter CHART_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void setup(
            User currentUser,
            List<Item> items,
            Runnable onBackToHome,
            BiConsumer<String, Double> onSubmitBid,
            Consumer<String> onRequestItemBidHistory,
            Consumer<String> onRequestAuctionChatHistory,
            BiConsumer<String, String> onSendAuctionChatMessage
    ) {
        this.currentUser = currentUser;
        this.items = items == null ? new ArrayList<>() : items;
        this.onBackToHome = onBackToHome;
        this.onSubmitBid = onSubmitBid;
        this.onRequestItemBidHistory = onRequestItemBidHistory;
        this.onRequestAuctionChatHistory = onRequestAuctionChatHistory;
        this.onSendAuctionChatMessage = onSendAuctionChatMessage;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public void updateItemsReference(List<Item> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public Item getActiveItem() {
        return activeBidDialogItem;
    }

    public boolean isActive() {
        return auctionRoomRoot != null && auctionRoomRoot.isVisible();
    }

    public void openAuctionRoom(Item item) {
        if (item == null) {
            return;
        }

        String targetItemId = item.getId();
        Item latestItem = items.stream()
                .filter(existingItem -> existingItem.getId().equals(targetItemId))
                .findFirst()
                .orElse(item);
        item = latestItem;

        clearAuctionRoomState();

        activeAuctionRoomItem = item;
        activeBidDialogItemId = item.getId();
        activeBidDialogItem = item;
        activeBidDialogLastKnownWinnerId = item.getCurrentWinnerId();

        auctionRoomRoot.getChildren().setAll(createAuctionRoomLayout(item));

        refreshActiveBidDialogLabels();
        refreshAuctionRoomHeader();
        validateActiveBidAmount();
        requestItemBidHistory(item.getId());
        requestAuctionChatHistory(item.getId());
        startAuctionRoomCountdown();

        Platform.runLater(() -> {
            if (activeBidAmountField != null) {
                activeBidAmountField.requestFocus();
            }
        });
    }

    private VBox createAuctionRoomLayout(Item item) {
        VBox root = new VBox(18);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f8fafc;");
        root.setMaxWidth(Double.MAX_VALUE);
        root.setMaxHeight(Double.MAX_VALUE);

        HBox header = createAuctionRoomHeader(item);
        HBox mainContent = new HBox(18);
        mainContent.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        VBox productPanel = createAuctionRoomProductPanel(item);
        VBox bidPanel = createAuctionRoomBidPanel(item);
        VBox chatPanel = createAuctionRoomChatPanel();

        HBox.setHgrow(bidPanel, Priority.ALWAYS);
        mainContent.getChildren().addAll(productPanel, bidPanel, chatPanel);

        root.getChildren().addAll(header, mainContent);
        return root;
    }

    private HBox createAuctionRoomHeader(Item item) {
        Button backButton = new Button("← Quay lại danh sách");
        backButton.setPrefHeight(38);
        backButton.setStyle(
                "-fx-background-color: #e2e8f0;" +
                        "-fx-text-fill: #334155;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;"
        );
        backButton.setOnAction(event -> {
            if (onBackToHome != null) {
                onBackToHome.run();
            }
        });

        Label titleLabel = new Label(item.getItemName());
        titleLabel.setWrapText(true);
        titleLabel.setStyle(
                "-fx-text-fill: #0f172a;" +
                        "-fx-font-size: 23;" +
                        "-fx-font-weight: bold;"
        );

        Label subtitleLabel = new Label("Phòng đấu giá realtime · đặt giá, xem biểu đồ và chat cùng bidder khác");
        subtitleLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12;");

        VBox titleBox = new VBox(4, titleLabel, subtitleLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        auctionRoomStatusBadge = new Label();
        auctionRoomStatusBadge.setStyle("-fx-text-fill: white; -fx-padding: 7 13; -fx-background-radius: 999; -fx-font-size: 12; -fx-font-weight: bold;");

        auctionRoomCountdownLabel = new Label();
        auctionRoomCountdownLabel.setStyle(getCountdownStyle(getDisplayStatus(item)));

        HBox header = new HBox(14, backButton, titleBox, auctionRoomStatusBadge, auctionRoomCountdownLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16));
        header.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 18;"
        );
        return header;
    }

    private VBox createAuctionRoomProductPanel(Item item) {
        VBox panel = new VBox(14);
        panel.setPrefWidth(270);
        panel.setMinWidth(250);
        panel.setPadding(new Insets(16));
        panel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 18;"
        );

        VBox imageBox = new VBox();
        imageBox.setAlignment(Pos.CENTER);
        imageBox.setPrefSize(238, 210);
        imageBox.setMinSize(238, 210);
        imageBox.setStyle(
                "-fx-background-color: #f8fafc;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 16;"
        );
        loadItemImageIntoBox(item, imageBox, 220, 190);

        Label infoTitle = new Label("Thông tin sản phẩm");
        infoTitle.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 15; -fx-font-weight: bold;");

        Label typeLabel = createDetailLine("Loại", safeText(item.getType()));
        Label startPriceLabel = createDetailLine("Giá khởi điểm", currencyFormat.format(item.getStartingPrice()) + " VNĐ");
        Label incrementLabel = createDetailLine("Bước giá", currencyFormat.format(item.getBidIncrement()) + " VNĐ");
        Label endTimeLabel = createDetailLine("Kết thúc", item.getEndTime() == null ? "Chưa thiết lập" : item.getEndTime().format(END_TIME_FORMATTER));

        Label descTitle = new Label("Mô tả");
        descTitle.setStyle("-fx-text-fill: #334155; -fx-font-size: 13; -fx-font-weight: bold;");

        Label descriptionLabel = new Label(item.getDescription() == null || item.getDescription().isBlank()
                ? "Không có mô tả."
                : item.getDescription());
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(235);
        descriptionLabel.setStyle(
                "-fx-text-fill: #64748b;" +
                        "-fx-font-size: 12;" +
                        "-fx-background-color: #f8fafc;" +
                        "-fx-padding: 10;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 12;"
        );

        panel.getChildren().addAll(
                imageBox,
                infoTitle,
                typeLabel,
                startPriceLabel,
                incrementLabel,
                endTimeLabel,
                descTitle,
                descriptionLabel
        );
        return panel;
    }

    private VBox createAuctionRoomBidPanel(Item item) {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(16));
        panel.setMinWidth(380);
        panel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 18;"
        );

        Label panelTitle = new Label("Khu vực đặt giá");
        panelTitle.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 18; -fx-font-weight: bold;");

        activeBidCurrentPriceLabel = new Label();
        activeBidCurrentPriceLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 18; -fx-font-weight: bold;");

        Label incrementLabel = new Label("Bước giá tối thiểu: " + currencyFormat.format(item.getBidIncrement()) + " VNĐ");
        incrementLabel.setStyle("-fx-text-fill: #334155; -fx-font-size: 13;");

        activeBidMinBidLabel = new Label();
        activeBidMinBidLabel.setStyle("-fx-text-fill: #0f766e; -fx-font-size: 14; -fx-font-weight: bold;");

        LineChart<String, Number> bidTrendChart = createBidTrendChart(item);
        activeBidTrendSeries = bidTrendChart.getData().isEmpty() ? null : bidTrendChart.getData().get(0);
        VBox bidTrendChartBox = createBidTrendChartBox(bidTrendChart);
        VBox.setVgrow(bidTrendChartBox, Priority.ALWAYS);

        activeBidAmountField = new TextField();
        activeBidAmountField.setPromptText("Ví dụ: " + currencyFormat.format(calculateMinimumBid(item)));
        activeBidAmountField.setPrefHeight(42);
        activeBidAmountField.setStyle(
                "-fx-background-color: #f8fafc;" +
                        "-fx-border-color: #cbd5e1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 0 12;" +
                        "-fx-font-size: 14;"
        );

        activeBidErrorLabel = new Label("");
        activeBidErrorLabel.setWrapText(true);
        activeBidErrorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12;");

        activeBidStatusLabel = new Label("");
        activeBidStatusLabel.setWrapText(true);
        activeBidStatusLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12;");

        activeBidSubmitButton = new Button("Xác nhận đặt giá");
        activeBidSubmitButton.setPrefHeight(42);
        activeBidSubmitButton.setMaxWidth(Double.MAX_VALUE);
        activeBidSubmitButton.setStyle(
                "-fx-background-color: #10b981;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;"
        );

        activeBidAmountField.textProperty().addListener((observable, oldValue, newValue) -> {
            String cleaned = newValue.replaceAll("[^0-9]", "");
            if (!cleaned.equals(newValue)) {
                activeBidAmountField.setText(cleaned);
                return;
            }
            validateActiveBidAmount();
        });

        activeBidSubmitButton.setOnAction(event -> submitActiveAuctionRoomBid());

        panel.getChildren().addAll(
                panelTitle,
                activeBidCurrentPriceLabel,
                incrementLabel,
                activeBidMinBidLabel,
                bidTrendChartBox,
                new Label("Số tiền đặt giá:"),
                activeBidAmountField,
                activeBidErrorLabel,
                activeBidSubmitButton,
                activeBidStatusLabel
        );

        refreshActiveBidDialogLabels();
        validateActiveBidAmount();
        return panel;
    }

    private VBox createAuctionRoomChatPanel() {
        VBox panel = new VBox(12);
        panel.setPrefWidth(320);
        panel.setMinWidth(300);
        panel.setPadding(new Insets(16));
        panel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 18;"
        );

        Label chatTitle = new Label("Chat trong phòng");
        chatTitle.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 18; -fx-font-weight: bold;");

        Label chatSubtitle = new Label("Tin nhắn chỉ hiển thị trong phòng đấu giá của sản phẩm này.");
        chatSubtitle.setWrapText(true);
        chatSubtitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12;");

        auctionRoomChatMessagesBox = new VBox(10);
        auctionRoomChatMessagesBox.setPadding(new Insets(10));
        showAuctionRoomEmptyChatPlaceholder();

        auctionRoomChatScrollPane = new ScrollPane(auctionRoomChatMessagesBox);
        auctionRoomChatScrollPane.setFitToWidth(true);
        auctionRoomChatScrollPane.setStyle("-fx-background-color: transparent; -fx-background: #f8fafc;");
        VBox.setVgrow(auctionRoomChatScrollPane, Priority.ALWAYS);

        auctionRoomChatField = new TextField();
        auctionRoomChatField.setPromptText("Nhập tin nhắn...");
        auctionRoomChatField.setPrefHeight(38);
        auctionRoomChatField.setStyle(
                "-fx-background-color: #f8fafc;" +
                        "-fx-border-color: #cbd5e1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 0 10;"
        );
        auctionRoomChatField.setOnAction(event -> sendAuctionRoomChatMessage());

        auctionRoomChatSendButton = new Button("Gửi");
        auctionRoomChatSendButton.setPrefHeight(38);
        auctionRoomChatSendButton.setStyle(
                "-fx-background-color: #3b82f6;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;"
        );
        auctionRoomChatSendButton.setOnAction(event -> sendAuctionRoomChatMessage());

        HBox inputRow = new HBox(8, auctionRoomChatField, auctionRoomChatSendButton);
        HBox.setHgrow(auctionRoomChatField, Priority.ALWAYS);

        panel.getChildren().addAll(chatTitle, chatSubtitle, auctionRoomChatScrollPane, inputRow);
        return panel;
    }

    private void loadItemImageIntoBox(Item item, VBox imageBox, double fitWidth, double fitHeight) {
        imageBox.getChildren().clear();

        if (item.getImagePath() == null || item.getImagePath().isBlank()) {
            imageBox.getChildren().add(createImagePlaceholder("Chưa có ảnh\nsản phẩm"));
            return;
        }

        Task<byte[]> loadImageTask = new Task<>() {
            @Override
            protected byte[] call() throws Exception {
                return ClientApp.getImageBytes(item.getImagePath());
            }
        };
        loadImageTask.setOnSucceeded(e -> {
            byte[] imageBytes = loadImageTask.getValue();
            imageBox.getChildren().clear();
            if (imageBytes != null && imageBytes.length > 0) {
                ImageView imageView = new ImageView(new Image(new ByteArrayInputStream(imageBytes)));
                imageView.setFitWidth(fitWidth);
                imageView.setFitHeight(fitHeight);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                imageBox.getChildren().add(imageView);
            } else {
                imageBox.getChildren().add(createImagePlaceholder("Không tải được ảnh"));
            }
        });
        loadImageTask.setOnFailed(e -> {
            logger.log(Level.WARNING, "Không tải được ảnh sản phẩm (auction room): {0}", item.getImagePath());
            imageBox.getChildren().clear();
            imageBox.getChildren().add(createImagePlaceholder("Lỗi tải ảnh"));
        });
        Thread imageThread = new Thread(loadImageTask, "auction-room-image-loader");
        imageThread.setDaemon(true);
        imageThread.start();
    }

    private void submitActiveAuctionRoomBid() {
        if (activeBidDialogItem == null || activeBidAmountField == null) {
            return;
        }

        validateActiveBidAmount();

        if (activeBidSubmitButton != null && activeBidSubmitButton.isDisabled()) {
            return;
        }

        try {
            double amount = Double.parseDouble(activeBidAmountField.getText().trim());
            pendingBidItemId = activeBidDialogItem.getId();
            pendingBidAmount = amount;

            if (activeBidSubmitButton != null) {
                activeBidSubmitButton.setDisable(true);
            }
            if (activeBidStatusLabel != null) {
                activeBidStatusLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 12; -fx-font-weight: bold;");
                activeBidStatusLabel.setText("Đang gửi lệnh đặt giá...");
            }

            if (onSubmitBid != null) {
                onSubmitBid.accept(activeBidDialogItem.getId(), amount);
            }
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Người dùng nhập số tiền đặt giá không hợp lệ: {0}",
                    activeBidAmountField != null ? activeBidAmountField.getText() : "null");
            if (activeBidErrorLabel != null) {
                activeBidErrorLabel.setText("Số tiền không hợp lệ.");
            }
        }
    }

    private void validateActiveBidAmount() {
        if (activeBidDialogItem == null || activeBidAmountField == null || activeBidSubmitButton == null) {
            return;
        }

        String status = getDisplayStatus(activeBidDialogItem);
        String raw = activeBidAmountField.getText() == null ? "" : activeBidAmountField.getText().trim();

        if (!"ACTIVE".equals(status)) {
            activeBidSubmitButton.setDisable(true);
            if (activeBidErrorLabel != null) {
                activeBidErrorLabel.setText("Phiên đấu giá chưa diễn ra hoặc đã kết thúc.");
            }
            return;
        }

        if (isCurrentUserLastBidder(activeBidDialogItem)) {
            activeBidSubmitButton.setDisable(true);
            if (activeBidErrorLabel != null) {
                activeBidErrorLabel.setText(getConsecutiveBidWarning());
            }
            return;
        }

        if (raw.isBlank()) {
            activeBidSubmitButton.setDisable(true);
            if (activeBidErrorLabel != null) {
                activeBidErrorLabel.setText("");
            }
            return;
        }

        try {
            double amount = Double.parseDouble(raw);
            double minimumBid = calculateMinimumBid(activeBidDialogItem);

            if (amount < minimumBid) {
                activeBidSubmitButton.setDisable(true);
                if (activeBidErrorLabel != null) {
                    activeBidErrorLabel.setText("Giá phải từ " + currencyFormat.format(minimumBid) + " VNĐ trở lên.");
                }
            } else {
                activeBidSubmitButton.setDisable(false);
                if (activeBidErrorLabel != null) {
                    activeBidErrorLabel.setText("");
                }
            }
        } catch (NumberFormatException e) {
            activeBidSubmitButton.setDisable(true);
            if (activeBidErrorLabel != null) {
                activeBidErrorLabel.setText("Số tiền không hợp lệ.");
            }
        }
    }

    public void refreshAuctionRoom() {
        refreshAuctionRoomHeader();
        refreshActiveBidDialogLabels();
        validateActiveBidAmount();
    }

    private void refreshAuctionRoomHeader() {
        if (activeBidDialogItem == null) {
            return;
        }

        String status = getDisplayStatus(activeBidDialogItem);

        if (auctionRoomStatusBadge != null) {
            auctionRoomStatusBadge.setText(getStatusText(status));
            auctionRoomStatusBadge.setStyle(
                    "-fx-background-color: " + getStatusColor(status) + ";" +
                            "-fx-text-fill: white;" +
                            "-fx-padding: 7 13;" +
                            "-fx-background-radius: 999;" +
                            "-fx-font-size: 12;" +
                            "-fx-font-weight: bold;"
            );
        }

        if (auctionRoomCountdownLabel != null) {
            auctionRoomCountdownLabel.setText(getCountdownText(activeBidDialogItem));
            auctionRoomCountdownLabel.setStyle(getCountdownStyle(status));
        }
    }

    private void startAuctionRoomCountdown() {
        stopAuctionRoomCountdown();
        auctionRoomCountdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    refreshAuctionRoomHeader();
                    refreshActiveBidDialogLabels();
                    validateActiveBidAmount();
                })
        );
        auctionRoomCountdownTimeline.setCycleCount(Timeline.INDEFINITE);
        auctionRoomCountdownTimeline.play();
    }

    private void stopAuctionRoomCountdown() {
        if (auctionRoomCountdownTimeline != null) {
            auctionRoomCountdownTimeline.stop();
            auctionRoomCountdownTimeline = null;
        }
    }

    public void clearAuctionRoomState() {
        stopAuctionRoomCountdown();
        activeAuctionRoomItem = null;
        auctionRoomChatMessagesBox = null;
        auctionRoomChatScrollPane = null;
        auctionRoomChatField = null;
        auctionRoomChatSendButton = null;
        auctionRoomCountdownLabel = null;
        auctionRoomStatusBadge = null;
        auctionRoomChatHasMessages = false;
        clearActiveBidDialogState();

        if (auctionRoomRoot != null) {
            auctionRoomRoot.getChildren().clear();
        }
    }

    private void requestItemBidHistory(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        if (onRequestItemBidHistory != null) {
            onRequestItemBidHistory.accept(itemId);
        }
    }

    private void requestAuctionChatHistory(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        if (onRequestAuctionChatHistory != null) {
            onRequestAuctionChatHistory.accept(itemId);
        }
    }

    private void sendAuctionRoomChatMessage() {
        if (activeBidDialogItemId == null || auctionRoomChatField == null) {
            return;
        }

        String text = auctionRoomChatField.getText() == null ? "" : auctionRoomChatField.getText().trim();
        if (text.isBlank()) {
            return;
        }

        if (onSendAuctionChatMessage != null) {
            onSendAuctionChatMessage.accept(activeBidDialogItemId, text);
        }
        auctionRoomChatField.clear();
    }

    public void updateAuctionRoomChatHistory(Object payload) {
        if (!(payload instanceof Object[])) {
            return;
        }

        Object[] data = (Object[]) payload;
        if (data.length < 2) {
            return;
        }

        String itemId = String.valueOf(data[0]);
        if (activeBidDialogItemId == null || !activeBidDialogItemId.equals(itemId) || auctionRoomChatMessagesBox == null) {
            return;
        }

        auctionRoomChatMessagesBox.getChildren().clear();
        auctionRoomChatHasMessages = false;

        Object rows = data[1];
        if (rows instanceof List<?>) {
            for (Object row : (List<?>) rows) {
                appendAuctionRoomChatMessage(row);
            }
        }

        if (!auctionRoomChatHasMessages) {
            showAuctionRoomEmptyChatPlaceholder();
        }
    }

    public void appendAuctionRoomChatMessage(Object payload) {
        if (!(payload instanceof AuctionChatMessage)) {
            return;
        }

        AuctionChatMessage chatMessage = (AuctionChatMessage) payload;
        if (activeBidDialogItemId == null || !activeBidDialogItemId.equals(chatMessage.getItemId())
                || auctionRoomChatMessagesBox == null) {
            return;
        }

        if (!auctionRoomChatHasMessages) {
            auctionRoomChatMessagesBox.getChildren().clear();
            auctionRoomChatHasMessages = true;
        }

        boolean mine = currentUser != null && currentUser.getId() != null
                && currentUser.getId().equals(chatMessage.getSenderId());

        Label metaLabel = new Label((mine ? "Bạn" : safeText(chatMessage.getSenderName()))
                + " · " + (chatMessage.getSentAt() == null ? "" : chatMessage.getSentAt().format(CHART_TIME_FORMATTER)));
        metaLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10;");

        Label contentLabel = new Label(chatMessage.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(230);
        contentLabel.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 12;");

        VBox bubble = new VBox(4, metaLabel, contentLabel);
        bubble.setMaxWidth(250);
        bubble.setPadding(new Insets(9));
        bubble.setStyle(
                "-fx-background-color: " + (mine ? "#dbeafe" : "#f8fafc") + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: " + (mine ? "#93c5fd" : "#e2e8f0") + ";" +
                        "-fx-border-radius: 12;"
        );

        HBox row = new HBox(bubble);
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        auctionRoomChatMessagesBox.getChildren().add(row);

        Platform.runLater(() -> {
            if (auctionRoomChatScrollPane != null) {
                auctionRoomChatScrollPane.setVvalue(1.0);
            }
        });
    }

    private void showAuctionRoomEmptyChatPlaceholder() {
        if (auctionRoomChatMessagesBox == null) {
            return;
        }

        Label emptyLabel = new Label("Chưa có tin nhắn nào. Hãy bắt đầu cuộc trò chuyện trong phòng đấu giá.");
        emptyLabel.setWrapText(true);
        emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12; -fx-padding: 12;");
        auctionRoomChatMessagesBox.getChildren().setAll(emptyLabel);
    }

    public void showAuctionRoomChatError(Object payload) {
        if (activeBidStatusLabel != null) {
            activeBidStatusLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12; -fx-font-weight: bold;");
            activeBidStatusLabel.setText(String.valueOf(payload));
        }
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

    private VBox createBidTrendChartBox(LineChart<String, Number> bidTrendChart) {
        Label chartTitle = new Label("Biểu đồ giá theo thời gian");
        chartTitle.setStyle(
                "-fx-text-fill: #334155;" +
                        "-fx-font-size: 13;" +
                        "-fx-font-weight: bold;"
        );

        VBox chartBox = new VBox(8, chartTitle, bidTrendChart);
        chartBox.setMaxWidth(Double.MAX_VALUE);
        chartBox.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 10;"
        );

        return chartBox;
    }

    private LineChart<String, Number> createBidTrendChart(Item item) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Mốc giá");
        xAxis.setTickLabelRotation(-25);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Giá (VNĐ)");
        yAxis.setForceZeroInRange(false);
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
            @Override
            public String toString(Number object) {
                return currencyFormat.format(object.doubleValue());
            }
        });

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setLegendVisible(false);
        chart.setPrefHeight(230);
        chart.setMinHeight(210);
        chart.setMaxWidth(Double.MAX_VALUE);
        chart.setTitle(null);

        URL chartCss = getClass().getResource("/styles/chart-style.css");
        if (chartCss != null) {
            chart.getStylesheets().add(chartCss.toExternalForm());
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Giá đấu toàn server");
        chart.getData().add(series);
        rebuildBidTrendSeries(series, item, new ArrayList<>());

        return chart;
    }

    private void rebuildActiveBidChartFromHistory(List<BidHistoryController.BidHistoryRecord> productBidHistory) {
        if (activeBidTrendSeries == null || activeBidDialogItem == null) {
            return;
        }

        rebuildBidTrendSeries(activeBidTrendSeries, activeBidDialogItem, productBidHistory);
    }

    private void rebuildBidTrendSeries(XYChart.Series<String, Number> series,
                                       Item item,
                                       List<BidHistoryController.BidHistoryRecord> productBidHistory) {
        if (series == null || item == null) {
            return;
        }

        series.getData().clear();
        series.getData().add(new XYChart.Data<>("Khởi điểm", item.getStartingPrice()));

        List<BidHistoryController.BidHistoryRecord> sortedHistory = productBidHistory == null
                ? new ArrayList<>()
                : productBidHistory.stream()
                .filter(record -> item.getId().equals(record.getItemId()))
                .sorted(Comparator.comparing(BidHistoryController.BidHistoryRecord::getBidTime))
                .collect(Collectors.toList());

        if (sortedHistory.size() > 10) {
            sortedHistory = sortedHistory.subList(sortedHistory.size() - 10, sortedHistory.size());
        }

        int bidIndex = 1;
        for (BidHistoryController.BidHistoryRecord record : sortedHistory) {
            String timeLabel = record.getBidTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            series.getData().add(new XYChart.Data<>("L" + bidIndex + " " + timeLabel, record.getBidAmount()));
            bidIndex++;
        }

        boolean lastPointIsCurrentPrice = !sortedHistory.isEmpty()
                && Double.compare(sortedHistory.get(sortedHistory.size() - 1).getBidAmount(), item.getCurrentPrice()) == 0;

        if (sortedHistory.isEmpty() || !lastPointIsCurrentPrice) {
            series.getData().add(new XYChart.Data<>("Hiện tại", item.getCurrentPrice()));
        }
    }

    private boolean isCurrentUserLastBidder(Item item) {
        return currentUser != null
                && item != null
                && item.getCurrentWinnerId() != null
                && item.getCurrentWinnerId().equals(currentUser.getId());
    }

    private String getConsecutiveBidWarning() {
        return "Bạn đang là người đặt giá gần nhất cho sản phẩm này. Vui lòng chờ người khác đặt giá trước.";
    }

    private double calculateMinimumBid(Item item) {
        if (item == null) {
            return 0;
        }

        return item.getCurrentWinnerId() == null || item.getCurrentWinnerId().isBlank()
                ? item.getStartingPrice()
                : item.getCurrentPrice() + item.getBidIncrement();
    }

    private void refreshActiveBidDialogLabels() {
        if (activeBidDialogItem == null) {
            return;
        }

        double minimumBid = calculateMinimumBid(activeBidDialogItem);

        if (activeBidCurrentPriceLabel != null) {
            activeBidCurrentPriceLabel.setText("Giá hiện tại: "
                    + currencyFormat.format(activeBidDialogItem.getCurrentPrice()) + " VNĐ");
        }

        if (activeBidMinBidLabel != null) {
            activeBidMinBidLabel.setText("Bạn cần đặt tối thiểu: "
                    + currencyFormat.format(minimumBid) + " VNĐ");
        }

        if (activeBidAmountField != null) {
            activeBidAmountField.setPromptText("Ví dụ: " + currencyFormat.format(minimumBid));
        }
    }

    private void appendActiveBidChartPoint(double amount, String labelPrefix, boolean forceAdd) {
        if (activeBidTrendSeries == null) {
            return;
        }

        if (!forceAdd && isLastActiveBidChartValue(amount)) {
            return;
        }

        String baseLabel = labelPrefix + " " + LocalDateTime.now().format(CHART_TIME_FORMATTER);
        String label = baseLabel;
        int duplicateIndex = 2;

        while (containsActiveBidChartLabel(label)) {
            label = baseLabel + " (" + duplicateIndex + ")";
            duplicateIndex++;
        }

        activeBidTrendSeries.getData().add(new XYChart.Data<>(label, amount));

        while (activeBidTrendSeries.getData().size() > 12) {
            int removeIndex = activeBidTrendSeries.getData().size() > 1 ? 1 : 0;
            activeBidTrendSeries.getData().remove(removeIndex);
        }
    }

    private boolean containsActiveBidChartLabel(String label) {
        if (activeBidTrendSeries == null) {
            return false;
        }

        for (XYChart.Data<String, Number> data : activeBidTrendSeries.getData()) {
            if (label.equals(data.getXValue())) {
                return true;
            }
        }

        return false;
    }

    private boolean isLastActiveBidChartValue(double amount) {
        if (activeBidTrendSeries == null || activeBidTrendSeries.getData().isEmpty()) {
            return false;
        }

        XYChart.Data<String, Number> lastPoint = activeBidTrendSeries.getData()
                .get(activeBidTrendSeries.getData().size() - 1);
        return Double.compare(lastPoint.getYValue().doubleValue(), amount) == 0;
    }

    public void handleBidResponse(Object payload) {
        String responseMessage = String.valueOf(payload);
        boolean success = responseMessage.startsWith("Đặt giá thành công");

        if (success) {
            updateActiveBidDialogAfterSuccessfulBid(responseMessage);
        } else {
            if (activeBidStatusLabel != null) {
                activeBidStatusLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12; -fx-font-weight: bold;");
                activeBidStatusLabel.setText(responseMessage);
            }

            validateActiveBidAmount();

            Alert bidAlert = new Alert(Alert.AlertType.WARNING);
            bidAlert.setTitle("Kết quả đặt giá");
            bidAlert.setHeaderText("Đặt giá thất bại");
            bidAlert.setContentText(responseMessage);
            bidAlert.showAndWait();
        }

        pendingBidItemId = null;
        pendingBidAmount = 0;
    }

    private void updateActiveBidDialogAfterSuccessfulBid(String responseMessage) {
        if (pendingBidItemId == null || !pendingBidItemId.equals(activeBidDialogItemId)
                || activeBidDialogItem == null) {
            Alert bidAlert = new Alert(Alert.AlertType.INFORMATION);
            bidAlert.setTitle("Kết quả đặt giá");
            bidAlert.setHeaderText("Đặt giá thành công");
            bidAlert.setContentText(responseMessage);
            bidAlert.showAndWait();
            return;
        }

        activeBidDialogItem.setCurrentPrice(pendingBidAmount);
        if (currentUser != null) {
            activeBidDialogItem.setCurrentWinnerId(currentUser.getId());
            activeBidDialogLastKnownWinnerId = currentUser.getId();
        }

        appendActiveBidChartPoint(pendingBidAmount, "Vừa đặt", true);
        requestItemBidHistory(activeBidDialogItemId);
        refreshActiveBidDialogLabels();
        refreshAuctionRoomHeader();
        validateActiveBidAmount();

        if (activeBidAmountField != null) {
            activeBidAmountField.clear();
        }

        if (activeBidSubmitButton != null) {
            activeBidSubmitButton.setDisable(true);
        }

        if (activeBidStatusLabel != null) {
            activeBidStatusLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 12; -fx-font-weight: bold;");
            activeBidStatusLabel.setText("Đặt giá thành công! Đang đồng bộ biểu đồ chung toàn server.");
        }
    }

    public void updateActiveBidDialogFromItemUpdate(Item updatedItem) {
        if (updatedItem == null || activeBidDialogItem == null || activeBidDialogItemId == null
                || !activeBidDialogItemId.equals(updatedItem.getId())) {
            return;
        }

        boolean winnerChanged = !Objects.equals(activeBidDialogLastKnownWinnerId, updatedItem.getCurrentWinnerId());
        boolean priceChanged = Double.compare(activeBidDialogItem.getCurrentPrice(), updatedItem.getCurrentPrice()) != 0;

        activeBidDialogItem.setCurrentPrice(updatedItem.getCurrentPrice());
        activeBidDialogItem.setCurrentWinnerId(updatedItem.getCurrentWinnerId());
        activeBidDialogItem.setStatus(updatedItem.getStatus());
        activeBidDialogItem.setEndTime(updatedItem.getEndTime());
        activeBidDialogLastKnownWinnerId = updatedItem.getCurrentWinnerId();

        if (winnerChanged || priceChanged) {
            appendActiveBidChartPoint(updatedItem.getCurrentPrice(), "Cập nhật", winnerChanged);
            requestItemBidHistory(updatedItem.getId());
        }

        refreshActiveBidDialogLabels();
        refreshAuctionRoomHeader();
        validateActiveBidAmount();

        if (activeBidStatusLabel != null) {
            if (isCurrentUserLastBidder(activeBidDialogItem)) {
                activeBidStatusLabel.setStyle("-fx-text-fill: #d97706; -fx-font-size: 12; -fx-font-weight: bold;");
                activeBidStatusLabel.setText(getConsecutiveBidWarning());
            } else if (winnerChanged || priceChanged) {
                activeBidStatusLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 12; -fx-font-weight: bold;");
                activeBidStatusLabel.setText("Đã có lượt đặt giá mới. Bạn có thể đặt tiếp nếu muốn.");
            }
        }
    }

    private void clearActiveBidDialogState() {
        activeBidDialogItemId = null;
        activeBidDialogLastKnownWinnerId = null;
        activeBidDialogItem = null;
        activeBidCurrentPriceLabel = null;
        activeBidMinBidLabel = null;
        activeBidStatusLabel = null;
        activeBidErrorLabel = null;
        activeBidAmountField = null;
        activeBidSubmitButton = null;
        activeBidTrendSeries = null;
        activeItemBidHistory.clear();
        pendingBidItemId = null;
        pendingBidAmount = 0;
    }

    public void updateActiveBidChartFromHistoryPayload(Object payload) {
        if (!(payload instanceof Object[])) {
            return;
        }

        Object[] data = (Object[]) payload;
        if (data.length < 2) {
            return;
        }

        String itemId = String.valueOf(data[0]);
        if (activeBidDialogItemId == null || !activeBidDialogItemId.equals(itemId)) {
            return;
        }

        activeItemBidHistory.clear();
        Object rows = data[1];

        if (rows instanceof List<?>) {
            for (Object row : (List<?>) rows) {
                BidHistoryController.BidHistoryRecord record = createBidHistoryRecordFromRow(row);
                if (record != null) {
                    activeItemBidHistory.add(record);
                }
            }
        }

        rebuildActiveBidChartFromHistory(activeItemBidHistory);

        if (activeBidStatusLabel != null && !activeItemBidHistory.isEmpty()) {
            activeBidStatusLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 12; -fx-font-weight: bold;");
            activeBidStatusLabel.setText("Biểu đồ đã đồng bộ theo lịch sử đặt giá chung của toàn server.");
        }
    }

    private BidHistoryController.BidHistoryRecord createBidHistoryRecordFromRow(Object row) {
        if (!(row instanceof Object[])) {
            return null;
        }

        Object[] data = (Object[]) row;
        if (data.length < 7) {
            return null;
        }

        try {
            String itemId = String.valueOf(data[0]);
            String itemName = String.valueOf(data[1]);
            String itemType = String.valueOf(data[2]);
            double bidAmount = ((Number) data[3]).doubleValue();
            java.time.LocalDateTime bidTime = (java.time.LocalDateTime) data[4];
            String auctionStatus = String.valueOf(data[5]);
            String result = String.valueOf(data[6]);

            return new BidHistoryController.BidHistoryRecord(
                    itemId,
                    itemName,
                    itemType,
                    bidAmount,
                    bidTime,
                    auctionStatus,
                    result
            );
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Không đọc được một dòng lịch sử đặt giá sản phẩm", e);
            return null;
        }
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
}
