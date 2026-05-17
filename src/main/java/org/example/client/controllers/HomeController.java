package org.example.client.controllers;

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
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.client.ClientApp;
import org.example.common.Message;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.util.Duration;

/**
 * Controller chính của HomeMenu.fxml.
 *
 * Đã bỏ Watchlist:
 * - Trang chủ hiển thị toàn bộ sản phẩm.
 * - Card sản phẩm chỉ hiện: tên sản phẩm, giá cao nhất hiện tại, số lần đặt giá.
 * - Bấm vào card sẽ mở popup chi tiết, có thông tin sản phẩm, khung ảnh và nút đặt giá.
 */
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

    // ═══════════════════════════════════════════════════════════
    // HOME VIEW COMPONENTS
    // ═══════════════════════════════════════════════════════════
    @FXML private VBox homeView;
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

    @FXML private VBox contentContainer;

    // ═══════════════════════════════════════════════════════════
    // SHARED DATA
    // ═══════════════════════════════════════════════════════════
    private final List<Item> items = new ArrayList<>();
    private final List<BidHistoryController.BidHistoryRecord> bidHistory = new ArrayList<>();

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User currentUser;
    private boolean sellerMode;

    private static final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    // ═══════════════════════════════════════════════════════════
    // INITIALIZE
    // ═══════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentUser = ClientApp.getCurrentUser();

        if (currentUser != null) {
            sellerMode = "seller".equalsIgnoreCase(currentUser.getRole());
            updateUserInfoLabel();
            updateUIBasedOnRole();
        }

        out = ClientApp.getOutputStream();
        in = ClientApp.getInputStream();

        setupHomeViewFilters();

        setViewState(homeView, true);
        setViewState(bidHistoryViewPane, false);
        setViewState(addItemViewPane, false);
        setViewState(myItemsViewPane, false);
        setViewState(salesHistoryViewPane, false);

        if (bidHistoryViewPaneController != null) {
            bidHistoryViewPaneController.setup(bidHistory);
        }

        if (addItemViewPaneController != null) {
            addItemViewPaneController.setup(out, currentUser, this::loadInitialItems);
        }

        if (myItemsViewPaneController != null) {
            myItemsViewPaneController.setup(items, out, currentUser, this::loadInitialItems);
        }

        if (salesHistoryViewPaneController != null) {
            salesHistoryViewPaneController.setup(items, currentUser, this::loadInitialItems);
        }

        listenForServerUpdates();
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
    private void switchToBidHistoryView() {
        showView(bidHistoryViewPane);
        pageTitle.setText("Lịch sử đấu giá");

        if (bidHistoryViewPaneController != null) {
            bidHistoryViewPaneController.refreshBidHistoryView();
        }
    }

    @FXML
    private void switchToAddItemView() {
        showView(addItemViewPane);
        pageTitle.setText("➕ Đăng sản phẩm mới");
    }

    @FXML
    private void switchToMyItemsView() {
        showView(myItemsViewPane);
        pageTitle.setText("Sản phẩm của tôi");

        if (myItemsViewPaneController != null) {
            myItemsViewPaneController.updateData(items);
        }
    }

    @FXML
    private void switchToSalesHistoryView() {
        showView(salesHistoryViewPane);
        pageTitle.setText("Lịch sử bán hàng");

        if (salesHistoryViewPaneController != null) {
            salesHistoryViewPaneController.updateData(items);
        }
    }

    @FXML
    private void switchToAccountView() {
        pageTitle.setText("Tài khoản");
        hideAllViews();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Tài khoản");
        alert.setHeaderText("Chức năng này đang được phát triển");
        alert.setContentText("Hiện tại chưa có giao diện tài khoản riêng.");
        alert.showAndWait();
    }

    @FXML
    private void switchToNotificationsView() {
        pageTitle.setText("Thông báo");
        hideAllViews();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText("Chức năng này đang được phát triển");
        alert.setContentText("Hiện tại chưa có giao diện thông báo riêng.");
        alert.showAndWait();
    }

    private void showView(VBox view) {
        hideAllViews();
        setViewState(view, true);
    }

    private void hideAllViews() {
        setViewState(homeView, false);
        setViewState(bidHistoryViewPane, false);
        setViewState(addItemViewPane, false);
        setViewState(myItemsViewPane, false);
        setViewState(salesHistoryViewPane, false);
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

    private void loadInitialItems() {
        if (out == null) {
            return;
        }

        Task<Void> task = new Task<>() {
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
    // CARD SẢN PHẨM
    // ═══════════════════════════════════════════════════════════
    private Node createItemCard(Item item) {
        VBox card = new VBox(12);
        card.setPrefSize(250, 175);
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

        Label bidCountLabel = new Label("Số lần đặt giá: " + getEstimatedBidCount(item));
        bidCountLabel.setStyle(
                "-fx-text-fill: #475569;" +
                        "-fx-font-size: 12;" +
                        "-fx-background-color: #f1f5f9;" +
                        "-fx-padding: 6 10;" +
                        "-fx-background-radius: 999;"
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
                bidCountLabel,
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

    // ═══════════════════════════════════════════════════════════
    // POPUP CHI TIẾT SẢN PHẨM
    // ═══════════════════════════════════════════════════════════
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
            try {
                ImageView imageView = new ImageView(new Image(item.getImagePath(), true));
                imageView.setFitWidth(210);
                imageView.setFitHeight(210);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                imageBox.getChildren().add(imageView);
            } catch (Exception e) {
                imageBox.getChildren().add(createImagePlaceholder("Không tải được\nảnh sản phẩm"));
            }
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

        Label bidCountLabel = createDetailLine(
                "Số lần đặt giá",
                String.valueOf(getEstimatedBidCount(item))
        );

        Label endTimeLabel = createDetailLine(
                "Thời gian kết thúc",
                item.getEndTime() == null ? "Chưa thiết lập" : item.getEndTime().toString()
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

        Button bidButton = new Button("Đặt giá ngay");
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
            openBidDialog(item);
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
                bidCountLabel,
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

    // ═══════════════════════════════════════════════════════════
    // ĐẶT GIÁ
    // ═══════════════════════════════════════════════════════════
    private void openBidDialog(Item item) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Đặt giá - " + item.getItemName());
        dialog.setHeaderText("Nhập mức giá bạn muốn đặt");

        TextField amountField = new TextField();
        amountField.setPromptText("Nhập giá tiền...");

        double minBid = item.getCurrentPrice() + item.getBidIncrement();

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(
                new Label("Giá hiện tại: " + currencyFormat.format(item.getCurrentPrice()) + " VNĐ"),
                new Label("Mức tăng tối thiểu: " + currencyFormat.format(item.getBidIncrement()) + " VNĐ"),
                new Label("Giá tối thiểu cần đặt: " + currencyFormat.format(minBid) + " VNĐ"),
                new Label("Nhập giá của bạn:"),
                amountField
        );

        ButtonType submitButton = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButton, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == submitButton) {
                try {
                    String raw = amountField.getText()
                            .trim()
                            .replace(".", "")
                            .replace(",", "");

                    return Double.parseDouble(raw);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        Optional<Double> result = dialog.showAndWait();

        result.ifPresent(amount -> {
            if (amount < minBid) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Giá không hợp lệ");
                alert.setHeaderText("Mức giá đặt chưa đủ");
                alert.setContentText("Bạn cần đặt tối thiểu " + currencyFormat.format(minBid) + " VNĐ.");
                alert.showAndWait();
                return;
            }

            submitBid(item.getId(), amount);
        });
    }

    private void submitBid(String itemId, double bidAmount) {
        if (out == null || currentUser == null) {
            return;
        }

        Task<Void> task = new Task<>() {
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
            return;
        }

        userInfoLabel.setText(currentUser.getUsername() + " | Role: " + (sellerMode ? "seller" : "bidder"));
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

        if (out == null) {
            return;
        }

        Task<Void> task = new Task<>() {
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
    // SERVER LISTENER
    // ═══════════════════════════════════════════════════════════
    private void listenForServerUpdates() {
        if (in == null) {
            return;
        }

        Task<Void> task = new Task<>() {
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

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @SuppressWarnings("unchecked")
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

                items.removeIf(item -> item.getId().equals(updatedItem.getId()));
                items.add(updatedItem);

                refreshCurrentViews();
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

            default:
                System.out.println("Unknown server message: " + message.getAction());
                break;
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
