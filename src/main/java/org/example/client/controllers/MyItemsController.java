// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package org.example.client.controllers;

import java.io.ObjectOutputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Font;
import org.example.common.Message;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;

public class MyItemsController implements Initializable {
    @FXML
    private TextField myItemsSearchTextField;
    @FXML
    private ComboBox<String> myItemsStatusComboBox;
    @FXML
    private ComboBox<String> myItemsSortComboBox;
    @FXML
    private FlowPane myItemsFlowPane;
    @FXML
    private Label myItemsSummaryLabel;
    private List<Item> items = new ArrayList();
    private ObjectOutputStream out;
    private User currentUser;
    private Runnable onItemsChanged;
    private static final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi_VN"));

    public MyItemsController() {
    }

    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.myItemsStatusComboBox.getItems().setAll(new String[]{"Tất cả", "Chờ", "Đang diễn ra", "Đã kết thúc", "Bị hủy"});
        this.myItemsStatusComboBox.setValue("Tất cả");
        this.myItemsSortComboBox.getItems().setAll(new String[]{"Mặc định", "Giá thấp → cao", "Giá cao → thấp", "Sắp hết hạn"});
        this.myItemsSortComboBox.setValue("Mặc định");
        this.myItemsSearchTextField.setOnKeyReleased((e) -> this.refreshMyItemsView());
        this.myItemsStatusComboBox.setOnAction((e) -> this.refreshMyItemsView());
        this.myItemsSortComboBox.setOnAction((e) -> this.refreshMyItemsView());
    }

    public void setup(List<Item> items, ObjectOutputStream out, User currentUser, Runnable onItemsChanged) {
        this.items = items;
        this.out = out;
        this.currentUser = currentUser;
        this.onItemsChanged = onItemsChanged;
        this.refreshMyItemsView();
    }

    public void updateData(List<Item> items) {
        this.items = items;
        this.refreshMyItemsView();
    }

    @FXML
    private void onMyItemsRefreshClicked() {
        if (this.onItemsChanged != null) {
            this.onItemsChanged.run();
        }

        this.refreshMyItemsView();
    }

    public void refreshMyItemsView() {
        this.myItemsFlowPane.getChildren().clear();
        if (this.currentUser == null) {
            this.myItemsFlowPane.getChildren().add(this.createEmptyLabel("Bạn chưa đăng nhập."));
        } else {
            String search = this.myItemsSearchTextField.getText() == null ? "" : this.myItemsSearchTextField.getText().toLowerCase();
            String status = (String)this.myItemsStatusComboBox.getValue();
            String sort = (String)this.myItemsSortComboBox.getValue();
            List<Item> filtered = (List)this.items.stream().filter((itemx) -> this.currentUser.getId().equals(itemx.getSellerId())).filter((itemx) -> itemx.getItemName().toLowerCase().contains(search)).filter((itemx) -> this.applyStatusFilter(itemx, status)).collect(Collectors.toList());
            this.applySorting(filtered, sort);
            this.myItemsSummaryLabel.setText("Tổng: " + filtered.size() + " sản phẩm");
            if (filtered.isEmpty()) {
                this.myItemsFlowPane.getChildren().add(this.createEmptyLabel("\ud83d\udce6 Bạn chưa có sản phẩm nào. Hãy sang mục Đăng sản phẩm mới."));
            } else {
                for(Item item : filtered) {
                    this.myItemsFlowPane.getChildren().add(this.createItemCard(item));
                }

            }
        }
    }

    private boolean applyStatusFilter(Item item, String filter) {
        if ("Tất cả".equals(filter)) {
            return true;
        } else if ("Chờ".equals(filter)) {
            return "PENDING".equals(item.getStatus().name());
        } else if ("Đang diễn ra".equals(filter)) {
            return "ACTIVE".equals(item.getStatus().name());
        } else if ("Đã kết thúc".equals(filter)) {
            return "CLOSED".equals(item.getStatus().name());
        } else {
            return "Bị hủy".equals(filter) ? "CANCELED".equals(item.getStatus().name()) : true;
        }
    }

    private void applySorting(List<Item> itemList, String sortOption) {
        switch (sortOption) {
            case "Sắp hết hạn":
                itemList.sort((a, b) -> {
                    if (a.getEndTime() == null) {
                        return 1;
                    } else {
                        return b.getEndTime() == null ? -1 : a.getEndTime().compareTo(b.getEndTime());
                    }
                });
                break;
            case "Giá thấp → cao":
                itemList.sort(Comparator.comparingDouble(Item::getCurrentPrice));
                break;
            case "Giá cao → thấp":
                itemList.sort(Comparator.comparingDouble(Item::getCurrentPrice).reversed());
        }

    }

    private Node createItemCard(Item item) {
        AnchorPane pane = new AnchorPane();
        pane.setPrefSize((double)255.0F, (double)305.0F);
        pane.setMinSize((double)255.0F, (double)305.0F);
        pane.setMaxSize((double)255.0F, (double)305.0F);
        pane.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-padding: 15;");
        Label statusBadge = new Label(this.getStatusText(item.getStatus().name()));
        String var10001 = this.getStatusColor(item.getStatus().name());
        statusBadge.setStyle("-fx-background-color: " + var10001 + "; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 6; -fx-font-size: 10;");
        AnchorPane.setTopAnchor(statusBadge, (double)12.0F);
        AnchorPane.setRightAnchor(statusBadge, (double)12.0F);
        Label nameLabel = new Label(item.getItemName());
        nameLabel.setFont(new Font("System Bold", (double)14.0F));
        nameLabel.setWrapText(true);
        AnchorPane.setTopAnchor(nameLabel, (double)50.0F);
        AnchorPane.setLeftAnchor(nameLabel, (double)12.0F);
        AnchorPane.setRightAnchor(nameLabel, (double)12.0F);
        Label typeLabel = new Label("Loại: " + item.getType());
        typeLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");
        AnchorPane.setTopAnchor(typeLabel, (double)96.0F);
        AnchorPane.setLeftAnchor(typeLabel, (double)12.0F);
        Label descLabel = new Label(item.getDescription() != null && !item.getDescription().isEmpty() ? item.getDescription() : "Không có mô tả");
        descLabel.setWrapText(true);
        descLabel.setPrefHeight((double)45.0F);
        descLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10;");
        AnchorPane.setTopAnchor(descLabel, (double)118.0F);
        AnchorPane.setLeftAnchor(descLabel, (double)12.0F);
        AnchorPane.setRightAnchor(descLabel, (double)12.0F);
        Label priceLabel = new Label("Giá hiện tại");
        priceLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");
        AnchorPane.setTopAnchor(priceLabel, (double)170.0F);
        AnchorPane.setLeftAnchor(priceLabel, (double)12.0F);
        NumberFormat var10002 = currencyFormat;
        Label priceValueLabel = new Label(var10002.format(item.getCurrentPrice()) + " VNĐ");
        priceValueLabel.setFont(new Font("System Bold", (double)14.0F));
        priceValueLabel.setStyle("-fx-text-fill: #2563eb;");
        AnchorPane.setTopAnchor(priceValueLabel, (double)188.0F);
        AnchorPane.setLeftAnchor(priceValueLabel, (double)12.0F);
        Label winnerLabel = new Label(item.getCurrentWinnerId() == null ? "Chưa có người đặt giá" : "Người thắng hiện tại: #" + item.getCurrentWinnerId());
        winnerLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");
        AnchorPane.setTopAnchor(winnerLabel, (double)214.0F);
        AnchorPane.setLeftAnchor(winnerLabel, (double)12.0F);
        AnchorPane.setRightAnchor(winnerLabel, (double)12.0F);
        Button startButton = new Button("▶ Bắt đầu đấu giá");
        startButton.setDisable(!"PENDING".equals(item.getStatus().name()));
        startButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 9 12; -fx-cursor: hand;");
        startButton.setOnAction((e) -> this.startAuction(item));
        AnchorPane.setBottomAnchor(startButton, (double)12.0F);
        AnchorPane.setLeftAnchor(startButton, (double)12.0F);
        AnchorPane.setRightAnchor(startButton, (double)12.0F);
        pane.getChildren().addAll(new Node[]{statusBadge, nameLabel, typeLabel, descLabel, priceLabel, priceValueLabel, winnerLabel, startButton});
        return pane;
    }

    private void startAuction(Item item) {
        if (this.out != null) {
            TextInputDialog dialog = new TextInputDialog("60");
            dialog.setTitle("Bắt đầu đấu giá");
            dialog.setHeaderText("Bắt đầu phiên đấu giá cho: " + item.getItemName());
            dialog.setContentText("Thời gian đấu giá (phút):");
            dialog.showAndWait().ifPresent((value) -> {
                try {
                    int duration = Integer.parseInt(value.trim());
                    if (duration <= 0) {
                        throw new NumberFormatException();
                    }

                    synchronized(this.out) {
                        this.out.writeObject(new Message("START_AUCTION", new Object[]{item.getId(), duration}));
                        this.out.flush();
                    }

                    this.showInfo("Đã gửi yêu cầu bắt đầu đấu giá. Vui lòng bấm Làm mới sau khi server cập nhật.");
                    if (this.onItemsChanged != null) {
                        this.onItemsChanged.run();
                    }
                } catch (NumberFormatException var6) {
                    this.showInfo("Thời gian đấu giá phải là số nguyên lớn hơn 0.");
                } catch (Exception ex) {
                    this.showInfo("Lỗi khi gửi yêu cầu: " + ex.getMessage());
                }

            });
        }
    }

    private Label createEmptyLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14;");
        label.setPadding(new Insets((double)20.0F));
        return label;
    }

    private void showInfo(String text) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setHeaderText((String)null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    private String getStatusText(String status) {
        switch (status) {
            case "PENDING":
                return "Chờ";
            case "CANCELED":
                return "Bị hủy";
            case "ACTIVE":
                return "Đang diễn ra";
            case "CLOSED":
                return "Đã kết thúc";
        }

        return status;
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "PENDING":
                return "#94a3b8";
            case "CANCELED":
                return "#ef4444";
            case "ACTIVE":
                return "#10b981";
            case "CLOSED":
                return "#8b5cf6";
        }

        return "#64748b";
    }
}
