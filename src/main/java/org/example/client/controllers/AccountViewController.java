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
    @FXML private Button withdrawButton;
    @FXML private Button refreshButton;

    private User currentUser;
    private UserUpdateCallback userUpdateCallback;

    private static final NumberFormat currencyFormat = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

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
            setTransactionButtonsEnabled(false);
            if (refreshButton != null) refreshButton.setDisable(true);
            return;
        }

        usernameLabel.setText(user.getUsername());
        userIdLabel.setText(user.getId());
        roleLabel.setText(getRoleText(user.getRole()));
        balanceLabel.setText(currencyFormat.format(user.getBalance()) + " VNĐ");
        statusLabel.setText(user.isBanned() ? "Đang bị khóa" : "Đang hoạt động");

        setTransactionButtonsEnabled(true);
        if (refreshButton != null) refreshButton.setDisable(false);

        if ("seller".equalsIgnoreCase(user.getRole())) {
            noteLabel.setText("Số dư bao gồm tiền nạp và nhận được từ đấu giá.");
        } else {
            noteLabel.setText("Nạp tiền để tham gia đặt giá. Bạn có thể rút tiền khi số dư còn đủ.");
        }
    }

    public void setTransactionButtonsEnabled(boolean enabled) {
        if (topUpButton != null) topUpButton.setDisable(!enabled);
        if (withdrawButton != null) withdrawButton.setDisable(!enabled);
    }

    @FXML
    private void onTopUpClicked() {
        refreshCurrentUserReference();
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
            double amount = parseMoneyInput(amountResult.get());
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
    private void onWithdrawClicked() {
        refreshCurrentUserReference();
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Rút tiền", "Bạn cần đăng nhập trước.");
            return;
        }

        ChoiceDialog<String> methodDialog = new ChoiceDialog<>("Chuyển khoản", Arrays.asList("Chuyển khoản", "Ví điện tử"));
        methodDialog.setTitle("Rút tiền");
        methodDialog.setHeaderText("Chọn phương thức nhận tiền");
        Optional<String> methodResult = methodDialog.showAndWait();
        if (methodResult.isEmpty()) return;

        TextInputDialog amountDialog = new TextInputDialog();
        amountDialog.setTitle("Rút tiền");
        amountDialog.setHeaderText("Số dư hiện tại: " + currencyFormat.format(currentUser.getBalance()) + " VNĐ");
        amountDialog.setContentText("Số tiền muốn rút (VNĐ):");
        Optional<String> amountResult = amountDialog.showAndWait();
        if (amountResult.isEmpty()) return;

        try {
            double amount = parseMoneyInput(amountResult.get());
            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "Rút tiền", "Số tiền rút phải > 0.");
                return;
            }
            if (amount > currentUser.getBalance()) {
                showAlert(Alert.AlertType.WARNING, "Rút tiền", "Số dư không đủ để rút số tiền này.");
                return;
            }
            sendWithdrawRequest(amount, methodResult.get());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Rút tiền", "Số tiền không hợp lệ.");
        }
    }

    @FXML
    private void onRefreshAccountClicked() {
        refreshCurrentUserReference();
        requestAccountInfoFromServer();
    }

    private void refreshCurrentUserReference() {
        User latestUser = ClientApp.getCurrentUser();
        if (latestUser != null && (currentUser == null || latestUser.getId().equals(currentUser.getId()))) {
            currentUser = latestUser;
        }
    }

    private double parseMoneyInput(String rawValue) {
        if (rawValue == null) {
            throw new NumberFormatException("empty");
        }
        return Double.parseDouble(rawValue.trim().replace(".", "").replace(",", ""));
    }

    // GỬI QUA ỐNG MỚI
    private void sendTopUpRequest(double amount, String method) {
        setTransactionButtonsEnabled(false);
        ClientApp.sendMessage(new Message("TOP_UP", new Object[]{currentUser.getId(), amount, method}));
    }

    private void sendWithdrawRequest(double amount, String method) {
        setTransactionButtonsEnabled(false);
        ClientApp.sendMessage(new Message("WITHDRAW", new Object[]{currentUser.getId(), amount, method}));
    }

    // GỬI QUA ỐNG MỚI
    private void requestAccountInfoFromServer() {
        if (currentUser == null) return;
        if (refreshButton != null) refreshButton.setDisable(true);
        ClientApp.sendMessage(new Message("GET_ACCOUNT_INFO", currentUser.getId()));
    }

    public void handleServerMessage(Message message) {
        if (message == null) {
            return;
        }

        Platform.runLater(() -> {
            switch (message.getAction()) {
                case "ACCOUNT_INFO_RESPONSE":
                    handleAccountInfoResponse(message.getPayload());
                    break;
                case "TOP_UP_RESPONSE":
                    handleTopUpResponse(message.getPayload());
                    break;
                case "WITHDRAW_RESPONSE":
                    handleWithdrawResponse(message.getPayload());
                    break;
                default:
                    break;
            }
        });
    }

    private void handleAccountInfoResponse(Object payload) {
        if (!(payload instanceof User)) {
            if (refreshButton != null) {
                refreshButton.setDisable(false);
            }
            return;
        }

        User updatedUser = (User) payload;
        if (currentUser != null && currentUser.getId() != null
                && updatedUser.getId() != null
                && !currentUser.getId().equals(updatedUser.getId())) {
            if (refreshButton != null) {
                refreshButton.setDisable(false);
            }
            return;
        }

        currentUser = updatedUser;
        ClientApp.setCurrentUser(updatedUser);
        updateUser(updatedUser);

        if (userUpdateCallback != null) {
            userUpdateCallback.onUpdated(updatedUser);
        }
    }

    private void handleTopUpResponse(Object payload) {
        handleTransactionResponse(payload, "Nạp tiền", "Nạp tiền thành công", "Nạp tiền thất bại");
    }

    private void handleWithdrawResponse(Object payload) {
        handleTransactionResponse(payload, "Rút tiền", "Rút tiền thành công", "Rút tiền thất bại");
    }

    private void handleTransactionResponse(Object payload, String title, String successHeader, String failureHeader) {
        if (!(payload instanceof Object[])) {
            setTransactionButtonsEnabled(true);
            showAlert(Alert.AlertType.INFORMATION, title, String.valueOf(payload));
            return;
        }

        Object[] data = (Object[]) payload;
        boolean success = Boolean.TRUE.equals(data[0]);
        String message = data.length > 1 ? String.valueOf(data[1]) : "Không rõ phản hồi từ server.";

        if (success && data.length > 2 && data[2] instanceof User) {
            User updatedUser = (User) data[2];
            currentUser = updatedUser;
            ClientApp.setCurrentUser(updatedUser);
            updateUser(updatedUser);

            if (userUpdateCallback != null) {
                userUpdateCallback.onUpdated(updatedUser);
            }
        } else {
            setTransactionButtonsEnabled(true);
        }

        if (success) {
            requestAccountInfoFromServer();
        }

        showAlert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING,
                title,
                (success ? successHeader : failureHeader) + "\n" + message);
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