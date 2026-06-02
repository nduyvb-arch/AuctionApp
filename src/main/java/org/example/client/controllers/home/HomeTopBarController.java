package org.example.client.controllers.home;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.common.model.user.User;

import java.text.NumberFormat;
import java.util.Locale;

public class HomeTopBarController {

    @FXML private Label pageTitle;
    @FXML private Label userInfoLabel;
    @FXML private Label balanceLabel;

    private static final NumberFormat currencyFormat = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

    public void setPageTitle(String title) {
        if (pageTitle != null) {
            pageTitle.setText(title);
        }
    }

    public void updateUserInfo(User currentUser, boolean sellerMode) {
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
}
