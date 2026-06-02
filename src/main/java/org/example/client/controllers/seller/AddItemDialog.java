package org.example.client.controllers.seller;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import org.example.common.Message;
import org.example.common.model.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.Optional;

public class AddItemDialog extends Dialog<Boolean> {

    private User currentUser;
    private ObjectOutputStream out;
    private byte[] selectedImageBytes; // Thêm trường để lưu trữ bytes của ảnh
    private Label imageFileNameLabel; // Label để hiển thị tên file ảnh
    private static final Logger logger = LoggerFactory.getLogger(AddItemDialog.class);


    public AddItemDialog(User currentUser, ObjectOutputStream out) {
        this.currentUser = currentUser;
        this.out = out;

        initializeDialog();
    }

    private void initializeDialog() {
        this.setTitle("Đăng sản phẩm mới");
        this.setHeaderText("Tạo một phiên đấu giá mới cho sản phẩm của bạn");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f8fafc;");

        // ===== ERROR MESSAGE LABEL (Phải khai báo trước khi sử dụng ở các nút) =====
        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.web("#dc2626"));
        errorLabel.setStyle("-fx-font-size: 11;");

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

        // ===== CHỌN ẢNH SẢN PHẨM =====
        Label imageLabel = new Label("Ảnh sản phẩm:");
        imageLabel.setFont(new Font("System Bold", 12));
        Button selectImageButton = new Button("Chọn ảnh...");
        imageFileNameLabel = new Label("Chưa chọn tệp nào");
        imageFileNameLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");

        selectImageButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Chọn ảnh sản phẩm");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File selectedFile = fileChooser.showOpenDialog(this.getDialogPane().getScene().getWindow());
            if (selectedFile != null) {
                try {
                    byte[] tempBytes = Files.readAllBytes(selectedFile.toPath());
                    // Kiểm tra dung lượng ảnh tối đa 5MB
                    if (tempBytes.length > 5 * 1024 * 1024) {
                        errorLabel.setText("❌ Kích thước ảnh quá lớn (tối đa 5MB).");
                        selectedImageBytes = null;
                        imageFileNameLabel.setText("Chưa chọn tệp nào");
                        return;
                    }

                    selectedImageBytes = tempBytes;
                    imageFileNameLabel.setText(selectedFile.getName());
                    errorLabel.setText(""); // Xóa lỗi nếu có
                } catch (IOException ex) {
                    errorLabel.setText("❌ Lỗi đọc tệp ảnh: " + ex.getMessage());
                    selectedImageBytes = null;
                    imageFileNameLabel.setText("Chưa chọn tệp nào");
                }
            }
        });
        HBox imageSelectionBox = new HBox(10, selectImageButton, imageFileNameLabel);
        imageSelectionBox.setPadding(new Insets(5, 0, 5, 0));
        VBox imageBox = new VBox(5);
        imageBox.getChildren().addAll(imageLabel, imageSelectionBox);

        // ===== NÚT ĐĂNG SẢN PHẨM =====
        Button submitButton = new Button("✅ Đăng sản phẩm");
        submitButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 10 20; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        submitButton.setPrefWidth(Double.MAX_VALUE);

        submitButton.setOnAction(e -> {
            if (nameTextField.getText().trim().isEmpty()) { errorLabel.setText("❌ Vui lòng nhập tên sản phẩm"); return; }
            if (descTextArea.getText().trim().isEmpty()) { errorLabel.setText("❌ Vui lòng nhập mô tả sản phẩm"); return; }
            if (selectedImageBytes == null || selectedImageBytes.length == 0) { errorLabel.setText("❌ Vui lòng chọn ảnh sản phẩm"); return; }

            try {
                double startPrice = Double.parseDouble(startPriceTextField.getText());
                double increment = Double.parseDouble(incrementTextField.getText());
                int duration = durationSpinner.getValue();

                if (startPrice <= 0 || increment <= 0 || duration <= 0) {
                    errorLabel.setText("❌ Giá và thời gian phải lớn hơn 0");
                    return;
                }

                if (out != null && currentUser != null) {
                    Object[] itemData = {
                            typeComboBox.getValue(),
                            nameTextField.getText().trim(),
                            descTextArea.getText().trim(),
                            startPrice,
                            increment,
                            String.valueOf(currentUser.getId()),
                            duration,
                            selectedImageBytes // Thêm bytes của ảnh vào payload
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

        ScrollPane scrollPane = new ScrollPane(new VBox(15,
                typeBox,
                nameBox,
                descBox,
                startPriceBox,
                incrementBox,
                durationBox,
                imageBox, // Thêm hộp chọn ảnh vào đây
                new Separator(),
                errorLabel,
                submitButton
        ));
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f8fafc;");

        content.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);

        this.getDialogPane().setContent(content);
        this.getDialogPane().setPrefWidth(500);
        this.getDialogPane().setPrefHeight(700);

        ButtonType cancelButtonType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        this.getDialogPane().getButtonTypes().add(cancelButtonType);
    }

    public static void showAddItemDialog(User currentUser, ObjectOutputStream out) {
        AddItemDialog dialog = new AddItemDialog(currentUser, out);
        Optional<Boolean> result = dialog.showAndWait();
        result.ifPresent(success -> {
            if (success) {
                logger.info("Đăng sản phẩm thành công");
            }
        });
    }
}