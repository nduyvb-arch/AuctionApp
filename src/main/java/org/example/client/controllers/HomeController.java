package org.example.client.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Font;
import org.example.client.ClientApp;
import org.example.common.Message;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label userInfoLabel;
    @FXML
    private Button logoutButton;
    @FXML
    private FlowPane itemFlowPane;

    private List<Item> items = new ArrayList<>();
    private ObjectOutputStream out;
    private ObjectInputStream in;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 1. Thiết lập thông tin người dùng
        User currentUser = ClientApp.getCurrentUser();
        if (currentUser != null) {
            welcomeLabel.setText("Chào mừng, " + currentUser.getUsername() + "!");
            userInfoLabel.setText("Vai trò: " + currentUser.getRole() + " | ID: " + currentUser.getId());
        } else {
            userInfoLabel.setText("Không có thông tin người dùng");
        }

        // 2. Lấy stream để giao tiếp với server
        out = ClientApp.getOutputStream();
        in = ClientApp.getInputStream();

        // 3. Lấy danh sách vật phẩm ban đầu và khởi động luồng lắng nghe
        loadInitialItems();
        startListeningForUpdates();
    }

    private void loadInitialItems() {
        Task<ArrayList<Item>> loadItemsTask = new Task<>() {
            @Override
            protected ArrayList<Item> call() throws Exception {
                System.out.println("Client: Gửi yêu cầu GET_ALL_ITEMS...");
                out.writeObject(new Message("GET_ALL_ITEMS", null));
                out.flush();

                Message response = (Message) in.readObject();
                if (response.getAction().equals("GET_ALL_ITEMS_RESPONSE")) {
                    System.out.println("Client: Đã nhận danh sách vật phẩm.");
                    return (ArrayList<Item>) response.getPayload();
                }
                return new ArrayList<>();
            }
        };

        loadItemsTask.setOnSucceeded(event -> {
            items = loadItemsTask.getValue();
            refreshItemDisplay();
        });

        loadItemsTask.setOnFailed(event -> {
            System.err.println("Lỗi khi tải danh sách vật phẩm: " + loadItemsTask.getException().getMessage());
        });

        new Thread(loadItemsTask).start();
    }

    private void startListeningForUpdates() {
        Task<Void> listenerTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("Client: Bắt đầu lắng nghe cập nhật từ server...");
                while (!isCancelled()) {
                    try {
                        // Vòng lặp này sẽ block (chờ) cho đến khi có tin nhắn mới
                        Message updateMessage = (Message) in.readObject();

                        if (updateMessage.getAction().equals("BID")) {
                            Item updatedItem = (Item) updateMessage.getPayload();
                            System.out.println("Client: Nhận được cập nhật cho vật phẩm " + updatedItem.getItemName());

                            // Cập nhật giao diện trên luồng chính của JavaFX
                            Platform.runLater(() -> updateItemInUI(updatedItem));
                        }
                    } catch (Exception e) {
                        if (!isCancelled()) {
                            System.err.println("Mất kết nối với server: " + e.getMessage());
                        }
                        break; // Thoát vòng lặp nếu có lỗi
                    }
                }
                return null;
            }
        };

        // Đảm bảo luồng lắng nghe kết thúc khi ứng dụng đóng
        listenerTask.setOnFailed(event -> {
            System.err.println("Luồng lắng nghe đã dừng do lỗi.");
        });

        Thread listenerThread = new Thread(listenerTask);
        listenerThread.setDaemon(true); // Đặt là luồng daemon để nó tự kết thúc khi ứng dụng chính thoát
        listenerThread.start();
    }

    private void refreshItemDisplay() {
        itemFlowPane.getChildren().clear();
        for (Item item : items) {
            itemFlowPane.getChildren().add(createItemNode(item));
        }
    }

    private void updateItemInUI(Item updatedItem) {
        // Cập nhật danh sách trong bộ nhớ
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(updatedItem.getId())) {
                items.set(i, updatedItem);
                break;
            }
        }

        // Tìm và cập nhật Node trên giao diện
        for (Node node : itemFlowPane.getChildren()) {
            if (node.getUserData() != null && node.getUserData().equals(updatedItem.getId())) {
                // Giả sử chúng ta có một phương thức để cập nhật node đã có
                // Cách đơn giản nhất là tạo lại và thay thế
                int index = itemFlowPane.getChildren().indexOf(node);
                itemFlowPane.getChildren().set(index, createItemNode(updatedItem));
                break;
            }
        }
    }

    private Node createItemNode(Item item) {
        AnchorPane pane = new AnchorPane();
        pane.setUserData(item.getId()); // Lưu ID của item vào Node để tìm kiếm sau này
        pane.setPrefSize(300, 165);
        pane.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #cbd5e1; -fx-border-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 8, 0, 0, 4);");

        Label nameLabel = new Label(item.getItemName());
        nameLabel.setFont(new Font("System Bold", 18));
        nameLabel.setTextFill(javafx.scene.paint.Color.web("#0f172a"));
        AnchorPane.setTopAnchor(nameLabel, 15.0);
        AnchorPane.setLeftAnchor(nameLabel, 15.0);

        Label priceLabel = new Label("Giá hiện tại:");
        priceLabel.setTextFill(javafx.scene.paint.Color.web("#64748b"));
        AnchorPane.setTopAnchor(priceLabel, 50.0);
        AnchorPane.setLeftAnchor(priceLabel, 15.0);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        Label priceValueLabel = new Label(currencyFormat.format(item.getCurrentPrice()));
        priceValueLabel.setFont(new Font("System Bold", 22));
        priceValueLabel.setTextFill(javafx.scene.paint.Color.web("#2563eb"));
        AnchorPane.setTopAnchor(priceValueLabel, 70.0);
        AnchorPane.setLeftAnchor(priceValueLabel, 15.0);

        // Thêm các label khác (trạng thái, thời gian, ...) nếu cần
        // ...

        pane.getChildren().addAll(nameLabel, priceLabel, priceValueLabel);
        return pane;
    }

    @FXML
    public void onLogoutClicked() {
        ClientApp.setCurrentUser(null);
        try {
            // Cần dừng luồng lắng nghe ở đây nếu không nó sẽ tiếp tục chạy
            ClientApp.switchToLogin();
        } catch (Exception e) {
            System.err.println("Error switching to login: " + e.getMessage());
        }
    }
}
