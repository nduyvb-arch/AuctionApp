package org.example.client.controllers;

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

import java.io.ObjectOutputStream;
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

    private ObjectOutputStream out;
    private User currentUser;
    private UserUpdateCallback userUpdateCallback;

    private static final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    public void setup(ObjectOutputStream out, User currentUser, UserUpdateCallback userUpdateCallback) {
        this.out = out;
        this.currentUser = currentUser;
        this.userUpdateCallback = userUpdateCallback;
        updateUser(currentUser);
    }

    public void updateUser(User user) {
        this.currentUser = user;

        if (user == null) {
            usernameLabel.setText("Chưa đăng nhập");
            userIdLabel.setText("-");
            roleLabel.setText("-");
            balanceLabel.setText("0 VNĐ");
            statusLabel.setText("-");
            noteLabel.setText("Bạn cần đăng nhập để xem thông tin tài khoản.");
            return;
        }

        usernameLabel.setText(user.getUsername());
        userIdLabel.setText(user.getId());
        roleLabel.setText(getRoleText(user.getRole()));
        balanceLabel.setText(currencyFormat.format(user.getBalance()) + " VNĐ");
        statusLabel.setText(user.isBanned() ? "Đang bị khóa" : "Đang hoạt động");

        if ("seller".equalsIgnoreCase(user.getRole())) {
            noteLabel.setText("Số dư bao gồm tiền nạp và tiền nhận được từ các phiên đấu giá đã kết thúc.");
        } else {
            noteLabel.setText("Bạn có thể nạp tiền để tham gia đặt giá trong các phiên đấu giá đang diễn ra.");
        }
    }

    @FXML
    private void onTopUpClicked() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Nạp tiền", "Bạn cần đăng nhập trước.");
            return;
        }

        ChoiceDialog<String> methodDialog = new ChoiceDialog<>(
                "Chuyển khoản ngân hàng",
                Arrays.asList("Chuyển khoản ngân hàng", "Ví điện tử", "Mô phỏng nạp tiền")
        );
        methodDialog.setTitle("Nạp tiền");
        methodDialog.setHeaderText("Chọn phương thức nạp tiền");
        methodDialog.setContentText("Phương thức:");

        Optional<String> methodResult = methodDialog.showAndWait();

        if (methodResult.isEmpty()) {
            return;
        }

        TextInputDialog amountDialog = new TextInputDialog();
        amountDialog.setTitle("Nạp tiền");
        amountDialog.setHeaderText("Nhập số tiền muốn nạp");
        amountDialog.setContentText("Số tiền (VNĐ):");

        Platform.runLater(() -> {
            if (amountDialog.getEditor() != null) {
                amountDialog.getEditor().requestFocus();
            }
        });

        Optional<String> amountResult = amountDialog.showAndWait();

        if (amountResult.isEmpty()) {
            return;
        }

        try {
            String raw = amountResult.get()
                    .trim()
                    .replace(".", "")
                    .replace(",", "");

            double amount = Double.parseDouble(raw);

            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "Nạp tiền", "Số tiền nạp phải lớn hơn 0.");
                return;
            }

            if ("Chuyển khoản ngân hàng".equals(methodResult.get())) {
                boolean confirmed = showTransferInfo(amount);

                if (!confirmed) {
                    return;
                }
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

    private boolean showTransferInfo(double amount) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Thông tin chuyển khoản");
        alert.setHeaderText("Chuyển khoản ngân hàng");

        String content = "Ngân hàng: Auction Bank Demo\n"
                + "Số tài khoản: 0123456789\n"
                + "Chủ tài khoản: AUCTION APP\n"
                + "Số tiền: " + currencyFormat.format(amount) + " VNĐ\n"
                + "Nội dung: NAPTIEN " + currentUser.getId() + "\n\n"
                + "Vì đây là bài tập/demo, bấm Xác nhận để mô phỏng giao dịch thành công.";

        alert.setContentText(content);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void sendTopUpRequest(double amount, String method) {
        if (out == null) {
            showAlert(Alert.AlertType.ERROR, "Nạp tiền", "Không có kết nối tới server.");
            return;
        }

        try {
            if (topUpButton != null) {
                topUpButton.setDisable(true);
            }

            Object[] payload = new Object[]{
                    currentUser.getId(),
                    amount,
                    method
            };

            synchronized (out) {
                out.writeObject(new Message("TOP_UP", payload));
                out.flush();
            }

        } catch (Exception e) {
            if (topUpButton != null) {
                topUpButton.setDisable(false);
            }
            showAlert(Alert.AlertType.ERROR, "Nạp tiền", "Lỗi gửi yêu cầu nạp tiền: " + e.getMessage());
        }
    }

    private void requestAccountInfoFromServer() {
        if (out == null || currentUser == null) {
            updateUser(currentUser);
            return;
        }

        try {
            if (refreshButton != null) {
                refreshButton.setDisable(true);
            }

            synchronized (out) {
                out.writeObject(new Message("GET_ACCOUNT_INFO", currentUser.getId()));
                out.flush();
            }

            Platform.runLater(() -> {
                if (refreshButton != null) {
                    refreshButton.setDisable(false);
                }
            });

        } catch (Exception e) {
            if (refreshButton != null) {
                refreshButton.setDisable(false);
            }
            showAlert(Alert.AlertType.ERROR, "Tài khoản", "Không thể làm mới tài khoản: " + e.getMessage());
        }
    }

    private String getRoleText(String role) {
        if (role == null) {
            return "Không rõ";
        }

        switch (role.toLowerCase()) {
            case "bidder":
                return "Người đấu giá";

            case "seller":
                return "Người bán";

            case "admin":
                return "Quản trị viên";

            default:
                return role;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public interface UserUpdateCallback {
        void onUpdated(User updatedUser);
    }
}
