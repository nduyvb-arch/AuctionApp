package org.example.client.controllers;
import org.example.client.ClientApp;
import javafx.animation.KeyFrame;
import javafx.event.ActionEvent;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.common.Message;
import org.example.common.model.item.Item;
import org.example.common.model.chat.AuctionChatMessage;
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
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.util.Duration;

public class HomeController implements Initializable {

    // ═══════════════════════════════════════════════════════════
    // SIDEBAR COMPONENTS
    // ═══════════════════════════════════════════════════════════
    @FXML private VBox sidebarMenu;
    @FXML private Label currentRoleLabel;
    @FXML private Button roleSwitcherButton;

    @FXML private Button homeMenuItem;
    @FXML private Button bidHistoryMenuItem;
    @FXML private Button addItemMenuItem;
    @FXML private Button myItemsMenuItem;
    @FXML private Button salesHistoryMenuItem;
    @FXML private Button accountMenuItem;
    @FXML private Button notificationsMenuItem;
    @FXML private Button logoutButton;

    @FXML private Label bidderMenuLabel;
    @FXML private Label sellerMenuLabel;

    // ═══════════════════════════════════════════════════════════
    // TOP BAR COMPONENTS
    // ═══════════════════════════════════════════════════════════
    @FXML private Label pageTitle;
    @FXML private Label userInfoLabel;
    @FXML private Label balanceLabel;

    // ═══════════════════════════════════════════════════════════
    // HOME VIEW COMPONENTS
    // ═══════════════════════════════════════════════════════════
    @FXML private VBox homeView;
    @FXML private VBox auctionRoomView;
    @FXML private TextField searchTextField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private Button refreshButton;
    @FXML private FlowPane itemFlowPane;

    // ═══════════════════════════════════════════════════════════
    // SUB-VIEWS
    // ═══════════════════════════════════════════════════════════
    @FXML private VBox bidHistoryViewPane;
    @FXML private BidHistoryController bidHistoryViewPaneController;

    @FXML private VBox addItemViewPane;
    @FXML private AddItemViewController addItemViewPaneController;

    @FXML private VBox myItemsViewPane;
    @FXML private MyItemsController myItemsViewPaneController;

    @FXML private VBox salesHistoryViewPane;
    @FXML private SalesHistoryController salesHistoryViewPaneController;

    @FXML private VBox accountViewPane;
    @FXML private AccountViewController accountViewPaneController;

    @FXML private VBox contentContainer;

    // ═══════════════════════════════════════════════════════════
    // SHARED DATA
    // ═══════════════════════════════════════════════════════════
    private final List<Item> items = new ArrayList<>();
    private final List<BidHistoryController.BidHistoryRecord> bidHistory = new ArrayList<>();
    private final List<String> notifications = new ArrayList<>();

    private User currentUser;
    private boolean sellerMode;

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

    // ═══════════════════════════════════════════════════════════
    // INITIALIZE
    // ═══════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentUser = ClientApp.getCurrentUser();
        sellerMode = ClientApp.isSellerSelected();

        setupHomeViewFilters();

        hideAllViews();

        if (bidHistoryViewPaneController != null) {
            bidHistoryViewPaneController.setup(bidHistory);
        }

        // 🔥 Đã xóa ClientApp.getOutputStream() cho 3 đàn em
        if (addItemViewPaneController != null) {
            addItemViewPaneController.setup(currentUser, this::loadInitialItems);
        }

        if (myItemsViewPaneController != null) {
            myItemsViewPaneController.setup(items, currentUser, this::loadInitialItems);
        }

        if (salesHistoryViewPaneController != null) {
            salesHistoryViewPaneController.setup(items, currentUser, this::loadInitialItems);
        }

        if (accountViewPaneController != null) {
            accountViewPaneController.setup(currentUser, this::onCurrentUserUpdated);
        }

        updateUIBasedOnRole();

        if (ClientApp.shouldOpenAccountOnHomeLoad()) {
            switchToAccountView();
        } else if (sellerMode) {
            switchToAddItemView();
        } else {
            switchToHomeView();
        }

