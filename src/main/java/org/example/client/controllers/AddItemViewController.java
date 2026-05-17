package org.example.client.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.example.common.Message;
import org.example.common.model.user.User;

import java.io.File;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ResourceBundle;

public class AddItemViewController implements Initializable {

    @FXML private ComboBox<String> itemTypeComboBox;
    @FXML private TextField itemNameTextField;
    @FXML private TextArea itemDescriptionTextArea;
    @FXML private TextField startPriceTextField;
    @FXML private TextField bidIncrementTextField;
    @FXML private Spinner<Integer> durationSpinner;

    @FXML private Button chooseImageButton;
    @FXML private Label selectedImageLabel;
    @FXML private ImageView imagePreview;

    @FXML private Label messageLabel;
    @FXML private Button submitItemButton;
    @FXML private Button clearFormButton;

    private ObjectOutputStream out;
    private User currentUser;
    private Runnable onItemCreated;

    private String selectedImagePath;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        itemTypeComboBox.getItems().setAll("Electronic", "Vehicle", "Art");
        itemTypeComboBox.setValue("Electronic");

        durationSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10080, 60));
        durationSpinner.setEditable(true);

        selectedImageLabel.setText("Chưa chọn ảnh");
    }

    public void setup(ObjectOutputStream out, User currentUser, Runnable onItemCreated) {
        this.out = out;
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

        if (selectedFile == null) {
            return;
        }

        selectedImagePath = selectedFile.toURI().toString();
        selectedImageLabel.setText(selectedFile.getName());

        try {
            Image image = new Image(selectedImagePath, true);
            imagePreview.setImage(image);
        } catch (Exception e) {
            showMessage("❌ Không thể đọc ảnh đã chọn.", false);
        }
    }

    @FXML
    private void onSubmitItemClicked() {
        if (out == null || currentUser == null) {
            showMessage("❌ Chưa có kết nối hoặc chưa đăng nhập.", false);
            return;
        }

        String name = itemNameTextField.getText().trim();
        String description = itemDescriptionTextArea.getText().trim();

        if (name.isEmpty()) {
            showMessage("❌ Vui lòng nhập tên sản phẩm.", false);
            return;
        }

        if (description.isEmpty()) {
            showMessage("❌ Vui lòng nhập mô tả sản phẩm.", false);
            return;
        }

        try {
            double startPrice = Double.parseDouble(startPriceTextField.getText().trim());
            double bidIncrement = Double.parseDouble(bidIncrementTextField.getText().trim());
            int duration = durationSpinner.getValue();

            if (startPrice <= 0 || bidIncrement <= 0 || duration <= 0) {
                showMessage("❌ Giá và thời gian đấu giá phải lớn hơn 0.", false);
                return;
            }

            /*
             * Payload cũ:
             * [0] type, [1] name, [2] description, [3] startPrice,
             * [4] bidIncrement, [5] sellerId, [6] duration
             *
             * Payload mới thêm:
             * [7] imagePath
             */
            Object[] itemData = new Object[]{
                    itemTypeComboBox.getValue(),
                    name,
                    description,
                    startPrice,
                    bidIncrement,
                    currentUser.getId(),
                    duration,
                    selectedImagePath
            };

            synchronized (out) {
                out.writeObject(new Message("ADD_ITEM", itemData));
                out.flush();
            }

            showMessage("✅ Đã gửi yêu cầu đăng sản phẩm. Danh sách sẽ tự làm mới khi server phản hồi.", true);
            clearForm();

            if (onItemCreated != null) {
                onItemCreated.run();
            }

        } catch (NumberFormatException e) {
            showMessage("❌ Giá khởi điểm và bước giá phải là số hợp lệ.", false);
        } catch (Exception e) {
            showMessage("❌ Lỗi khi gửi yêu cầu: " + e.getMessage(), false);
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
        durationSpinner.getValueFactory().setValue(60);

        selectedImagePath = null;
        selectedImageLabel.setText("Chưa chọn ảnh");
        imagePreview.setImage(null);
    }

    private void showMessage(String message, boolean success) {
        messageLabel.setText(message);
        messageLabel.setTextFill(Color.web(success ? "#10b981" : "#dc2626"));
    }
}
