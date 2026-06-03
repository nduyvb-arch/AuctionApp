package org.example.client.controllers.admin; // package chứa controller phía client cho chức năng admin

import javafx.beans.property.SimpleStringProperty; // SimpleStringProperty cho binding giá trị chuỗi trong TableView
import javafx.collections.FXCollections; // FXCollections tiện ích tạo ObservableList
import javafx.collections.ObservableList; // ObservableList để TableView tự động cập nhật khi dữ liệu thay đổi
import javafx.fxml.FXML; // Annotation để liên kết với FXML
import javafx.scene.control.Alert; // Alert dùng để hiển thị dialog tới người dùng
import javafx.scene.control.Button; // Button UI control
import javafx.scene.control.ButtonType; // ButtonType (OK, CANCEL, ... ) cho Alert
import javafx.scene.control.TableColumn; // TableColumn biểu diễn cột trong TableView
import javafx.scene.control.TableView; // TableView để hiển thị danh sách Item
import org.example.common.Message; // Message dùng để giao tiếp client <-> server
import org.example.common.model.item.AuctionStatus; // Enum trạng thái phiên đấu giá
import org.example.common.model.item.Item; // Model Item đại diện cho một phiên đấu giá / món hàng

import java.text.NumberFormat; // NumberFormat để format số (tiền tệ)
import java.time.format.DateTimeFormatter; // DateTimeFormatter để format thời gian
import java.util.ArrayList; // ArrayList triển khai List
import java.util.List; // Interface List
import java.util.Locale; // Locale để định dạng theo vùng (vi_VN)
import java.util.Optional; // Optional để xử lý kết quả dialog showAndWait()
import java.util.stream.Collectors; // Collectors để thu thập kết quả stream

public class AdminAuctionsController implements AdminChildController { // Controller cho màn hình quản lý phiên đấu giá (Admin)

    @FXML private TableView<Item> auctionsTable; // TableView hiển thị danh sách Item
    @FXML private TableColumn<Item, String> idColumn; // Cột hiển thị ID của Item
    @FXML private TableColumn<Item, String> nameColumn; // Cột hiển thị tên Item
    @FXML private TableColumn<Item, String> priceColumn; // Cột hiển thị giá hiện tại của Item
    @FXML private TableColumn<Item, String> winnerColumn; // Cột hiển thị ID người đang dẫn đầu (winner)
    @FXML private TableColumn<Item, String> endTimeColumn; // Cột hiển thị thời gian kết thúc phiên đấu giá
    @FXML private TableColumn<Item, String> statusColumn; // Cột hiển thị trạng thái (ACTIVE, CLOSED,...)
    @FXML private Button cancelButton; // Nút để admin hủy phiên đấu giá
    @FXML private Button endButton; // Nút để admin kết thúc phiên đấu giá ngay lập tức

    private AdminDashboardController dashboardController; // Tham chiếu tới dashboard chính để gửi message và gọi UI helper
    private final ObservableList<Item> displayedAuctions = FXCollections.observableArrayList(); // Danh sách đang hiển thị trên TableView
    private List<Item> allItems = new ArrayList<>(); // Lưu toàn bộ items lấy từ server
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN")); // Formatter số theo locale Việt Nam
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"); // Formatter thời gian

    @Override
    public void setup(AdminDashboardController dashboardController) { // Thiết lập controller con khi được khởi tạo bởi dashboard
        this.dashboardController = dashboardController; // Lưu tham chiếu dashboard
        setupTable(); // Cấu hình TableView và các cell
        requestAuctions(); // Yêu cầu server gửi danh sách phiên đấu giá
    }

