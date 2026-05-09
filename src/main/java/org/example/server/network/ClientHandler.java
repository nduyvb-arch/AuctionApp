package org.example.server.network;

import org.example.server.manager.AuctionManager;
import org.example.server.manager.UserManager;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;
import org.example.common.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable, Observer {
    private Socket clientSocket;
    private AuctionNotifier notifier;

    //Object Stream để truyền nhận Message
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User currentUser;

    public ClientHandler(Socket clientSocket, AuctionNotifier notifier) {
        this.clientSocket = clientSocket;
        this.notifier = notifier;
    }

    @Override
    public void run() {
        try {
            // Mở OutputStream trước InputStream để tránh deadlock
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            // Đăng ký observer này để nhận thông báo broadcast
            notifier.registerObserver(this);

            Message inputMessage;
            // Lắng nghe các tin nhắn từ Client
            while ((inputMessage = (Message) in.readObject()) != null) {
                System.out.println("Nhận được lệnh từ Client: " + inputMessage.getAction());

                // Xử lý yêu cầu theo action
                switch (inputMessage.getAction()) {

                    case "LOGIN":
                        // 1. Lấy dữ liệu (Client gửi String[] chứa {username, password})
                        String[] loginData = (String[]) inputMessage.getPayload();
                        // 2. Nhờ UserManager xác thực
                        User loggedInUser = UserManager.getInstance().login(loginData[0], loginData[1]);
                        currentUser = loggedInUser; // Lưu user hiện tại
                        // 3. Gửi trả kết quả (Gửi cả object User nếu thành công, hoặc null nếu thất bại)
                        out.writeObject(new Message("LOGIN_RESPONSE", loggedInUser));
                        break;

                    case "REGISTER":
                        // 1. Lấy dữ liệu (Client gửi String[] chứa {username, password, role})
                        String[] regData = (String[]) inputMessage.getPayload();

                        // 2. Gọi UserManager để xử lý đăng ký
                        String regResult = UserManager.getInstance().createAccount(regData[0], regData[1], regData[2]);

                        // 3. Gửi kết quả về cho Client
                        out.writeObject(new Message("REGISTER_RESPONSE", regResult));
                        break;

                    case "SWITCH_ROLE":
                        // 1. Lấy vai trò mới từ payload
                        String newRole = (String) inputMessage.getPayload();

                        // 2. Cập nhật vai trò trong database (qua UserManager)
                        if (currentUser != null) {
                            String roleUpdateResult = UserManager.getInstance().updateUserRole(currentUser.getId(), newRole);
                            boolean success = roleUpdateResult.equals("Cập nhật quyền thành công!");

                            if (success) {
                                // Cập nhật currentUser
                                currentUser.setRole(newRole);
                                // 3. Gửi phản hồi thành công
                                out.writeObject(new Message("SWITCH_ROLE_RESPONSE", "success"));
                                System.out.println("Người dùng " + currentUser.getUsername() + " đã đổi vai trò thành: " + newRole);
                            } else {
                                out.writeObject(new Message("SWITCH_ROLE_RESPONSE", "Lỗi: Không thể cập nhật vai trò"));
                            }
                        } else {
                            out.writeObject(new Message("SWITCH_ROLE_RESPONSE", "Lỗi: Chưa đăng nhập"));
                        }
                        break;

                    case "BID":
                        // 1. Lấy dữ liệu (Client gửi Object[] chứa {itemId, bidAmount, bidderId})
                        Object[] bidData = (Object[]) inputMessage.getPayload();
                        String itemId = (String) bidData[0];
                        double bidAmount = (Double) bidData[1];
                        String bidderId = (String) bidData[2];

                        // 2. Xử lý đặt giá thông qua AuctionManager
                        String bidResult = AuctionManager.getInstance().placeBid(itemId, bidAmount, bidderId);

                        // 3. Gửi phản hồi về cho client vừa đặt giá
                        out.writeObject(new Message("BID_RESPONSE", bidResult));

                        // 4. Nếu đặt giá thành công, thông báo cho TẤT CẢ client khác
                        if (bidResult.startsWith("Đặt giá thành công")) {
                            Item updatedItem = AuctionManager.getInstance().getAllItems().stream()
                                    .filter(i -> i.getId().equals(itemId)).findFirst().orElse(null);
                            if (updatedItem != null) {
                                // Gửi thông báo cập nhật vật phẩm đến tất cả mọi người
                                notifier.notifyObservers(new Message("ITEM_UPDATE", updatedItem));
                            }
                        }
                        break;

                    case "GET_ALL_ITEMS":
                        // Trả về một ArrayList chứa các object Item
                        // LƯU Ý: Class Item và các class con PHẢI implement Serializable
                        out.writeObject(new Message("GET_ALL_ITEMS_RESPONSE", new ArrayList<>(AuctionManager.getInstance().getAllItems())));
                        break;

                    case "START_AUCTION":
                        // 1. Lấy dữ liệu (Frontend sẽ gửi Object[] chứa: {itemId, durationInMinutes})
                        Object[] startData = (Object[]) inputMessage.getPayload();
                        String sItemId = (String) startData[0];
                        int duration = (Integer) startData[1];

                        // 2. Gọi Manager xử lý logic mở phiên, tự động set EndTime và lưu DB
                        String startResult = AuctionManager.getInstance().startAuction(sItemId, duration);

                        // 3. Phản hồi cho người bấm nút
                        out.writeObject(new Message("START_AUCTION_RESPONSE", startResult));

                        // 4. Nếu mở thành công, broadcast cho tất cả Client biết để hiện đếm ngược thời gian
                        if (startResult.startsWith("Đã bắt đầu")) {
                            notifier.notifyObservers(new Message("SYSTEM_NOTIFICATION", startResult));

                            // (Tùy chọn nâng cao): Nếu Frontend muốn tự động load lại Item khi nó đổi trạng thái,
                            // em có thể bắn kèm chính cái Object Item đó đi
                            Item startedItem = AuctionManager.getInstance().getAllItems().stream()
                                    .filter(i -> i.getId().equals(sItemId)).findFirst().orElse(null);
                            if (startedItem != null) {
                                notifier.notifyObservers(new Message("ITEM_UPDATE", startedItem));
                            }
                        }
                        break;

                    case "ADD_ITEM":
                        // 1. Lấy dữ liệu (Frontend sẽ gửi Object[] chứa: {type, name, describe, startingPrice, bidIncrement, sellerId})
                        Object[] itemData = (Object[]) inputMessage.getPayload();
                        String type = (String) itemData[0];
                        String name = (String) itemData[1];
                        String desc = (String) itemData[2];
                        double startPrice = (Double) itemData[3];
                        double increment = (Double) itemData[4];
                        String sellerId = (String) itemData[5]; // Lưu ID người bán để sau này quản lý

                        // 2. Khởi tạo đối tượng Item tương ứng
                        Item newItem;
                        switch (type.toLowerCase()) {
                            case "art":
                                newItem = new org.example.common.model.item.Art(name, type, desc, startPrice, increment);
                                break;
                            case "vehicle":
                                newItem = new org.example.common.model.item.Vehicle(name, type, desc, startPrice, increment);
                                break;
                            default:
                                newItem = new org.example.common.model.item.Electronic(name, type, desc, startPrice, increment);
                                break;
                        }

                        // 3. Gọi Manager để lưu vào RAM và DB
                        AuctionManager.getInstance().addItem(newItem);

                        // 4. Trả phản hồi về cho Seller
                        out.writeObject(new Message("ADD_ITEM_RESPONSE", "Đăng sản phẩm thành công! Mã SP: " + newItem.getId()));

                        // 5. Bắn thông báo Realtime cho toàn Server biết có hàng mới (để Frontend các Client khác tự refresh danh sách)
                        notifier.notifyObservers(new Message("SYSTEM_NOTIFICATION", "Có sản phẩm mới vửa lên sàn: [" + name + "]"));
                        break;

                    default:
                        out.writeObject(new Message("ERROR", "Lệnh không hợp lệ!"));
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Client đã ngắt kết nối: " + e.getMessage());
        } finally {
            try {
                // Khi Client thoát, gỡ họ ra khỏi danh sách nhận thông báo
                notifier.removeObserver(this);
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void update(Message message) {
        // Gửi thông báo Broadcast đến Client này
        if (out != null) {
            try {
                // Gửi thông báo đến Client
                // Gửi nguyên message gốc mà notifier đã gửi đi (ví dụ: ITEM_UPDATE)
                out.writeObject(message);
                out.flush(); // Đảm bảo dữ liệu được gửi đi ngay lập tức
            } catch (IOException e) {
                System.err.println("Không thể gửi thông báo tới Client");
            }
        }
    }
}