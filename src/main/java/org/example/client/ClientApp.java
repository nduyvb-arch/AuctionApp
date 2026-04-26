package org.example.client;
import javafx.scene.image.Image;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.manager.UserManager;
import org.example.model.user.User;

/**
 * Lớp chính điều khiển ứng dụng Client sử dụng JavaFX.
 */
public class ClientApp extends Application {

    /** Stage chính của ứng dụng. */
    private static Stage primaryStage;

    /** Người dùng hiện tại đang đăng nhập vào hệ thống. */
    private static User currentUser;

    /**
     * Điểm bắt đầu của ứng dụng JavaFX.
     * @param stage Stage chính.
     * @throws Exception nếu không tải được FXML.
     */
    @Override
    public void start(final Stage stage) throws Exception {
        primaryStage = stage;

        Image icon = new Image(getClass().getResourceAsStream("/images/logo.png"));
        primaryStage.getIcons().add(icon);
        // Hiển thị danh sách tài khoản khi ứng dụng khởi động
        //UserManager.getInstance().printAllUsers();

        switchToLogin();
        stage.setResizable(false);

        // Đóng kết nối database khi đóng ứng dụng
        stage.setOnCloseRequest(event -> {
            UserManager.getInstance().closeConnection();
            System.out.println("✅ Ứng dụng đóng lại");
        });

        stage.show();
    }

    @Override
    public void stop() throws Exception {
        UserManager.getInstance().closeConnection();
        super.stop();
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
     * Hàm main để khởi chạy ứng dụng.
     * @param args tham số dòng lệnh.
     */
    public static void main(final String[] args) {
        launch(args);
    }
}