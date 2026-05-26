package org.example.client.controllers;

import org.example.client.ClientApp; // ADDED
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import org.example.common.Message;
import org.example.common.model.user.User;

import java.net.URL;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class AccountViewController implements Initializable {

    @FXML private Label usernameLabel;
    @FXML private Label userIdLabel;
    @FXML private Label roleLabel;
    @FXML private Label balanceLabel;
    @FXML private Label statusLabel;
    @FXML private Label noteLabel;

    @FXML private Button topUpButton;
    @FXML private Button refreshButton;

    private User currentUser;
    private UserUpdateCallback userUpdateCallback;

    private static final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {}

    // XÓA THAM SỐ out
    public void setup(User currentUser, UserUpdateCallback userUpdateCallback) {
        this.currentUser = currentUser;
        this.userUpdateCallback = userUpdateCallback;
        updateUser(currentUser);
    }

    public void updateUser(User user) {
        this.currentUser = user;

        if (user == null) {
            usernameLabel.setText("Chưa đăng nhập");
            userIdLabel.setText("-"); roleLabel.setText("-");
            balanceLabel.setText("0 VNĐ"); statusLabel.setText("-");
            noteLabel.setText("Bạn cần đăng nhập để xem thông tin.");
            return;
        }

        usernameLabel.setText(user.getUsername());
        userIdLabel.setText(user.getId());
        roleLabel.setText(getRoleText(user.getRole()));
        balanceLabel.setText(currencyFormat.format(user.getBalance()) + " VNĐ");
        statusLabel.setText(user.isBanned() ? "Đang bị khóa" : "Đang hoạt động");

        if (topUpButton != null) topUpButton.setDisable(false);
        if (refreshButton != null) refreshButton.setDisable(false);

        if ("seller".equalsIgnoreCase(user.getRole())) {
            noteLabel.setText("Số dư bao gồm tiền nạp và nhận được từ đấu giá.");
        } else {
            noteLabel.setText("Nạp tiền để tham gia đặt giá.");
        }
    }

    @FXML
    private void onTopUpClicked() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Nạp tiền", "Bạn cần đăng nhập trước.");
            return;
        }

        ChoiceDialog<String> methodDialog = new ChoiceDialog<>("Chuyển khoản", Arrays.asList("Chuyển khoản", "Ví điện tử"));
        methodDialog.setTitle("Nạp tiền");
        Optional<String> methodResult = methodDialog.showAndWait();
        if (methodResult.isEmpty()) return;

        TextInputDialog amountDialog = new TextInputDialog();
        amountDialog.setTitle("Nạp tiền");
        amountDialog.setContentText("Số tiền (VNĐ):");
        Optional<String> amountResult = amountDialog.showAndWait();
        if (amountResult.isEmpty()) return;

        try {
            double amount = Double.parseDouble(amountResult.get().trim().replace(".", ""));
            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "Nạp tiền", "Số tiền nạp phải > 0.");
                return;
            }
            sendTopUpRequest(amount, methodResult.get());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Nạp tiền", "Số tiền không hợp lệ.");
        }
    }

    @FXML
    private void onRefreshAccountClicked() {
        requestAccountInfoFromServer();
    }

    // GỬI QUA ỐNG MỚI
    private void sendTopUpRequest(double amount, String method) {
        if (topUpButton != null) topUpButton.setDisable(true);
        ClientApp.sendMessage(new Message("TOP_UP", new Object[]{currentUser.getId(), amount, method}));
    }

    // GỬI QUA ỐNG MỚI
    private void requestAccountInfoFromServer() {
        if (currentUser == null) return;
        if (refreshButton != null) refreshButton.setDisable(true);
        ClientApp.sendMessage(new Message("GET_ACCOUNT_INFO", currentUser.getId()));
    }

    @FXML
    private void onBackToRoleSelectionClicked() {
        try { ClientApp.switchToRoleSelection(); }
        catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Tài khoản", e.getMessage()); }
    }

    private String getRoleText(String role) {
        if (role == null) return "Không rõ";
        switch (role.toLowerCase()) {
            case "bidder": return "Người đấu giá";
            case "seller": return "Người bán";
            case "admin": return "Quản trị viên";
            default: return role;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type); alert.setTitle(title); alert.setContentText(content); alert.showAndWait();
    }

    public interface UserUpdateCallback { void onUpdated(User updatedUser); }
}