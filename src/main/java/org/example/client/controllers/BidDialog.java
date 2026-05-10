package org.example.client.controllers;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.example.common.Message;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;

import java.io.ObjectOutputStream;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

public class BidDialog extends Dialog<Double> {

    private Item item;
    private User currentUser;
    private ObjectOutputStream out;
    private double minBidAmount;

    public BidDialog(Item item, User currentUser, ObjectOutputStream out) {
        this.item = item;
        this.currentUser = currentUser;
        this.out = out;
        this.minBidAmount = item.getCurrentPrice() + item.getBidIncrement();

        initializeDialog();
    }

    private void initializeDialog() {
        // Thiết lập tiêu đề
        this.setTitle("Đặt giá - " + item.getItemName());
        this.setHeaderText("Đặt giá mới cho sản phẩm");

        // Tạo content VBox
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f8fafc;");

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Thông tin sản phẩm
        Label itemNameLabel = new Label("Sản phẩm: " + item.getItemName());
        itemNameLabel.setFont(new Font("System Bold", 14));
        itemNameLabel.setTextFill(Color.web("#0f172a"));

        // Giá hiện tại
        HBox currentPriceBox = new HBox(15);
        Label currentPriceText = new Label("Giá hiện tại:");
        currentPriceText.setFont(new Font("System", 12));
        Label currentPriceValue = new Label(currencyFormat.format(item.getCurrentPrice()));
        currentPriceValue.setFont(new Font("System Bold", 16));
        currentPriceValue.setTextFill(Color.web("#2563eb"));
        currentPriceBox.getChildren().addAll(currentPriceText, currentPriceValue);

        // Bước giá tối thiểu
        HBox bidIncrementBox = new HBox(15);
        Label bidIncrementText = new Label("Bước giá tối thiểu:");
        bidIncrementText.setFont(new Font("System", 12));
        Label bidIncrementValue = new Label(currencyFormat.format(item.getBidIncrement()));
        bidIncrementValue.setFont(new Font("System Bold", 12));
        bidIncrementValue.setTextFill(Color.web("#64748b"));
        bidIncrementBox.getChildren().addAll(bidIncrementText, bidIncrementValue);

        // Giá tối thiểu cần đặt
        HBox minBidBox = new HBox(15);
        Label minBidText = new Label("Giá tối thiểu để đặt:");
        minBidText.setFont(new Font("System Bold", 12));
        minBidText.setTextFill(Color.web("#f59e0b"));
        Label minBidValue = new Label(currencyFormat.format(minBidAmount));
        minBidValue.setFont(new Font("System Bold", 14));
        minBidValue.setTextFill(Color.web("#f59e0b"));
        minBidBox.getChildren().addAll(minBidText, minBidValue);

        // Input field cho giá đặt
        Label inputLabel = new Label("Nhập giá đặt của bạn:");
        inputLabel.setFont(new Font("System Bold", 12));

        TextField bidTextField = new TextField();
        bidTextField.setPromptText("Nhập giá cần đặt (tối thiểu: " + currencyFormat.format(minBidAmount) + ")");
        bidTextField.setPrefHeight(40);
        bidTextField.setStyle("-fx-font-size: 12; -fx-padding: 10;");

        // Label thông báo lỗi
        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.web("#dc2626"));
        errorLabel.setStyle("-fx-font-size: 11;");

        // Nút đặt giá
        Button placeBidButton = new Button("💰 Đặt giá");
        placeBidButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 10 20; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        placeBidButton.setPrefWidth(Double.MAX_VALUE);

        placeBidButton.setOnAction(e -> {
            String input = bidTextField.getText().trim();
            try {
                double bidAmount = Double.parseDouble(input);

                if (bidAmount < minBidAmount) {
                    errorLabel.setText("❌ Giá đặt phải lớn hơn hoặc bằng " + currencyFormat.format(minBidAmount));
                    return;
                }

                // Gửi request đặt giá tới server
                if (currentUser != null && out != null) {
                    Object[] bidData = {item.getId(), bidAmount, currentUser.getId()};
                    out.writeObject(new Message("BID", bidData));
                    out.flush();

                    errorLabel.setText("✓ Đang gửi yêu cầu...");
                    errorLabel.setTextFill(Color.web("#10b981"));

                    this.setResult(bidAmount);
                    this.close();
                }
            } catch (NumberFormatException ex) {
                errorLabel.setText("❌ Vui lòng nhập một số hợp lệ");
            } catch (Exception ex) {
                errorLabel.setText("❌ Lỗi: " + ex.getMessage());
            }
        });

        // Add các component vào content
        content.getChildren().addAll(
                itemNameLabel,
                new Separator(),
                currentPriceBox,
                bidIncrementBox,
                minBidBox,
                new Separator(),
                inputLabel,
                bidTextField,
                errorLabel,
                placeBidButton
        );

        // Thiết lập dialog
        this.getDialogPane().setContent(content);
        this.getDialogPane().setPrefWidth(450);

        // Tạo nút Cancel
        ButtonType cancelButtonType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        this.getDialogPane().getButtonTypes().add(cancelButtonType);

        // Hide default buttons
        this.getDialogPane().getButtonTypes().remove(0);
    }

    public static void showBidDialog(Item item, User currentUser, ObjectOutputStream out) {
        BidDialog dialog = new BidDialog(item, currentUser, out);
        Optional<Double> result = dialog.showAndWait();
        result.ifPresent(bidAmount -> {
            System.out.println("Đặt giá " + bidAmount + " cho sản phẩm " + item.getItemName());
        });
    }
}