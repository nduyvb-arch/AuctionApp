// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package org.example.client.controllers;

import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import org.example.common.Message;
import org.example.common.model.user.User;

public class AddItemViewController implements Initializable {
    @FXML
    private ComboBox<String> itemTypeComboBox;
    @FXML
    private TextField itemNameTextField;
    @FXML
    private TextArea itemDescriptionTextArea;
    @FXML
    private TextField startPriceTextField;
    @FXML
    private TextField bidIncrementTextField;
    @FXML
    private Spinner<Integer> durationSpinner;
    @FXML
    private Label messageLabel;
    @FXML
    private Button submitItemButton;
    @FXML
    private Button clearFormButton;
    private ObjectOutputStream out;
    private User currentUser;
    private Runnable onItemCreated;

    public AddItemViewController() {
    }

    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.itemTypeComboBox.getItems().setAll(new String[]{"Electronic", "Vehicle", "Art"});
        this.itemTypeComboBox.setValue("Electronic");
        this.durationSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10080, 60));
        this.durationSpinner.setEditable(true);
    }

    public void setup(ObjectOutputStream out, User currentUser, Runnable onItemCreated) {
        this.out = out;
        this.currentUser = currentUser;
        this.onItemCreated = onItemCreated;
    }

    @FXML
    private void onSubmitItemClicked() {
        if (this.out != null && this.currentUser != null) {
            String name = this.itemNameTextField.getText().trim();
            String description = this.itemDescriptionTextArea.getText().trim();
            if (name.isEmpty()) {
                this.showMessage("❌ Vui lòng nhập tên sản phẩm.", false);
            } else if (description.isEmpty()) {
                this.showMessage("❌ Vui lòng nhập mô tả sản phẩm.", false);
            } else {
                try {
                    double startPrice = Double.parseDouble(this.startPriceTextField.getText().trim());
                    double bidIncrement = Double.parseDouble(this.bidIncrementTextField.getText().trim());
                    int duration = (Integer)this.durationSpinner.getValue();
                    if (startPrice <= (double)0.0F || bidIncrement <= (double)0.0F || duration <= 0) {
                        this.showMessage("❌ Giá và thời gian đấu giá phải lớn hơn 0.", false);
                        return;
                    }

                    Object[] itemData = new Object[]{this.itemTypeComboBox.getValue(), name, description, startPrice, bidIncrement, this.currentUser.getId(), duration};
                    synchronized(this.out) {
                        this.out.writeObject(new Message("ADD_ITEM", itemData));
                        this.out.flush();
                    }

                    this.showMessage("✅ Đã gửi yêu cầu đăng sản phẩm. Danh sách sẽ tự làm mới khi server phản hồi.", true);
                    this.clearForm();
                    if (this.onItemCreated != null) {
                        this.onItemCreated.run();
                    }
                } catch (NumberFormatException var11) {
                    this.showMessage("❌ Giá khởi điểm và bước giá phải là số hợp lệ.", false);
                } catch (Exception e) {
                    this.showMessage("❌ Lỗi khi gửi yêu cầu: " + e.getMessage(), false);
                }

            }
        } else {
            this.showMessage("❌ Chưa có kết nối hoặc chưa đăng nhập.", false);
        }
    }

    @FXML
    private void onClearFormClicked() {
        this.clearForm();
        this.showMessage("", true);
    }

    private void clearForm() {
        this.itemTypeComboBox.setValue("Electronic");
        this.itemNameTextField.clear();
        this.itemDescriptionTextArea.clear();
        this.startPriceTextField.clear();
        this.bidIncrementTextField.clear();
        this.durationSpinner.getValueFactory().setValue(60);
    }

    private void showMessage(String message, boolean success) {
        this.messageLabel.setText(message);
        this.messageLabel.setTextFill(Color.web(success ? "#10b981" : "#dc2626"));
    }
}