    private void setupTable() { // Cấu hình cell factory cho từng cột và liên kết dữ liệu
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId())); // Cell hiển thị ID
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getItemName())); // Cell hiển thị tên
        priceColumn.setCellValueFactory(data -> new SimpleStringProperty(currencyFormat.format(data.getValue().getCurrentPrice()) + " VNĐ")); // Cell hiển thị giá, format theo locale rồi thêm VNĐ
        winnerColumn.setCellValueFactory(data -> new SimpleStringProperty(nullToDash(data.getValue().getCurrentWinnerId()))); // Cell hiển thị winner hoặc '-' nếu null
        endTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEndTime() == null ? "-" : data.getValue().getEndTime().format(dateTimeFormatter))); // Cell hiển thị thời gian kết thúc hoặc '-'
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(formatStatus(data.getValue().getStatus()))); // Cell hiển thị trạng thái dưới dạng chuỗi
        auctionsTable.setItems(displayedAuctions); // Gán source cho TableView
        auctionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> updateActionButtons()); // Listener khi selection thay đổi
        updateActionButtons(); // Cập nhật trạng thái nút lần đầu
    }

    @FXML
    private void onRefreshClicked() { // Handler khi click nút Refresh
        requestAuctions(); // Gửi yêu cầu lấy dữ liệu mới từ server
    }

    @FXML
    private void onShowActiveClicked() { // Handler hiển thị chỉ các phiên đang ACTIVE
        displayedAuctions.setAll(allItems.stream() // Lọc allItems bằng stream
                .filter(item -> item.getStatus() == AuctionStatus.ACTIVE) // Giữ các item có status = ACTIVE
                .collect(Collectors.toList())); // Thu về List và set vào displayedAuctions
        updateActionButtons(); // Cập nhật trạng thái nút
    }

    @FXML
    private void onShowAllClicked() { // Handler hiển thị tất cả phiên
        displayedAuctions.setAll(allItems); // Gán toàn bộ allItems vào displayedAuctions
        updateActionButtons(); // Cập nhật trạng thái nút
    }

    @FXML
    private void onCancelClicked() { // Handler khi admin bấm Hủy phiên
        Item selected = auctionsTable.getSelectionModel().getSelectedItem(); // Lấy item đang chọn
        if (selected == null) { // Nếu không có selection thì không làm gì
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION); // Tạo dialog xác nhận
        confirm.setTitle("Hủy phiên đấu giá"); // Tiêu đề dialog
        confirm.setHeaderText("Hủy phiên: " + selected.getItemName()); // Header mô tả item
        confirm.setContentText("Nếu đã có người dẫn đầu, hệ thống sẽ hoàn tiền cho người đó. Bạn chắc chắn muốn hủy?"); // Nội dung cảnh báo
        Optional<ButtonType> result = confirm.showAndWait(); // Hiển thị và chờ kết quả

        if (result.isPresent() && result.get() == ButtonType.OK) { // Nếu người dùng xác nhận OK
            dashboardController.sendMessage(new Message("CANCEL_AUCTION_ADMIN", selected.getId())); // Gửi yêu cầu hủy phiên tới server
        }
    }

    @FXML
    private void onEndClicked() { // Handler khi admin bấm Kết thúc phiên
        Item selected = auctionsTable.getSelectionModel().getSelectedItem(); // Lấy item đang chọn
        if (selected == null) { // Nếu không có selection thì dừng
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION); // Tạo dialog xác nhận
        confirm.setTitle("Kết thúc phiên đấu giá"); // Tiêu đề dialog
        confirm.setHeaderText("Kết thúc phiên: " + selected.getItemName()); // Header mô tả item
        confirm.setContentText("Nếu có người thắng, tiền đang giữ sẽ được chuyển cho người bán. Bạn chắc chắn muốn kết thúc?"); // Nội dung cảnh báo
        Optional<ButtonType> result = confirm.showAndWait(); // Hiển thị và chờ kết quả

        if (result.isPresent() && result.get() == ButtonType.OK) { // Nếu xác nhận OK
            dashboardController.sendMessage(new Message("END_AUCTION_ADMIN", selected.getId())); // Gửi yêu cầu kết thúc phiên tới server
        }
    }

    @Override
    public void handleServerMessage(Message message) { // Xử lý các message trả về từ server
        switch (message.getAction()) { // Phân loại theo action
            case "GET_ALL_ITEMS_ADMIN_RESPONSE": // Server trả về danh sách items cho admin
                allItems = castList(message.getPayload()); // Cast payload thành List<Item>
                displayedAuctions.setAll(allItems); // Hiển thị tất cả items
                updateActionButtons(); // Cập nhật nút
                break;
            case "CANCEL_AUCTION_RESPONSE":
            case "END_AUCTION_ADMIN_RESPONSE": // Phản hồi từ server sau khi hủy/kết thúc
                dashboardController.showInfo("Quản lý phiên đấu giá", String.valueOf(message.getPayload())); // Hiển thị thông báo kết quả
                requestAuctions(); // Làm mới danh sách
                break;
            case "ITEM_UPDATE":
            case "NEW_ITEM_ADDED": // Khi có thay đổi item hoặc item mới
                requestAuctions(); // Yêu cầu dữ liệu mới (đơn giản nhưng an toàn)
                break;
            default:
                break; // Các action khác không được xử lý ở đây
        }
    }

    private void requestAuctions() { // Gửi message yêu cầu server trả về tất cả items
        dashboardController.sendMessage(new Message("GET_ALL_ITEMS_ADMIN", null)); // payload null
    }

    private void updateActionButtons() { // Cập nhật trạng thái enable/disable của các nút hành động
        Item selected = auctionsTable == null ? null : auctionsTable.getSelectionModel().getSelectedItem(); // Lấy item đang chọn, an toàn nếu auctionsTable chưa inject
        boolean active = selected != null && selected.getStatus() == AuctionStatus.ACTIVE; // Chỉ enable khi item đang ACTIVE
        if (cancelButton != null) { // Nếu cancelButton đã được inject
            cancelButton.setDisable(!active); // Vô hiệu hóa nếu không active
        }
        if (endButton != null) { // Nếu endButton đã được inject
            endButton.setDisable(!active); // Vô hiệu hóa nếu không active
        }
    }

    private String nullToDash(String value) { // Trả '-' nếu giá trị null hoặc trắng
        return value == null || value.isBlank() ? "-" : value; // isBlank() kiểm tra cả whitespace
    }

    private String formatStatus(AuctionStatus status) { // Chuyển enum AuctionStatus sang chuỗi mô tả tiếng Việt
        if (status == null) { // Nếu null
            return "Không rõ"; // Trả chuỗi mặc định
        }
        switch (status) { // Map từng giá trị enum sang chuỗi
            case PENDING:
                return "Chờ bắt đầu";
            case ACTIVE:
                return "Đang diễn ra";
            case CLOSED:
                return "Đã kết thúc";
            case CANCELED:
                return "Đã hủy";
            default:
                return status.name(); // Nếu có enum mới, trả tên enum để không bị null
        }
    }

    @SuppressWarnings("unchecked")
    private List<Item> castList(Object payload) { // Cố gắng ép payload thành List<Item>
        if (payload instanceof List<?>) { // Nếu payload là List ở runtime
            return (List<Item>) payload; // Unsafe cast nhưng suppressed
        }
        return new ArrayList<>(); // Nếu không phải List, trả về list rỗng để an toàn
    }
}
