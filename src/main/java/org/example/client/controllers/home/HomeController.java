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

    private User currentUser;
    private boolean sellerMode;

    private enum ActiveView {
        NONE, HOME, AUCTION_ROOM, BID_HISTORY, ADD_ITEM, MY_ITEMS, SALES_HISTORY, ACCOUNT
    }

    private ActiveView activeView = ActiveView.NONE;
    private boolean itemsLoadedOnce;
    private boolean itemsRequestInProgress;
    private boolean bidHistoryLoadedOnce;
    private boolean bidHistoryRequestInProgress;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentUser = ClientApp.getCurrentUser();
        sellerMode = ClientApp.isSellerSelected();

        hideAllViews();
        setupChildControllers();
        updateUIBasedOnRole();
        listenForServerUpdates();

        if (sellerMode) {
            switchToAddItemView();
        } else {
            switchToHomeView();
        }

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
            homeViewPaneController.setup(items, this::reloadItemsFromServer, this::openAuctionRoom);
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
            addItemViewPaneController.setup(currentUser, this::reloadItemsFromServer);
        }

        if (myItemsViewPaneController != null) {
            myItemsViewPaneController.setup(items, currentUser, this::reloadItemsFromServer);
        }

        if (salesHistoryViewPaneController != null) {
            salesHistoryViewPaneController.setup(items, currentUser, this::reloadItemsFromServer);
        }

        if (accountViewPaneController != null) {
            accountViewPaneController.setup(currentUser, this::onCurrentUserUpdated);
        }
    }

    private void switchToHomeView() {
        boolean changed = showView(homeViewPane, ActiveView.HOME);
        setPageTitle("Trang chủ sàn đấu giá");

        if (!itemsLoadedOnce) {
            loadInitialItems();
        } else if (changed && homeViewPaneController != null) {
            homeViewPaneController.refreshHomeView();
        }
    }

    private void switchToBidHistoryView() {
        boolean changed = showView(bidHistoryViewPane, ActiveView.BID_HISTORY);
        setPageTitle("Lịch sử đấu giá");
        requestMyBidHistoryIfNeeded();

        if (changed && bidHistoryViewPaneController != null) {
            bidHistoryViewPaneController.refreshBidHistoryView();
        }
    }

    private void switchToAddItemView() {
        showView(addItemViewPane, ActiveView.ADD_ITEM);
        setPageTitle(" Đăng sản phẩm mới");
    }

    private void switchToMyItemsView() {
        boolean changed = showView(myItemsViewPane, ActiveView.MY_ITEMS);
        setPageTitle("Sản phẩm của tôi");

        if (!itemsLoadedOnce) {
            loadInitialItems();
        } else if (changed && myItemsViewPaneController != null) {
            myItemsViewPaneController.updateData(items);
        }
    }

    private void switchToSalesHistoryView() {
        boolean changed = showView(salesHistoryViewPane, ActiveView.SALES_HISTORY);
        setPageTitle("Lịch sử bán hàng");

        if (!itemsLoadedOnce) {
            loadInitialItems();
        } else if (changed && salesHistoryViewPaneController != null) {
            salesHistoryViewPaneController.updateData(items);
        }
    }

    private void switchToAccountView() {
        boolean changed = showView(accountViewPane, ActiveView.ACCOUNT);
        setPageTitle("Tài khoản");

        if (changed && accountViewPaneController != null) {
            accountViewPaneController.updateUser(currentUser);
        }
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

        showView(auctionRoomViewPane, ActiveView.AUCTION_ROOM);
        auctionRoomViewPaneController.openAuctionRoom(item);
        setPageTitle("Phòng đấu giá - " + item.getItemName());
    }

    private boolean showView(VBox view, ActiveView targetView) {
        if (view == null) {
            return false;
        }

        if (activeView == targetView && view.isVisible()) {
            return false;
        }

        clearAuctionRoomStateIfLeaving(targetView);
        hideAllViews();
        setViewState(view, true);
        activeView = targetView;
        return true;
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
        requestItemsFromServer(false);
    }

    private void reloadItemsFromServer() {
        requestItemsFromServer(true);
    }

    private void requestItemsFromServer(boolean forceReload) {
        if (itemsRequestInProgress) {
            return;
        }

        if (!forceReload && itemsLoadedOnce) {
            return;
        }

        itemsRequestInProgress = true;
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

    private void listenForServerUpdates() {
        ClientApp.setServerMessageHandler(this::handleServerMessage);
    }

    @SuppressWarnings("unchecked")
    private void handleServerMessage(Message message) {
        Platform.runLater(() -> {
            switch (message.getAction()) {
                case "GET_ALL_ITEMS_RESPONSE":
                    itemsRequestInProgress = false;
                    itemsLoadedOnce = true;
                    updateItemsFromServer((List<Item>) message.getPayload());
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
                    requestMyBidHistory(true);
                    break;

                case "AUCTION_RESULT_NOTIFICATION":
                    String notification = String.valueOf(message.getPayload());
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
                    reloadItemsFromServer();
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

    private void requestMyBidHistoryIfNeeded() {
        requestMyBidHistory(false);
    }

    private void requestMyBidHistory(boolean forceReload) {
        if (currentUser == null || bidHistoryRequestInProgress) {
            return;
        }

        if (!forceReload && bidHistoryLoadedOnce) {
            return;
        }

        bidHistoryRequestInProgress = true;
        ClientApp.sendMessage(new Message("GET_MY_BID_HISTORY", currentUser.getId()));
    }

    private void updateBidHistoryFromPayload(Object payload) {
        bidHistoryRequestInProgress = false;
        bidHistoryLoadedOnce = true;
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

    private void clearAuctionRoomStateIfLeaving(ActiveView targetView) {
        if (activeView == ActiveView.AUCTION_ROOM && targetView != ActiveView.AUCTION_ROOM) {
            clearAuctionRoomState();
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
