package org.example.client.controllers.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.example.common.Message;
import org.example.common.model.user.User;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminUsersController implements AdminChildController {

    @FXML private TextField searchField;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> balanceColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private Button banButton;
    @FXML private Button unbanButton;

    private AdminDashboardController dashboardController;
    private final ObservableList<User> displayedUsers = FXCollections.observableArrayList();
    private List<User> allUsers = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    public void setup(AdminDashboardController dashboardController) {
        this.dashboardController = dashboardController;
        setupTable();
        requestUsers();
    }

    private void setupTable() {
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(formatRole(data.getValue().getRole())));
        balanceColumn.setCellValueFactory(data -> new SimpleStringProperty(currencyFormat.format(data.getValue().getBalance()) + " VNĐ"));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isBanned() ? "Đã khóa" : "Hoạt động"));
        usersTable.setItems(displayedUsers);

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldUser, newUser) -> updateActionButtons());
        updateActionButtons();
    }

    @FXML
    private void onRefreshClicked() {
        requestUsers();
    }

    @FXML
    private void onSearchChanged() {
        applySearch();
    }

    @FXML
    private void onBanClicked() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if ("admin".equalsIgnoreCase(selected.getRole())) {
            dashboardController.showError("Khóa tài khoản", "Không nên khóa tài khoản admin.");
            return;
        }
        dashboardController.sendMessage(new Message("BAN_USER", selected.getId()));
    }

    @FXML
    private void onUnbanClicked() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        dashboardController.sendMessage(new Message("UNBAN_USER", selected.getId()));
    }

    @Override
    public void handleServerMessage(Message message) {
        switch (message.getAction()) {
            case "GET_ALL_USERS_RESPONSE":
                allUsers = castList(message.getPayload());
                applySearch();
                break;
            case "BAN_USER_RESPONSE":
            case "UNBAN_USER_RESPONSE":
                dashboardController.showInfo("Quản lý người dùng", String.valueOf(message.getPayload()));
                requestUsers();
                break;
            default:
                break;
        }
    }

    private void requestUsers() {
        dashboardController.sendMessage(new Message("GET_ALL_USERS", null));
    }

    private void applySearch() {
        String keyword = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        List<User> filtered = allUsers.stream()
                .filter(user -> keyword.isEmpty()
                        || user.getId().toLowerCase().contains(keyword)
                        || user.getUsername().toLowerCase().contains(keyword)
                        || user.getRole().toLowerCase().contains(keyword))
                .collect(Collectors.toList());

        displayedUsers.setAll(filtered);
        updateActionButtons();
    }

    private void updateActionButtons() {
        User selected = usersTable == null ? null : usersTable.getSelectionModel().getSelectedItem();
        boolean noSelection = selected == null;
        boolean adminSelected = selected != null && "admin".equalsIgnoreCase(selected.getRole());

        if (banButton != null) {
            banButton.setDisable(noSelection || adminSelected || selected.isBanned());
        }
        if (unbanButton != null) {
            unbanButton.setDisable(noSelection || !selected.isBanned());
        }
    }

    private String formatRole(String role) {
        if ("admin".equalsIgnoreCase(role)) {
            return "Admin";
        }
        if ("seller".equalsIgnoreCase(role)) {
            return "Người bán";
        }
        if ("bidder".equalsIgnoreCase(role)) {
            return "Người đấu giá";
        }
        return Optional.ofNullable(role).orElse("Không rõ");
    }

    @SuppressWarnings("unchecked")
    private List<User> castList(Object payload) {
        if (payload instanceof List<?>) {
            return (List<User>) payload;
        }
        return new ArrayList<>();
    }
}
