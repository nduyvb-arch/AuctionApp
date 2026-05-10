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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class ClientApp extends Application {

    /** Stage chính của ứng dụng. */
    private static Stage primaryStage;

    /** Người dùng hiện tại đang đăng nhập vào hệ thống. */
    private static User currentUser;

    // --- Network Components ---
    private static Socket socket;
    private static ObjectOutputStream outputStream;
    private static ObjectInputStream inputStream;
    private static final String SERVER_ADDRESS = "localhost"; // Hoặc IP của server
    private static final int SERVER_PORT = 8888; // Phải khớp với cổng của server

    /**
     * Điểm bắt đầu của ứng dụng JavaFX.
     * @param stage Stage chính.
     * @throws Exception nếu không tải được FXML.
     */
    @Override
    public void start(final Stage stage) throws Exception {
        try {
            primaryStage = stage;

            // --- Thiết lập kết nối mạng và bắt buộc thoát nếu thất bại ---
            try {
                System.out.println("Đang kết nối tới server tại " + SERVER_ADDRESS + ":" + SERVER_PORT + "...");
                socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                inputStream = new ObjectInputStream(socket.getInputStream());
                System.out.println("Kết nối server thành công!");
            } catch (Exception e) {
                System.err.println("Không thể kết nối tới server: " + e.getMessage());
                showConnectionErrorAndExit(e.getMessage());
                return; // Dừng thực thi phương thức start
            }

            Image icon = new Image(getClass().getResourceAsStream("/images/logo.png"));
            primaryStage.getIcons().add(icon);

            switchToLogin();
            stage.setResizable(false);

            // Xử lý khi người dùng đóng ứng dụng
            stage.setOnCloseRequest(event -> {
                closeConnection();
                System.out.println("Ứng dụng đã đóng.");
            });

            stage.show();
        } catch (Exception e) {
            System.err.println("Lỗi nghiêm trọng khi khởi động ứng dụng: " + e.getMessage());
            e.printStackTrace();
            Platform.exit();
        }
    }

    /**
     * Hiển thị hộp thoại báo lỗi kết nối và đóng ứng dụng.
     * @param message Nội dung lỗi.
     */
    private void showConnectionErrorAndExit(String message) {
        // Đảm bảo rằng mã giao diện được chạy trên luồng JavaFX
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi Kết Nối");
            alert.setHeaderText("Không thể kết nối đến máy chủ.");
            alert.setContentText("Chi tiết: " + message + "\nVui lòng đảm bảo máy chủ đang chạy và thử lại.");
            alert.showAndWait();
            Platform.exit(); // Đóng ứng dụng JavaFX
        });
    }


    @Override
    public void stop() throws Exception {
        closeConnection();
        super.stop();
    }

    /**
     * Gửi thông báo và đóng kết nối mạng một cách an toàn.
     */
    private static void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                socket.close();
                System.out.println("Đã ngắt kết nối với server.");
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }

    /** Chuyển giao diện sang màn hình đăng nhập. */
    public static void switchToLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/org/example/client/views/LoginMenu.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setTitle("Hệ thống đấu giá - Đăng nhập");
        primaryStage.setScene(scene);
    }

    /** Chuyển giao diện sang màn hình đăng ký. */
    public static void switchToSignUp() throws Exception {
        System.out.println("Loading SignUpMenu.fxml...");
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/org/example/client/views/SignUpMenu.fxml"));
        if (loader.getLocation() == null) {
            throw new Exception("SignUpMenu.fxml not found");
        }
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setTitle("Hệ thống đấu giá - Đăng ký");
        primaryStage.setScene(scene);
        System.out.println("SignUpMenu loaded successfully.");
    }

    /** Chuyển giao diện sang trang chủ. */
    public static void switchToHome() throws Exception {
        System.out.println("Loading HomeMenu.fxml...");
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/org/example/client/views/HomeMenu.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setTitle("Hệ thống đấu giá - Trang chủ");
        primaryStage.setScene(scene);
        System.out.println("HomeMenu loaded successfully.");
    }

    /**
     * Lấy người dùng hiện tại.
     * @return người dùng hiện tại.
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Thiết lập người dùng hiện tại.
     * @param user người dùng mới.
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    /**
     * Cung cấp stream để gửi dữ liệu đến server.
     * @return ObjectOutputStream.
     */
    public static ObjectOutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Cung cấp stream để nhận dữ liệu từ server.
     * @return ObjectInputStream.
     */
    public static ObjectInputStream getInputStream() {
        return inputStream;
    }

    /**
     * Hàm main để khởi chạy ứng dụng.
     * @param args tham số dòng lệnh.
     */
    public static void main(final String[] args) {
        launch(args);
    }
}