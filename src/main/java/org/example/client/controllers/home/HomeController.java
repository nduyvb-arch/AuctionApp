package org.example.client.controllers.home;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.client.ClientApp;
import org.example.client.controllers.account.AccountViewController;
import org.example.client.controllers.auction.AuctionRoomController;
import org.example.client.controllers.auction.BidHistoryController;
import org.example.client.controllers.seller.AddItemViewController;
import org.example.client.controllers.seller.MyItemsController;
import org.example.client.controllers.seller.SalesHistoryController;
import org.example.common.Message;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomeController implements Initializable {
    private static final Logger logger = Logger.getLogger(HomeController.class.getName());

    @FXML private VBox sidebarPane;
    @FXML private HomeSidebarController sidebarPaneController;

    @FXML private HBox topBarPane;
    @FXML private HomeTopBarController topBarPaneController;

    @FXML private VBox homeViewPane;
    @FXML private HomeListController homeViewPaneController;

    @FXML private VBox auctionRoomViewPane;
    @FXML private AuctionRoomController auctionRoomViewPaneController;

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

    private final List<Item> items = new ArrayList<>();
    private final List<BidHistoryController.BidHistoryRecord> bidHistory = new ArrayList<>();
    private final List<String> notifications = new ArrayList<>();

    private User currentUser;
    private boolean sellerMode;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentUser = ClientApp.getCurrentUser();
        sellerMode = ClientApp.isSellerSelected();

        hideAllViews();
        setupChildControllers();
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

    private void setupChildControllers() {
        if (sidebarPaneController != null) {
            sidebarPaneController.setup(
                    this::switchToHomeView,
                    this::switchToBidHistoryView,
                    this::switchToAddItemView,
                    this::switchToMyItemsView,
                    this::switchToSalesHistoryView,
                    this::switchToRoleSelectionView,
                    this::onLogoutClicked
            );
        }

        if (homeViewPaneController != null) {
            homeViewPaneController.setup(items, this::loadInitialItems, this::openAuctionRoom);
        }

        if (auctionRoomViewPaneController != null) {
            auctionRoomViewPaneController.setup(
                    currentUser,
                    items,
                    this::switchToHomeView,
                    this::submitBid,
                    this::requestItemBidHistory,
                    this::requestAuctionChatHistory,
                    this::sendAuctionRoomChatMessage
            );
        }

        if (bidHistoryViewPaneController != null) {
            bidHistoryViewPaneController.setup(bidHistory);
        }

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
    }

    private void switchToHomeView() {
        clearAuctionRoomState();
        showView(homeViewPane);
        setPageTitle("Trang chủ sàn đấu giá");
        loadInitialItems();
    }

    private void switchToBidHistoryView() {
        clearAuctionRoomState();
        showView(bidHistoryViewPane);
        setPageTitle("Lịch sử đấu giá");
        requestMyBidHistory();

        if (bidHistoryViewPaneController != null) {
            bidHistoryViewPaneController.refreshBidHistoryView();
        }
    }

    private void switchToAddItemView() {
        clearAuctionRoomState();
        showView(addItemViewPane);
        setPageTitle(" Đăng sản phẩm mới");
    }

    private void switchToMyItemsView() {
        clearAuctionRoomState();
        showView(myItemsViewPane);
        setPageTitle("Sản phẩm của tôi");

        if (myItemsViewPaneController != null) {
            myItemsViewPaneController.updateData(items);
        }
    }

    private void switchToSalesHistoryView() {
        clearAuctionRoomState();
        showView(salesHistoryViewPane);
        setPageTitle("Lịch sử bán hàng");

        if (salesHistoryViewPaneController != null) {
            salesHistoryViewPaneController.updateData(items);
        }
    }

    private void switchToAccountView() {
        clearAuctionRoomState();
        showView(accountViewPane);
        setPageTitle("Tài khoản");

        if (accountViewPaneController != null) {
            accountViewPaneController.updateUser(currentUser);
        }
    }

    @SuppressWarnings("unused")
    private void switchToNotificationsView() {
        setPageTitle("Thông báo");

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

    private void switchToRoleSelectionView() {
        try {
            ClientApp.switchToRoleSelection();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Không thể quay lại màn chọn vai trò", e);
            showError("Không thể quay lại màn chọn vai trò", e.getMessage());
        }
    }

    private void openAuctionRoom(Item item) {
        if (auctionRoomViewPaneController == null) {
            return;
        }

        auctionRoomViewPaneController.openAuctionRoom(item);
        showView(auctionRoomViewPane);
        setPageTitle("Phòng đấu giá - " + item.getItemName());
    }

    private void showView(VBox view) {
        hideAllViews();
        setViewState(view, true);
    }

    private void hideAllViews() {
        setViewState(homeViewPane, false);
        setViewState(auctionRoomViewPane, false);
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

    private void setPageTitle(String title) {
        if (topBarPaneController != null) {
            topBarPaneController.setPageTitle(title);
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content == null || content.isBlank() ? "Không rõ nguyên nhân." : content);
        alert.showAndWait();
    }

    private void loadInitialItems() {
        ClientApp.sendMessage(new Message("GET_ALL_ITEMS", null));
    }

    private void submitBid(String itemId, double bidAmount) {
        if (currentUser == null) {
            return;
        }
        Object[] bidData = {itemId, bidAmount, currentUser.getId()};
        ClientApp.sendMessage(new Message("BID", bidData));
    }

    private void updateUIBasedOnRole() {
        if (sidebarPaneController != null) {
            sidebarPaneController.updateRole(sellerMode);
        }
        updateUserInfoLabel();
    }

    private void updateUserInfoLabel() {
        if (topBarPaneController != null) {
            topBarPaneController.updateUserInfo(currentUser, sellerMode);
        }
    }

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
                    if (auctionRoomViewPaneController != null) {
                        auctionRoomViewPaneController.handleBidResponse(message.getPayload());
                    }
                    break;

                case "MY_BID_HISTORY_RESPONSE":
                    updateBidHistoryFromPayload(message.getPayload());
                    break;

                case "ITEM_BID_HISTORY_RESPONSE":
                case "ITEM_BID_HISTORY_UPDATE":
                    if (auctionRoomViewPaneController != null) {
                        auctionRoomViewPaneController.updateActiveBidChartFromHistoryPayload(message.getPayload());
                    }
                    break;

                case "AUCTION_CHAT_HISTORY":
                    if (auctionRoomViewPaneController != null) {
                        auctionRoomViewPaneController.updateAuctionRoomChatHistory(message.getPayload());
                    }
                    break;

                case "AUCTION_CHAT_MESSAGE":
                    if (auctionRoomViewPaneController != null) {
                        auctionRoomViewPaneController.appendAuctionRoomChatMessage(message.getPayload());
                    }
                    break;

                case "AUCTION_CHAT_ERROR":
                    if (auctionRoomViewPaneController != null) {
                        auctionRoomViewPaneController.showAuctionRoomChatError(message.getPayload());
                    }
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
                    if (auctionRoomViewPaneController != null) {
                        auctionRoomViewPaneController.updateActiveBidDialogFromItemUpdate(updatedItem);
                    }

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
                    logger.log(Level.INFO, "Server response: {0}", message.getPayload());
                    loadInitialItems();
                    break;

                case "SYSTEM_NOTIFICATION":
                    logger.log(Level.INFO, "System notification: {0}", message.getPayload());
                    break;

                default:
                    logger.log(Level.WARNING, "Unknown server message: {0}", message.getAction());
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

    private void requestAuctionChatHistory(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        ClientApp.sendMessage(new Message("JOIN_AUCTION_ROOM", itemId));
    }

    private void sendAuctionRoomChatMessage(String itemId, String text) {
        if (itemId == null || itemId.isBlank() || text == null || text.isBlank()) {
            return;
        }
        ClientApp.sendMessage(new Message("SEND_AUCTION_CHAT", new Object[]{itemId, text, ClientApp.getSelectedRole()}));
    }

    private void requestMyBidHistory() {
        if (currentUser == null) {
            return;
        }
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
                    logger.log(Level.SEVERE, "Không đọc được một dòng lịch sử đấu giá", e);
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
            if (auctionRoomViewPaneController != null) {
                auctionRoomViewPaneController.setCurrentUser(currentUser);
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
            if (auctionRoomViewPaneController != null) {
                auctionRoomViewPaneController.setCurrentUser(currentUser);
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
        if (auctionRoomViewPaneController != null) {
            auctionRoomViewPaneController.setCurrentUser(updatedUser);
        }
    }

    private void updateItemsFromServer(List<Item> fetchedItems) {
        items.clear();

        if (fetchedItems != null) {
            items.addAll(fetchedItems);
        }

        if (homeViewPaneController != null) {
            homeViewPaneController.updateData(items);
        }
        if (auctionRoomViewPaneController != null) {
            auctionRoomViewPaneController.updateItemsReference(items);
        }

        refreshCurrentViews();
    }

    private void refreshCurrentViews() {
        if (homeViewPane != null && homeViewPane.isVisible() && homeViewPaneController != null) {
            homeViewPaneController.refreshHomeView();
        }

        if (myItemsViewPane != null && myItemsViewPane.isVisible() && myItemsViewPaneController != null) {
            myItemsViewPaneController.updateData(items);
        }

        if (salesHistoryViewPane != null && salesHistoryViewPane.isVisible() && salesHistoryViewPaneController != null) {
            salesHistoryViewPaneController.updateData(items);
        }

        if (auctionRoomViewPaneController != null && auctionRoomViewPaneController.isActive()
                && auctionRoomViewPaneController.getActiveItem() != null) {
            auctionRoomViewPaneController.refreshAuctionRoom();
        }
    }

    private void clearAuctionRoomState() {
        if (auctionRoomViewPaneController != null) {
            auctionRoomViewPaneController.clearAuctionRoomState();
        }
    }

    @FXML
    public void onLogoutClicked() {
        clearAuctionRoomState();
        ClientApp.setCurrentUser(null);
        ClientApp.closeConnection();

        try {
            ClientApp.switchToLogin();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error switching to login", e);
        }
    }
}
