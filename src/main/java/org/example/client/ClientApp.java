package org.example.client;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.common.model.user.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientApp extends Application {

    private static Stage primaryStage;
    private static User currentUser;

    private static Socket socket;
    private static ObjectOutputStream outputStream;
    private static ObjectInputStream inputStream;
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8888;

    @Override
    public void start(final Stage stage) throws Exception {
        primaryStage = stage;
        Image icon = new Image(getClass().getResourceAsStream("/images/logo.png"));
        primaryStage.getIcons().add(icon);

        // Không kết nối tự động khi khởi động
        // Việc kết nối sẽ do LoginController xử lý
        switchToLogin();
        stage.setResizable(false);

        stage.setOnCloseRequest(event -> {
            closeConnection();
            System.out.println("✅ Ứng dụng đã đóng.");
        });

        stage.show();
    }

    /**
     * Phương thức mới: Thiết lập một kết nối hoàn toàn mới đến server.
     * Ném Exception nếu thất bại để Controller có thể bắt và xử lý.
     */
    public static void connectToServer() throws IOException {
        if (socket != null && !socket.isClosed()) {
            System.out.println("Kết nối đã tồn tại, không cần tạo mới.");
            return;
        }
        try {
            System.out.println("Đang tạo kết nối mới tới server...");
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
            System.out.println("Kết nối mới thành công!");
        } catch (IOException e) {
            System.err.println("Lỗi khi tạo kết nối mới: " + e.getMessage());
            throw e; // Ném lại lỗi để LoginController xử lý
        }
    }

    /**
     * Đóng kết nối hiện tại và dọn dẹp tài nguyên.
     */
    public static void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                System.out.println("Đang đóng kết nối...");
                // Đóng stream theo thứ tự ngược lại để tránh lỗi
                if (outputStream != null) outputStream.close();
                if (inputStream != null) inputStream.close();
                socket.close();
                System.out.println("Đã đóng kết nối thành công.");
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
        } finally {
            // Đảm bảo tất cả tài nguyên được giải phóng
            socket = null;
            outputStream = null;
            inputStream = null;
        }
    }

    @Override
    public void stop() throws Exception {
        closeConnection();
        super.stop();
    }

    public static void switchToLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/org/example/client/views/LoginMenu.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setTitle("Hệ thống đấu giá - Đăng nhập");
        primaryStage.setScene(scene);
    }

    public static void switchToHome() throws Exception {
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/org/example/client/views/HomeMenu.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setTitle("Hệ thống đấu giá - Trang chủ");
        primaryStage.setScene(scene);
    }
    
    public static void switchToSignUp() throws Exception {
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/org/example/client/views/SignUpMenu.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setTitle("Hệ thống đấu giá - Đăng ký");
        primaryStage.setScene(scene);
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static ObjectOutputStream getOutputStream() {
        return outputStream;
    }

    public static ObjectInputStream getInputStream() {
        return inputStream;
    }

    public static void main(final String[] args) {
        launch(args);
    }
}
