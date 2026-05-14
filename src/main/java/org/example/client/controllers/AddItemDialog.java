package org.example.client.controllers;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.example.common.Message;
import org.example.common.model.user.User;

import java.io.ObjectOutputStream;
import java.util.Optional;

public class AddItemDialog extends Dialog<Boolean> {

    private User currentUser;
    private ObjectOutputStream out;

    public AddItemDialog(User currentUser, ObjectOutputStream out) {
        this.currentUser = currentUser;
        this.out = out;

        initializeDialog();
    }

    private void initializeDialog() {
        // Thiết lập tiêu đề
        this.setTitle("Đăng sản phẩm mới");
        this.setHeaderText("Tạo một phiên đấu giá mới cho sản phẩm của bạn");

        // Tạo content VBox
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f8fafc;");

        // ===== LOẠI SẢN PHẨM =====
        Label typeLabel = new Label("Loại sản phẩm:");
        typeLabel.setFont(new Font("System Bold", 12));
        ComboBox<String> typeComboBox = new ComboBox<>();
        typeComboBox.getItems().addAll("Electronic", "Vehicle", "Art");
        typeComboBox.setValue("Electronic");
        typeComboBox.setStyle("-fx-font-size: 11; -fx-padding: 8;");

        VBox typeBox = new VBox(5);
        typeBox.getChildren().addAll(typeLabel, typeComboBox);

        // ===== TÊN SẢN PHẨM =====
        Label nameLabel = new Label("Tên sản phẩm:");
        nameLabel.setFont(new Font("System Bold", 12));
        TextField nameTextField = new TextField();
        nameTextField.setPromptText("Ví dụ: iPhone 15 Pro Max");
        nameTextField.setPrefHeight(35);
        nameTextField.setStyle("-fx-font-size: 11; -fx-padding: 8;");

        VBox nameBox = new VBox(5);
        nameBox.getChildren().addAll(nameLabel, nameTextField);

        // ===== MÔ TẢ =====
        Label descLabel = new Label("Mô tả chi tiết:");
        descLabel.setFont(new Font("System Bold", 12));
        TextArea descTextArea = new TextArea();
        descTextArea.setPromptText("Mô tả chi tiết về sản phẩm...");
        descTextArea.setPrefHeight(100);
        descTextArea.setWrapText(true);
        descTextArea.setStyle("-fx-font-size: 11; -fx-padding: 8;");

        VBox descBox = new VBox(5);
        descBox.getChildren().addAll(descLabel, descTextArea);

        // ===== GIÁ KHỞI ĐIỂM =====
        Label startPriceLabel = new Label("Giá khởi điểm (₫):");
        startPriceLabel.setFont(new Font("System Bold", 12));
        TextField startPriceTextField = new TextField();
        startPriceTextField.setPromptText("Ví dụ: 1000000");
        startPriceTextField.setPrefHeight(35);
        startPriceTextField.setStyle("-fx-font-size: 11; -fx-padding: 8;");

        VBox startPriceBox = new VBox(5);
        startPriceBox.getChildren().addAll(startPriceLabel, startPriceTextField);

        // ===== BƯỚC GIÁ TỐI THIỂU =====
        Label incrementLabel = new Label("Bước giá tối thiểu (₫):");
        incrementLabel.setFont(new Font("System Bold", 12));
        TextField incrementTextField = new TextField();
        incrementTextField.setPromptText("Ví dụ: 50000");
        incrementTextField.setPrefHeight(35);
        incrementTextField.setStyle("-fx-font-size: 11; -fx-padding: 8;");

        VBox incrementBox = new VBox(5);
        incrementBox.getChildren().addAll(incrementLabel, incrementTextField);

        // ===== THỜI GIAN ĐẤU GIÁ =====
        Label durationLabel = new Label("Thời gian đấu giá (phút):");
        durationLabel.setFont(new Font("System Bold", 12));
        Spinner<Integer> durationSpinner = new Spinner<>(1, 7 * 24 * 60, 60);
        durationSpinner.setPrefHeight(35);
        durationSpinner.setStyle("-fx-font-size: 11; -fx-padding: 8;");

        VBox durationBox = new VBox(5);
        durationBox.getChildren().addAll(durationLabel, durationSpinner);

        // ===== ERROR MESSAGE LABEL =====
        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.web("#dc2626"));
        errorLabel.setStyle("-fx-font-size: 11;");

        // ===== NÚT ĐĂNG SẢN PHẨM =====
        Button submitButton = new Button("✅ Đăng sản phẩm");
        submitButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 10 20; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        submitButton.setPrefWidth(Double.MAX_VALUE);

        submitButton.setOnAction(e -> {
            // Validate input
            if (nameTextField.getText().trim().isEmpty()) {
                errorLabel.setText("❌ Vui lòng nhập tên sản phẩm");
                return;
            }
            if (descTextArea.getText().trim().isEmpty()) {
                errorLabel.setText("❌ Vui lòng nhập mô tả sản phẩm");
                return;
            }

            try {
                double startPrice = Double.parseDouble(startPriceTextField.getText());
                double increment = Double.parseDouble(incrementTextField.getText());

                if (startPrice <= 0 || increment <= 0) {
                    errorLabel.setText("❌ Giá phải lớn hơn 0");
                    return;
                }

                // Gửi request tới server
                if (out != null && currentUser != null) {
                    Object[] itemData = {
                            typeComboBox.getValue(),
                            nameTextField.getText().trim(),
                            descTextArea.getText().trim(),
                            startPrice,
                            increment,
                            currentUser.getId()
                    };

                    out.writeObject(new Message("ADD_ITEM", itemData));
                    out.flush();

                    errorLabel.setText("✓ Đang đăng sản phẩm...");
                    errorLabel.setTextFill(Color.web("#10b981"));

                    this.setResult(true);
                    this.close();
                }
            } catch (NumberFormatException ex) {
                errorLabel.setText("❌ Vui lòng nhập giá hợp lệ");
            } catch (Exception ex) {
                errorLabel.setText("❌ Lỗi: " + ex.getMessage());
            }
        });

        // Add các component vào content
        ScrollPane scrollPane = new ScrollPane(new VBox(15,
                typeBox,
                nameBox,
                descBox,
                startPriceBox,
                incrementBox,
                durationBox,
                new Separator(),
                errorLabel,
                submitButton
        ));
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f8fafc;");

        content.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);

        // Thiết lập dialog
        this.getDialogPane().setContent(content);
        this.getDialogPane().setPrefWidth(500);
        this.getDialogPane().setPrefHeight(600);

        // Tạo nút Cancel
        ButtonType cancelButtonType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        this.getDialogPane().getButtonTypes().add(cancelButtonType);

        // Hide default buttons
        this.getDialogPane().getButtonTypes().remove(0);
    }

    public static void showAddItemDialog(User currentUser, ObjectOutputStream out) {
        AddItemDialog dialog = new AddItemDialog(currentUser, out);
        Optional<Boolean> result = dialog.showAndWait();
        result.ifPresent(success -> {
            if (success) {
                System.out.println("Sản phẩm đã được đăng thành công");
            }
        });
    }
}
