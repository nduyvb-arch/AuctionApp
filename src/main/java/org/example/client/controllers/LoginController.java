package org.example.client.controllers;

import org.example.client.ClientApp;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.example.common.Message;
import org.example.common.model.user.Admin;
import org.example.common.model.user.User;

import java.net.URL;
import java.text.Normalizer;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private CheckBox rememberCheckbox;

    @FXML
    private Button loginButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        errorLabel.setText("");
    }

    @FXML
    public void onLoginButtonClicked() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập tên đăng nhập và mật khẩu");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Đang đăng nhập...");

        Task<LoginResult> loginTask = new Task<>() {
            @Override
            protected LoginResult call() throws Exception {
                if (ClientApp.getOutputStream() == null) {
                    ClientApp.connectToServer();
                    ClientApp.startServerListenerIfNeeded();
                }

                var out = ClientApp.getOutputStream();
                var in = ClientApp.getInputStream();

                String[] loginData = {username, password};
                Message loginMsg = new Message("LOGIN", loginData);

                out.writeObject(loginMsg);
                out.flush();

                Message responseMsg = (Message) in.readObject();

                if (responseMsg != null && "LOGIN_RESPONSE".equals(responseMsg.getAction())) {
                    return parseLoginResponse(responseMsg.getPayload());
                }

                return new LoginResult(null, null);
            }
        };

        loginTask.setOnSucceeded(event -> Platform.runLater(() -> {
            LoginResult result = loginTask.getValue();
            User user = result.user;

            if (user == null) {
                showError("Tài khoản không tồn tại, sai mật khẩu, hoặc đã bị khóa!");
                resetLoginButton();
                return;
            }

            ClientApp.setCurrentUser(user);

            String objectRole = normalizeRole(user.getRole());
            String dbRole = normalizeRole(result.roleFromServer);

            System.out.println("Đăng nhập thành công: " + user.getUsername());
            System.out.println("Role object: " + objectRole + " | Role server/db: " + dbRole);

            try {
                if (user instanceof Admin || "admin".equals(objectRole) || "admin".equals(dbRole)) {
                    ClientApp.setSelectedRole("admin");
                    ClientApp.switchToAdmin();
                } else {
                    ClientApp.setSelectedRole("bidder");
                    ClientApp.switchToRoleSelection();
                }
            } catch (Exception e) {
                showError("Lỗi chuyển màn hình: " + e.getMessage());
                resetLoginButton();
            }
        }));

        loginTask.setOnFailed(event -> Platform.runLater(() -> {
            showError("Lỗi kết nối tới Server: Server chưa mở hoặc mạng rớt!");
            resetLoginButton();
        }));

        new Thread(loginTask).start();
    }

    private void resetLoginButton() {
        loginButton.setDisable(false);
        loginButton.setText("Đăng nhập");
    }

    private LoginResult parseLoginResponse(Object payload) {
        if (payload instanceof Object[]) {
            Object[] data = (Object[]) payload;
            User user = data.length > 0 && data[0] instanceof User ? (User) data[0] : null;
            String roleFromServer = data.length > 1 && data[1] != null ? String.valueOf(data[1]) : null;
            return new LoginResult(user, roleFromServer);
        }

        if (payload instanceof User) {
            return new LoginResult((User) payload, null);
        }

        return new LoginResult(null, null);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }

        String normalizedRole = Normalizer.normalize(role, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase()
                .replace("đ", "d")
                .replace("_", "")
                .replace("-", "")
                .replaceAll("[\\s\u00A0]+", "");

        if ("administrator".equals(normalizedRole)
                || "superadmin".equals(normalizedRole)
                || "root".equals(normalizedRole)
                || "quantri".equals(normalizedRole)
                || "quantrivien".equals(normalizedRole)) {
            return "admin";
        }

        return normalizedRole;
    }

    @FXML
    public void onSignUpButtonClicked() {
        try {
            ClientApp.switchToSignUp();
        } catch (Exception e) {
            System.err.println("Error switching to sign up: " + e.getMessage());
        }
    }

    @FXML
    public void onForgotPasswordClicked() {
        showError("Chức năng đang được nâng cấp!");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12;");

        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(event -> errorLabel.setText(""));
        pause.play();
    }

    private static class LoginResult {
        private final User user;
        private final String roleFromServer;

        private LoginResult(User user, String roleFromServer) {
            this.user = user;
            this.roleFromServer = roleFromServer;
        }
    }
}
