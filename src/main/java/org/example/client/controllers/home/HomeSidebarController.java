package org.example.client.controllers.home;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class HomeSidebarController {

    @FXML private Label currentRoleLabel;
    @FXML private Button roleSwitcherButton;
    @FXML private Button bidHistoryMenuItem;
    @FXML private Button addItemMenuItem;
    @FXML private Button myItemsMenuItem;
    @FXML private Button salesHistoryMenuItem;
    @FXML private Label bidderMenuLabel;
    @FXML private Label sellerMenuLabel;

    private Runnable onHome;
    private Runnable onBidHistory;
    private Runnable onAddItem;
    private Runnable onMyItems;
    private Runnable onSalesHistory;
    private Runnable onRoleSelection;
    private Runnable onLogout;

    public void setup(
            Runnable onHome,
            Runnable onBidHistory,
            Runnable onAddItem,
            Runnable onMyItems,
            Runnable onSalesHistory,
            Runnable onRoleSelection,
            Runnable onLogout
    ) {
        this.onHome = onHome;
        this.onBidHistory = onBidHistory;
        this.onAddItem = onAddItem;
        this.onMyItems = onMyItems;
        this.onSalesHistory = onSalesHistory;
        this.onRoleSelection = onRoleSelection;
        this.onLogout = onLogout;
    }

    public void updateRole(boolean sellerMode) {
        boolean isSeller = sellerMode;
        boolean isBidder = !sellerMode;

        setVisibleManaged(bidderMenuLabel, isBidder);
        setVisibleManaged(bidHistoryMenuItem, isBidder);
        setVisibleManaged(sellerMenuLabel, isSeller);
        setVisibleManaged(addItemMenuItem, isSeller);
        setVisibleManaged(myItemsMenuItem, isSeller);
        setVisibleManaged(salesHistoryMenuItem, isSeller);

        if (currentRoleLabel != null) {
            currentRoleLabel.setText(isSeller ? "Người bán" : "Người đấu giá");
        }
        if (roleSwitcherButton != null) {
            roleSwitcherButton.setText(isSeller ? "Chuyển sang Người đấu giá" : "Chuyển sang Người bán");
        }
    }

    private void setVisibleManaged(javafx.scene.Node node, boolean active) {
        if (node == null) {
            return;
        }
        node.setVisible(active);
        node.setManaged(active);
    }

    @FXML private void switchToHomeView() { run(onHome); }
    @FXML private void switchToBidHistoryView() { run(onBidHistory); }
    @FXML private void switchToAddItemView() { run(onAddItem); }
    @FXML private void switchToMyItemsView() { run(onMyItems); }
    @FXML private void switchToSalesHistoryView() { run(onSalesHistory); }
    @FXML private void switchToRoleSelectionView() { run(onRoleSelection); }
    @FXML private void onLogoutClicked() { run(onLogout); }

    private void run(Runnable action) {
        if (action != null) {
            action.run();
        }
    }
}
