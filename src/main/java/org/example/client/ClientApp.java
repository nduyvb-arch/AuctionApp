package org.example.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.example.common.Message;
import org.example.common.model.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.Normalizer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ClientApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(ClientApp.class);

    private static Stage primaryStage;
    private static User currentUser;

    private static boolean openAccountOnHomeLoad = false;

    public static void setOpenAccountOnHomeLoad(boolean value) {
        openAccountOnHomeLoad = value;
    }

    public static boolean shouldOpenAccountOnHomeLoad() {
        boolean result = openAccountOnHomeLoad;
        openAccountOnHomeLoad = false;
        return result;
    }

    private static Socket socket;
    private static ObjectOutputStream outputStream;
    private static ObjectInputStream inputStream;

    private static volatile Consumer<Message> serverMessageHandler;
    private static volatile Thread serverListenerThread;

    private static volatile CompletableFuture<byte[]> imageResponseFuture;

    public static final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8888;

    private static String selectedRole = "bidder";

    @Override
    public void start(final Stage stage) throws Exception {
        primaryStage = stage;

        Image icon = new Image(getClass().getResourceAsStream("/images/logo.png"));
        primaryStage.getIcons().add(icon);

        switchToLogin();

        stage.setResizable(true);
        stage.setMinWidth(1000);
        stage.setMinHeight(700);

        stage.setOnCloseRequest(event -> {
            closeConnection();
            logger.info("Ứng dụng đã đóng.");
            Platform.exit();
            System.exit(0);
        });

        stage.show();
    }

    public static void connectToServer() throws IOException {
        closeConnection();

        try {
            logger.info("Đang tạo kết nối mới tới server tại {}:{}", SERVER_ADDRESS, SERVER_PORT);
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
            logger.info("Kết nối mới thành công!");
            startServerListenerIfNeeded(); // Bắt đầu lắng nghe ngay sau khi kết nối
        } catch (IOException e) {
            logger.error("Lỗi khi tạo kết nối mới: {}", e.getMessage());
            throw e;
        }
    }

    public static void closeConnection() {
        if (serverListenerThread != null) {
            serverListenerThread.interrupt(); // Ngắt luồng lắng nghe
            serverListenerThread = null;
        }
        try {
            if (socket != null && !socket.isClosed()) {
                logger.info("Đang đóng kết nối...");
                if (outputStream != null) outputStream.close();
                if (inputStream != null) inputStream.close();
                socket.close();
                logger.info("Đã đóng kết nối thành công.");
            }
        } catch (IOException e) {
            logger.warn("Lỗi không nghiêm trọng khi đóng kết nối: {}", e.getMessage());
        } finally {
            socket = null;
            outputStream = null;
            inputStream = null;
        }
    }

    @Override
    public void stop() throws Exception {
        closeConnection();
        executorService.shutdownNow();
        super.stop();
    }

    public static void switchToLogin() throws Exception {
        closeConnection(); // Đảm bảo đóng kết nối cũ trước khi về màn hình login
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/org/example/client/views/LoginMenu.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        applyScene(scene, "Hệ thống đấu giá - Đăng nhập", 1000, 700, 1200, 800);
    }

    public static void switchToRoleSelection() throws Exception {
        if (isAdminUser(currentUser)) {
            switchToAdmin();
            return;
        }
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/org/example/client/views/RoleSelection.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        applyScene(scene, "Hệ thống đấu giá - Chọn vai trò", 1000, 700, 1200, 800);
    }

    public static void switchToHome() throws Exception {
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/org/example/client/views/HomeMenu.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        applyScene(scene, "Hệ thống đấu giá - Trang chủ", 1000, 700, 1200, 800);
    }

    public static void switchToAdmin() throws Exception {
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/org/example/client/views/admin/AdminDashboard.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        applyScene(scene, "Hệ thống đấu giá - Quản trị", 1100, 720, 1280, 820);
    }

    public static void switchToSignUp() throws Exception {
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/org/example/client/views/SignUpMenu.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        applyScene(scene, "Hệ thống đấu giá - Đăng ký", 1000, 700, 1200, 800);
    }

    private static void applyScene(Scene scene, String title,
                                   double minWidth, double minHeight,
                                   double defaultWidth, double defaultHeight) {
        boolean wasMaximized = primaryStage.isMaximized();
        double previousWidth = primaryStage.getWidth();
        double previousHeight = primaryStage.getHeight();
        double previousX = primaryStage.getX();
        double previousY = primaryStage.getY();
        boolean hadPreviousSize = previousWidth > 0 && previousHeight > 0;

        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(minWidth);
        primaryStage.setMinHeight(minHeight);

        if (wasMaximized) {
            Platform.runLater(() -> primaryStage.setMaximized(true));
            return;
        }

        if (hadPreviousSize) {
            primaryStage.setWidth(Math.max(previousWidth, minWidth));
            primaryStage.setHeight(Math.max(previousHeight, minHeight));
            primaryStage.setX(previousX);
            primaryStage.setY(previousY);
        } else {
            primaryStage.setWidth(defaultWidth);
            primaryStage.setHeight(defaultHeight);
            primaryStage.centerOnScreen();
        }
    }

    public static void setServerMessageHandler(Consumer<Message> handler) {
        serverMessageHandler = handler;
    }

    public static void startServerListenerIfNeeded() {
        if (inputStream == null || socket == null || socket.isClosed()) {
            logger.warn("Không thể bắt đầu lắng nghe: kết nối không hợp lệ.");
            return;
        }

        if (serverListenerThread != null && serverListenerThread.isAlive()) {
            logger.info("Luồng lắng nghe đã chạy, không cần khởi động lại.");
            return;
        }

        serverListenerThread = new Thread(() -> {
            logger.info("Bắt đầu luồng lắng nghe server...");
            while (!Thread.currentThread().isInterrupted() && socket != null && !socket.isClosed()) {
                try {
                    Message message = (Message) inputStream.readObject();
                    if (message == null) continue;

                    // Xử lý phản hồi ảnh một cách đặc biệt
                    if ("GET_IMAGE_RESPONSE".equals(message.getAction())) {
                        if (imageResponseFuture != null && !imageResponseFuture.isDone()) {
                            imageResponseFuture.complete((byte[]) message.getPayload());
                        }
                        continue; // Không chuyển tin nhắn ảnh cho handler chung
                    }

                    // Đối với các tin nhắn khác, chuyển cho handler đã đăng ký
                    Consumer<Message> handler = serverMessageHandler;
                    if (handler != null) {
                        Platform.runLater(() -> handler.accept(message));
                    } else {
                        logger.warn("Đã nhận tin nhắn nhưng không có handler nào được thiết lập: {}", message.getAction());
                    }

                } catch (IOException e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        logger.error("Mất kết nối với server: {}", e.getMessage());
                        // Có thể thêm logic để hiển thị thông báo lỗi cho người dùng ở đây
                    }
                    break;
                } catch (ClassNotFoundException e) {
                    logger.error("Lỗi không thể giải mã tin nhắn từ server: {}", e.getMessage());
                }
            }
            logger.info("Luồng lắng nghe server đã dừng.");
        }, "client-server-listener");

        serverListenerThread.setDaemon(true);
        serverListenerThread.start();
    }

    public static boolean isAdminUser(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String normalizedRole = Normalizer.normalize(user.getRole(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim().toLowerCase()
                .replace("đ", "d");
        return "admin".equals(normalizedRole) || "quantrivien".equals(normalizedRole);
    }

    public static byte[] getImageBytes(String imagePath) {
        if (outputStream == null || imagePath == null || imagePath.isBlank()) {
            return null;
        }
        // Tạo một CompletableFuture mới cho mỗi yêu cầu
        imageResponseFuture = new CompletableFuture<>();
        sendMessage(new Message("GET_IMAGE", imagePath));
        try {
            // Đợi kết quả trong một khoảng thời gian nhất định
            return imageResponseFuture.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Lỗi hoặc timeout khi tải ảnh '{}': {}", imagePath, e.getMessage());
            return null;
        }
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static String getSelectedRole() {
        return selectedRole;
    }

    public static void setSelectedRole(String role) {
        selectedRole = (role == null || role.isBlank()) ? "bidder" : role.trim().toLowerCase();
    }

    public static synchronized void sendMessage(Message message) {
        if (outputStream == null || socket == null || socket.isClosed()) {
            logger.error("Không thể gửi tin nhắn, kết nối đã đóng hoặc không hợp lệ.");
            return;
        }
        try {
            outputStream.writeObject(message);
            outputStream.flush();
            outputStream.reset(); // Reset trạng thái để tránh lỗi cache đối tượng
        } catch (IOException e) {
            logger.error("Lỗi khi gửi tin nhắn tới Server: {}", e.getMessage());
            // Có thể cần đóng kết nối và yêu cầu đăng nhập lại ở đây
        }
    }

    public static boolean isSellerSelected() {
        return "seller".equalsIgnoreCase(selectedRole);
    }

    public static void main(final String[] args) {
        launch(args);
    }
}
