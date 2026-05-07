package org.example.client.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.example.client.ClientApp;
import org.example.common.model.user.User;

import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label userInfoLabel;

    @FXML
    private Button logoutButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        User currentUser = ClientApp.getCurrentUser();
        if (currentUser != null) {
            welcomeLabel.setText("Chào mừng, " + currentUser.getUsername() + "!");
            userInfoLabel.setText("Vai trò: " + currentUser.getClass().getSimpleName() + " | ID: " + currentUser.getId());
        } else {
            userInfoLabel.setText("Không có thông tin người dùng");
        }
    }

    @FXML
    public void onLogoutClicked() {
        ClientApp.setCurrentUser(null);
        try {
            ClientApp.switchToLogin();
        } catch (Exception e) {
            System.err.println("Error switching to login: " + e.getMessage());
        }
    }
}
