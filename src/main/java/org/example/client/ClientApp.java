package org.example.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        // Chỉ định đường dẫn tới file fxml
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/org/example/client/views/LoginMenu.fxml"));
        // Tải file lên và gán vào biến root
        Parent root = loader.load();
        // Đưa root vào Scene
        Scene scene = new Scene(root);
        // Thiết lập stage (Cửa sổ)
        stage.setScene(scene);
        stage.setResizable(false); // Để giao diện đăng nhập bị kéo giãn làm xô lệch các nút
        stage.show();
        stage.setTitle("Giao diện đăng nhập");
    }
}
