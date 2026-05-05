package org.example.network;

import org.example.manager.AuctionManager;
import org.example.manager.UserManager;
import org.example.model.item.Item;
import org.example.model.user.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable, Observer {
    private Socket clientSocket;
    private AuctionNotifier notifier;

    // Nâng cấp lên thành Object Stream để chở class Message
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(Socket clientSocket, AuctionNotifier notifier) {
        this.clientSocket = clientSocket;
        this.notifier = notifier;
    }

    @Override
    public void run() {
        try {
            // Nguyên tắc vàng của Java Socket: Luôn mở OutputStream trước InputStream để tránh kẹt mạng (deadlock)
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            Message inputMessage;
            // Liên tục đón lõng các gói hàng Message từ Client gửi lên
            while ((inputMessage = (Message) in.readObject()) != null) {
                System.out.println("Nhận được lệnh từ Client: " + inputMessage.getAction());

                // Phân luồng xử lý tùy theo thẻ Action sếp gắn trên gói hàng
                switch (inputMessage.getAction()) {

                    case "LOGIN":
                        // 1. Khui hàng (Client gửi String[] chứa {username, password})
                        String[] loginData = (String[]) inputMessage.getPayload();
                        // 2. Nhờ UserManager xác thực
                        User loggedInUser = UserManager.getInstance().login(loginData[0], loginData[1]);
                        // 3. Gửi trả kết quả (Gửi cả object User nếu thành công, hoặc null nếu thất bại)
                        // LƯU Ý: Class User và các class con (Bidder, Seller, Admin) PHẢI implement Serializable
                        out.writeObject(new Message("LOGIN_RESPONSE", loggedInUser));
                        break;

                    case "REGISTER":
                        // 1. Khui hàng (Giả sử Client sẽ gửi lên một mảng 3 chữ: username, password, role)
                        String[] regData = (String[]) inputMessage.getPayload();

                        // 2. Nhờ Quản lý User chọc vào DB để đăng ký
                        String regResult = UserManager.getInstance().createAccount(regData[0], regData[1], regData[2]);

                        // 3. Đóng gói kết quả gửi trả lại Giao diện
                        out.writeObject(new Message("REGISTER_RESPONSE", regResult));
                        break;

                    case "BID":
                        // 1. Khui hàng (Client gửi Object[] chứa {itemId, bidAmount, bidderId})
                        Object[] bidData = (Object[]) inputMessage.getPayload();
                        String itemId = (String) bidData[0];
                        double bidAmount = (Double) bidData[1];
                        String bidderId = (String) bidData[2];

                        // 2. Nhờ AuctionManager xử lý việc đặt giá
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
                // Đóng gói thông báo ném thẳng xuống Client
                // Gửi nguyên message gốc mà notifier đã gửi đi (ví dụ: ITEM_UPDATE)
                out.writeObject(message);
                out.flush(); // Đảm bảo dữ liệu được gửi đi ngay lập tức
            } catch (IOException e) {
                System.err.println("Không thể gửi thông báo tới Client");
            }
        }
    }
}