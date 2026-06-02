package org.example.client.controllers.seller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.example.client.ClientApp; // ADDED
import org.example.common.Message;
import org.example.common.model.user.User;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.ResourceBundle;

public class AddItemViewController implements Initializable {

    @FXML private ComboBox<String> itemTypeComboBox;
    @FXML private TextField itemNameTextField;
    @FXML private TextArea itemDescriptionTextArea;
    @FXML private TextField startPriceTextField;
    @FXML private TextField bidIncrementTextField;
    @FXML private Button chooseImageButton;
    @FXML private Label selectedImageLabel;
    @FXML private ImageView imagePreview;

    @FXML private Label messageLabel;
    @FXML private Button submitItemButton;
    @FXML private Button clearFormButton;

    private User currentUser;
    private Runnable onItemCreated;

    private String selectedImagePath;
    private File selectedImageFile;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        itemTypeComboBox.getItems().setAll("Electronic", "Vehicle", "Art");
        itemTypeComboBox.setValue("Electronic");


        selectedImageLabel.setText("Chưa chọn ảnh");
    }

    // 🔥 XÓA THAM SỐ out
    public void setup(User currentUser, Runnable onItemCreated) {
        this.currentUser = currentUser;
        this.onItemCreated = onItemCreated;
    }

    @FXML
    private void onChooseImageClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Ảnh sản phẩm", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Tất cả file", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(chooseImageButton.getScene().getWindow());

        if (selectedFile == null) return;

        selectedImageFile = selectedFile;
        selectedImagePath = selectedFile.toURI().toString();
        selectedImageLabel.setText(selectedFile.getName());

        try {
            Image image = new Image(selectedImagePath, true);
            imagePreview.setImage(image);
        } catch (Exception e) {
            showMessage("Không thể đọc ảnh đã chọn.", false);
            selectedImageFile = null;
        }
    }

    @FXML
    private void onSubmitItemClicked() {
        if (currentUser == null) {
            showMessage("Chưa đăng nhập.", false);
            return;
        }

        String name = itemNameTextField.getText().trim();
        String description = itemDescriptionTextArea.getText().trim();

        if (name.isEmpty() || description.isEmpty()) {
            showMessage("Vui lòng nhập đủ tên và mô tả sản phẩm.", false);
            return;
        }

        try {
            double startPrice = Double.parseDouble(startPriceTextField.getText().trim());
            double bidIncrement = Double.parseDouble(bidIncrementTextField.getText().trim());
            if (startPrice <= 0 || bidIncrement <= 0) {
                showMessage("Giá khởi điểm và bước giá tối thiểu phải lớn hơn 0.", false);
                return;
            }

            byte[] imageBytes = null;
            if (selectedImageFile != null) {
                try {
                    imageBytes = Files.readAllBytes(selectedImageFile.toPath());
                    if (imageBytes.length > 5 * 1024 * 1024) {
                        showMessage("Kích thước ảnh quá lớn (tối đa 5MB).", false);
                        return;
                    }
                } catch (Exception e) {
                    showMessage("Lỗi khi đọc file ảnh.", false);
                    return;
                }
            }

            Object[] itemData = new Object[]{
                    itemTypeComboBox.getValue(), name, description, startPrice,
                    bidIncrement, currentUser.getId(), imageBytes
            };

            // 🔥 DÙNG CHUNG ĐƯỜNG ỐNG
            ClientApp.sendMessage(new Message("ADD_ITEM", itemData));

            showMessage("Đã gửi yêu cầu đăng sản phẩm. Danh sách sẽ tự làm mới.", true);
            clearForm();

            if (onItemCreated != null) onItemCreated.run();

        } catch (NumberFormatException e) {
            showMessage("Giá khởi điểm và bước giá phải là số hợp lệ.", false);
        }
    }

    @FXML
    private void onClearFormClicked() {
        clearForm();
        showMessage("", true);
    }

    private void clearForm() {
        itemTypeComboBox.setValue("Electronic");
        itemNameTextField.clear();
        itemDescriptionTextArea.clear();
        startPriceTextField.clear();
        bidIncrementTextField.clear();
        selectedImagePath = null;
        selectedImageFile = null;
        selectedImageLabel.setText("Chưa chọn ảnh");
        imagePreview.setImage(null);
    }

    private void showMessage(String message, boolean success) {
        messageLabel.setText(message);
        messageLabel.setTextFill(Color.web(success ? "#10b981" : "#dc2626"));
    }
}