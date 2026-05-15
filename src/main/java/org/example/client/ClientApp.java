package org.example.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.example.common.model.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(ClientApp.class);

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

        switchToLogin();
        stage.setResizable(false);

        stage.setOnCloseRequest(event -> {
            closeConnection();
            logger.info("Ứng dụng đã đóng.");
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
        } catch (IOException e) {
            logger.error("Lỗi khi tạo kết nối mới: {}", e.getMessage());
            throw e;
        }
    }

    public static void closeConnection() {
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