        listenForServerUpdates();
        loadInitialItems();
    }

    // ═══════════════════════════════════════════════════════════
    // CHUYỂN MÀN HÌNH
    // ═══════════════════════════════════════════════════════════
    @FXML
    private void switchToHomeView() {
        clearAuctionRoomState();
        showView(homeView);
        pageTitle.setText("Trang chủ sàn đấu giá");
        loadInitialItems();
    }

    @FXML
    private void switchToBidHistoryView() {
        clearAuctionRoomState();
        showView(bidHistoryViewPane);
        pageTitle.setText("Lịch sử đấu giá");
        requestMyBidHistory();

        if (bidHistoryViewPaneController != null) {
            bidHistoryViewPaneController.refreshBidHistoryView();
        }
    }

    @FXML
    private void switchToAddItemView() {
        clearAuctionRoomState();
        showView(addItemViewPane);
        pageTitle.setText("➕ Đăng sản phẩm mới");
    }

    @FXML
    private void switchToMyItemsView() {
        clearAuctionRoomState();
        showView(myItemsViewPane);
        pageTitle.setText("Sản phẩm của tôi");

        if (myItemsViewPaneController != null) {
            myItemsViewPaneController.updateData(items);
        }
    }

    @FXML
    private void switchToSalesHistoryView() {
        clearAuctionRoomState();
        showView(salesHistoryViewPane);
        pageTitle.setText("Lịch sử bán hàng");

        if (salesHistoryViewPaneController != null) {
            salesHistoryViewPaneController.updateData(items);
        }
    }

    @FXML
    private void switchToAccountView() {
        clearAuctionRoomState();
        showView(accountViewPane);
        pageTitle.setText("Tài khoản");

        if (accountViewPaneController != null) {
            accountViewPaneController.updateUser(currentUser);
        }
    }

    @FXML
    private void switchToNotificationsView() {
        pageTitle.setText("Thông báo");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText("Thông báo của bạn");

        if (notifications.isEmpty()) {
            alert.setContentText("Chưa có thông báo nào.");
        } else {
            StringBuilder builder = new StringBuilder();

            for (int i = notifications.size() - 1; i >= 0; i--) {
                builder.append("• ").append(notifications.get(i)).append("\n\n");
            }

            alert.setContentText(builder.toString());
        }

        alert.getDialogPane().setPrefWidth(600);
        alert.showAndWait();
    }

    @FXML
    private void switchToRoleSelectionView() {
        try {
            ClientApp.switchToRoleSelection();
        } catch (Exception e) {
            showError("Không thể quay lại màn chọn vai trò", e.getMessage());
        }
    }

    private void showView(VBox view) {
        hideAllViews();
        setViewState(view, true);
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content == null || content.isBlank() ? "Không rõ nguyên nhân." : content);
        alert.showAndWait();
    }

    private void hideAllViews() {
        setViewState(homeView, false);
        setViewState(auctionRoomView, false);
        setViewState(bidHistoryViewPane, false);
        setViewState(addItemViewPane, false);
        setViewState(myItemsViewPane, false);
        setViewState(salesHistoryViewPane, false);
        setViewState(accountViewPane, false);
    }

    private void setViewState(VBox view, boolean active) {
        if (view == null) {
            return;
        }

        view.setVisible(active);
        view.setManaged(active);
    }

    // ═══════════════════════════════════════════════════════════
    // HOME VIEW
    // ═══════════════════════════════════════════════════════════
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
        loadInitialItems();
    }

    // 🔥 FIX 1: Lấy danh sách sản phẩm (Bỏ Task)
    private void loadInitialItems() {
        ClientApp.sendMessage(new Message("GET_ALL_ITEMS", null));
    }

    private void applyHomeFiltersAndSort() {
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

    // ═══════════════════════════════════════════════════════════
    // CARD SẢN PHẨM & POPUP (NGUYÊN BẢN 100%)
    // ═══════════════════════════════════════════════════════════
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

        // Không hiển thị ảnh ở card ngoài trang chủ.
        // Ảnh chỉ được tải và hiển thị trong popup chi tiết sản phẩm.

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
            Platform.runLater(() -> openAuctionRoom(item));
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



    private void openAuctionRoom(Item item) {
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

        auctionRoomView.getChildren().setAll(createAuctionRoomLayout(item));
        showView(auctionRoomView);
        pageTitle.setText("Phòng đấu giá - " + item.getItemName());

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
        backButton.setOnAction(event -> switchToHomeView());

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

            submitBid(activeBidDialogItem.getId(), amount);
        } catch (NumberFormatException e) {
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

    private void clearAuctionRoomState() {
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

        if (auctionRoomView != null) {
            auctionRoomView.getChildren().clear();
        }
    }

    private void requestAuctionChatHistory(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        ClientApp.sendMessage(new Message("JOIN_AUCTION_ROOM", itemId));
    }

    private void sendAuctionRoomChatMessage() {
        if (activeBidDialogItemId == null || auctionRoomChatField == null) {
            return;
        }

        String text = auctionRoomChatField.getText() == null ? "" : auctionRoomChatField.getText().trim();
        if (text.isBlank()) {
            return;
        }

        ClientApp.sendMessage(new Message("SEND_AUCTION_CHAT", new Object[]{activeBidDialogItemId, text, ClientApp.getSelectedRole()}));
        auctionRoomChatField.clear();
    }

    private void updateAuctionRoomChatHistory(Object payload) {
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

    private void appendAuctionRoomChatMessage(Object payload) {
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

    private void showAuctionRoomChatError(Object payload) {
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

    // ═══════════════════════════════════════════════════════════
    // ĐẶT GIÁ
    // ═══════════════════════════════════════════════════════════
    private void openBidDialog(Item item) {
        Dialog<Void> bidDialog = new Dialog<>();
        bidDialog.setTitle("Đặt giá - " + item.getItemName());
        bidDialog.setHeaderText(null);

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setPrefWidth(520);
        root.setStyle("-fx-background-color: #f8fafc;");

        Label titleLabel = new Label("Nhập mức giá bạn muốn đặt");
        titleLabel.setStyle(
                "-fx-text-fill: #0f172a;" +
                        "-fx-font-size: 20;" +
                        "-fx-font-weight: bold;"
        );

        Label currentPriceLabel = new Label();
        currentPriceLabel.setStyle("-fx-text-fill: #334155; -fx-font-size: 13;");

        Label incrementLabel = new Label("Bước giá tối thiểu: " + currencyFormat.format(item.getBidIncrement()) + " VNĐ");
        incrementLabel.setStyle("-fx-text-fill: #334155; -fx-font-size: 13;");

        Label minBidLabel = new Label();
        minBidLabel.setStyle(
                "-fx-text-fill: #2563eb;" +
                        "-fx-font-size: 14;" +
                        "-fx-font-weight: bold;"
        );

        LineChart<String, Number> bidTrendChart = createBidTrendChart(item);
        XYChart.Series<String, Number> bidTrendSeries = bidTrendChart.getData().isEmpty()
                ? null
                : bidTrendChart.getData().get(0);
        VBox bidTrendChartBox = createBidTrendChartBox(bidTrendChart);

        TextField amountField = new TextField();
        amountField.setPromptText("Ví dụ: " + currencyFormat.format(calculateMinimumBid(item)));
        amountField.setPrefHeight(42);
        amountField.setEditable(true);
        amountField.setDisable(false);
        amountField.setFocusTraversable(true);
        amountField.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #cbd5e1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 0 12;" +
                        "-fx-font-size: 14;"
        );

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12;");

        Label statusLabel = new Label("");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12;");

        activeBidDialogItemId = item.getId();
        activeBidDialogItem = item;
        activeBidDialogLastKnownWinnerId = item.getCurrentWinnerId();
        activeBidCurrentPriceLabel = currentPriceLabel;
        activeBidMinBidLabel = minBidLabel;
        activeBidStatusLabel = statusLabel;
        activeBidErrorLabel = errorLabel;
        activeBidAmountField = amountField;
        activeBidTrendSeries = bidTrendSeries;

        refreshActiveBidDialogLabels();
        requestItemBidHistory(item.getId());

        root.getChildren().addAll(
                titleLabel,
                currentPriceLabel,
                incrementLabel,
                minBidLabel,
                bidTrendChartBox,
                new Label("Số tiền đặt giá:"),
                amountField,
                errorLabel,
                statusLabel
        );

        ButtonType submitButtonType = new ButtonType("Xác nhận đặt giá", ButtonBar.ButtonData.OTHER);
        bidDialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CLOSE);
        bidDialog.getDialogPane().setContent(root);

        Button submitButton = (Button) bidDialog.getDialogPane().lookupButton(submitButtonType);
        activeBidSubmitButton = submitButton;
        submitButton.setDisable(true);
        submitButton.setStyle(
                "-fx-background-color: #10b981;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;"
        );

        amountField.textProperty().addListener((observable, oldValue, newValue) -> {
            String cleaned = newValue.replaceAll("[^0-9]", "");

            if (!cleaned.equals(newValue)) {
                amountField.setText(cleaned);
                return;
            }

            if (cleaned.isBlank()) {
                submitButton.setDisable(true);
                errorLabel.setText("");
                return;
            }

            if (isCurrentUserLastBidder(item)) {
                submitButton.setDisable(true);
                errorLabel.setText(getConsecutiveBidWarning());
                return;
            }

            try {
                double amount = Double.parseDouble(cleaned);
                double minimumBid = calculateMinimumBid(item);

                if (amount < minimumBid) {
                    submitButton.setDisable(true);
                    errorLabel.setText("Giá phải từ " + currencyFormat.format(minimumBid) + " VNĐ trở lên.");
                } else {
                    submitButton.setDisable(false);
                    errorLabel.setText("");
                }
            } catch (NumberFormatException e) {
                submitButton.setDisable(true);
                errorLabel.setText("Số tiền không hợp lệ.");
            }
        });

        submitButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();

            String raw = amountField.getText().trim();
            if (raw.isBlank()) {
                errorLabel.setText("Vui lòng nhập số tiền đặt giá.");
                submitButton.setDisable(true);
                return;
            }

            if (isCurrentUserLastBidder(item)) {
                errorLabel.setText(getConsecutiveBidWarning());
                submitButton.setDisable(true);
                return;
            }

            try {
                double amount = Double.parseDouble(raw);
                double minimumBid = calculateMinimumBid(item);

                if (amount < minimumBid) {
                    errorLabel.setText("Giá phải từ " + currencyFormat.format(minimumBid) + " VNĐ trở lên.");
                    submitButton.setDisable(true);
                    return;
                }

                pendingBidItemId = item.getId();
                pendingBidAmount = amount;
                submitButton.setDisable(true);
                statusLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 12; -fx-font-weight: bold;");
                statusLabel.setText("Đang gửi lệnh đặt giá...");
                submitBid(item.getId(), amount);
            } catch (NumberFormatException e) {
                errorLabel.setText("Số tiền không hợp lệ.");
                submitButton.setDisable(true);
            }
        });

        bidDialog.setOnShown(event -> Platform.runLater(() -> {
            amountField.requestFocus();
            amountField.positionCaret(amountField.getText().length());
        }));

        bidDialog.setOnHidden(event -> {
            if (item.getId().equals(activeBidDialogItemId)) {
                clearActiveBidDialogState();
            }
        });

        bidDialog.showAndWait();
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

    private void handleBidResponse(Object payload) {
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

    private void updateActiveBidDialogFromItemUpdate(Item updatedItem) {
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

    // 🔥 FIX 2: Đặt giá an toàn (Bỏ Task)
    private void submitBid(String itemId, double bidAmount) {
        if (currentUser == null) return;
        Object[] bidData = {itemId, bidAmount, currentUser.getId()};
        ClientApp.sendMessage(new Message("BID", bidData));
    }

    // ═══════════════════════════════════════════════════════════
    // ROLE SWITCHER
    // ═══════════════════════════════════════════════════════════
    private void updateUIBasedOnRole() {
        boolean isSeller = sellerMode;
        boolean isBidder = !sellerMode;

        if (bidderMenuLabel != null) {
            bidderMenuLabel.setVisible(isBidder);
            bidderMenuLabel.setManaged(isBidder);
        }

        if (bidHistoryMenuItem != null) {
            bidHistoryMenuItem.setVisible(isBidder);
            bidHistoryMenuItem.setManaged(isBidder);
        }

        if (sellerMenuLabel != null) {
            sellerMenuLabel.setVisible(isSeller);
            sellerMenuLabel.setManaged(isSeller);
        }

        if (addItemMenuItem != null) {
            addItemMenuItem.setVisible(isSeller);
            addItemMenuItem.setManaged(isSeller);
        }

        if (myItemsMenuItem != null) {
            myItemsMenuItem.setVisible(isSeller);
            myItemsMenuItem.setManaged(isSeller);
        }

        if (salesHistoryMenuItem != null) {
            salesHistoryMenuItem.setVisible(isSeller);
            salesHistoryMenuItem.setManaged(isSeller);
        }

        currentRoleLabel.setText(isSeller ? "Người bán" : "Người đấu giá");
        roleSwitcherButton.setText(isSeller ? "Chuyển sang Người đấu giá" : "Chuyển sang Người bán");

        updateUserInfoLabel();
    }

    private void updateUserInfoLabel() {
        if (currentUser == null) {
            if (userInfoLabel != null) {
                userInfoLabel.setText("...");
            }
            if (balanceLabel != null) {
                balanceLabel.setText("Số dư: 0 VNĐ");
            }
            return;
        }

        if (userInfoLabel != null) {
            userInfoLabel.setText(currentUser.getUsername() + " | Role: " + (sellerMode ? "seller" : "bidder"));
        }
        if (balanceLabel != null) {
            balanceLabel.setText("Số dư: " + currencyFormat.format(currentUser.getBalance()) + " VNĐ");
        }
    }

    // 🔥 FIX 3: Chuyển vai trò an toàn (Bỏ Task)
    @FXML
    public void onRoleSwitcherClicked() {
        sellerMode = !sellerMode;

        String newRole = sellerMode ? "seller" : "bidder";
        ClientApp.setSelectedRole(newRole);
        if (currentUser != null) {
            currentUser.setRole(newRole);
            ClientApp.setCurrentUser(currentUser);
        }

        updateUIBasedOnRole();

        if (sellerMode) {
            switchToAddItemView();
        } else {
            switchToHomeView();
        }

        ClientApp.sendMessage(new Message("SWITCH_ROLE", newRole));
    }

    // ═══════════════════════════════════════════════════════════
    // SERVER LISTENER
    // ═══════════════════════════════════════════════════════════
    private void listenForServerUpdates() {
        ClientApp.setServerMessageHandler(this::handleServerMessage);
    }

    @SuppressWarnings("unchecked")
    private void handleServerMessage(Message message) {
        Platform.runLater(() -> {
            switch (message.getAction()) {
                case "GET_ALL_ITEMS_RESPONSE":
                    updateItemsFromServer((List<Item>) message.getPayload());
                    requestMyBidHistory();
                    break;

                case "BID_RESPONSE":
                    handleBidResponse(message.getPayload());
                    break;

                case "MY_BID_HISTORY_RESPONSE":
                    updateBidHistoryFromPayload(message.getPayload());
                    break;

                case "ITEM_BID_HISTORY_RESPONSE":
                case "ITEM_BID_HISTORY_UPDATE":
                    updateActiveBidChartFromHistoryPayload(message.getPayload());
                    break;

                case "AUCTION_CHAT_HISTORY":
                    updateAuctionRoomChatHistory(message.getPayload());
                    break;

                case "AUCTION_CHAT_MESSAGE":
                    appendAuctionRoomChatMessage(message.getPayload());
                    break;

                case "AUCTION_CHAT_ERROR":
                    showAuctionRoomChatError(message.getPayload());
                    break;

                case "BID_HISTORY_REFRESH_REQUIRED":
                    requestMyBidHistory();
                    break;

                case "AUCTION_RESULT_NOTIFICATION":
                    String notification = String.valueOf(message.getPayload());
                    notifications.add(notification);

                    Alert resultAlert = new Alert(Alert.AlertType.INFORMATION);
                    resultAlert.setTitle("Thông báo đấu giá");
                    resultAlert.setHeaderText("Kết quả đấu giá");
                    resultAlert.setContentText(notification);
                    resultAlert.showAndWait();

                    break;

                case "ITEM_UPDATE":
                    Item updatedItem = (Item) message.getPayload();

                    items.removeIf(item -> item.getId().equals(updatedItem.getId()));
                    items.add(updatedItem);
                    updateActiveBidDialogFromItemUpdate(updatedItem);

                    refreshCurrentViews();
                    break;

                case "NEW_ITEM_ADDED":
                    Item newItem = (Item) message.getPayload();
                    if (newItem != null) {
                        items.removeIf(item -> item.getId().equals(newItem.getId()));
                        items.add(newItem);
                        refreshCurrentViews();
                    }
                    break;

                case "ADD_ITEM_RESPONSE":
                case "START_AUCTION_RESPONSE":
                case "SWITCH_ROLE_RESPONSE":
                    System.out.println("Server: " + message.getPayload());
                    loadInitialItems();
                    break;

                case "TOP_UP_RESPONSE":
                    handleTopUpResponse(message.getPayload());
                    break;

                case "ACCOUNT_INFO_RESPONSE":
                    handleAccountInfoResponse(message.getPayload());
                    break;

                case "SYSTEM_NOTIFICATION":
                    System.out.println("Server: " + message.getPayload());
                    break;

                default:
                    System.out.println("Unknown server message: " + message.getAction());
                    break;
            }
        });
    }

    private void requestItemBidHistory(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }

        ClientApp.sendMessage(new Message("GET_ITEM_BID_HISTORY", itemId));
    }

    private void updateActiveBidChartFromHistoryPayload(Object payload) {
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
            System.err.println("Không đọc được một dòng lịch sử đặt giá sản phẩm: " + e.getMessage());
            return null;
        }
    }

    // 🔥 FIX 4: Gọi lịch sử an toàn (Bỏ Task)
    private void requestMyBidHistory() {
        if (currentUser == null) return;
        ClientApp.sendMessage(new Message("GET_MY_BID_HISTORY", currentUser.getId()));
    }

    private void updateBidHistoryFromPayload(Object payload) {
        bidHistory.clear();

        if (payload instanceof List<?>) {
            for (Object row : (List<?>) payload) {
                if (!(row instanceof Object[])) {
                    continue;
                }

                Object[] data = (Object[]) row;

                try {
                    String itemId = String.valueOf(data[0]);
                    String itemName = String.valueOf(data[1]);
                    String itemType = String.valueOf(data[2]);
                    double bidAmount = ((Number) data[3]).doubleValue();
                    java.time.LocalDateTime bidTime = (java.time.LocalDateTime) data[4];
                    String auctionStatus = String.valueOf(data[5]);
                    String result = String.valueOf(data[6]);

                    bidHistory.add(new BidHistoryController.BidHistoryRecord(
                            itemId,
                            itemName,
                            itemType,
                            bidAmount,
                            bidTime,
                            auctionStatus,
                            result
                    ));
                } catch (Exception e) {
                    System.err.println("Không đọc được một dòng lịch sử đấu giá: " + e.getMessage());
                }
            }
        }

        if (bidHistoryViewPaneController != null) {
            bidHistoryViewPaneController.updateData(bidHistory);
        }
    }

    private void handleAccountInfoResponse(Object payload) {
        if (payload instanceof User) {
            User updatedUser = (User) payload;

            if (currentUser != null && currentUser.getId() != null
                    && updatedUser.getId() != null
                    && !currentUser.getId().equals(updatedUser.getId())) {
                return;
            }

            currentUser = updatedUser;
            ClientApp.setCurrentUser(currentUser);
            updateUserInfoLabel();

            if (accountViewPaneController != null) {
                accountViewPaneController.updateUser(currentUser);
            }
        }
    }

    private void handleTopUpResponse(Object payload) {
        if (!(payload instanceof Object[])) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Nạp tiền");
            alert.setHeaderText("Phản hồi từ server");
            alert.setContentText(String.valueOf(payload));
            alert.showAndWait();
            return;
        }

        Object[] data = (Object[]) payload;
        boolean success = Boolean.TRUE.equals(data[0]);
        String message = String.valueOf(data[1]);

        if (success && data.length > 2 && data[2] instanceof User) {
            currentUser = (User) data[2];
            ClientApp.setCurrentUser(currentUser);
            updateUserInfoLabel();

            if (accountViewPaneController != null) {
                accountViewPaneController.updateUser(currentUser);
            }
        }

        Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        alert.setTitle("Nạp tiền");
        alert.setHeaderText(success ? "Nạp tiền thành công" : "Nạp tiền thất bại");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void onCurrentUserUpdated(User updatedUser) {
        if (updatedUser == null) {
            return;
        }

        currentUser = updatedUser;
        ClientApp.setCurrentUser(updatedUser);
        updateUserInfoLabel();

        if (accountViewPaneController != null) {
            accountViewPaneController.updateUser(updatedUser);
        }
    }

    private void updateItemsFromServer(List<Item> fetchedItems) {
        items.clear();

        if (fetchedItems != null) {
            items.addAll(fetchedItems);
        }

        refreshCurrentViews();
    }

    private void refreshCurrentViews() {
        if (homeView != null && homeView.isVisible()) {
            applyHomeFiltersAndSort();
        }

        if (myItemsViewPane != null && myItemsViewPane.isVisible() && myItemsViewPaneController != null) {
            myItemsViewPaneController.updateData(items);
        }

        if (salesHistoryViewPane != null && salesHistoryViewPane.isVisible() && salesHistoryViewPaneController != null) {
            salesHistoryViewPaneController.updateData(items);
        }

        if (auctionRoomView != null && auctionRoomView.isVisible() && activeBidDialogItem != null) {
            refreshAuctionRoomHeader();
            refreshActiveBidDialogLabels();
            validateActiveBidAmount();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ĐĂNG XUẤT
    // ═══════════════════════════════════════════════════════════
    @FXML
    public void onLogoutClicked() {
        clearAuctionRoomState();
        ClientApp.setCurrentUser(null);
        ClientApp.closeConnection();

        try {
            ClientApp.switchToLogin();
        } catch (Exception e) {
            System.err.println("Error switching to login: " + e.getMessage());
        }
    }
}
