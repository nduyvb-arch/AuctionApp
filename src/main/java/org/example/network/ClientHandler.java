package org.example.network;

import org.example.manager.UserManager;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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

            // Gửi lời chào khi Client vừa vào
            out.writeObject(new Message("INFO", "Chào mừng bạn đã kết nối vào sàn đấu giá Hệ Thống Mới!"));

            Message inputMessage;
            // Liên tục đón lõng các gói hàng Message từ Client gửi lên
            while ((inputMessage = (Message) in.readObject()) != null) {
                System.out.println("Nhận được lệnh từ Client: " + inputMessage.getAction());

                // Phân luồng xử lý tùy theo thẻ Action sếp gắn trên gói hàng
                switch (inputMessage.getAction()) {

                    case "REGISTER":
                        // 1. Khui hàng (Giả sử Client sẽ gửi lên một mảng 3 chữ: username, password, role)
                        String[] regData = (String[]) inputMessage.getPayload();

                        // 2. Nhờ Quản lý User chọc vào DB để đăng ký
                        String result = UserManager.getInstance().createAccount(regData[0], regData[1], regData[2]);

                        // 3. Đóng gói kết quả gửi trả lại Giao diện
                        out.writeObject(new Message("REGISTER_RESPONSE", result));
                        break;

                    case "BID":
                        // (Phần code cũ của sếp, sau này mình ráp AuctionManager vào đây)
                        String bidInfo = (String) inputMessage.getPayload();
                        System.out.println("Đang xử lý đặt giá: " + bidInfo);
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
                out.writeObject(new Message("NOTIFICATION", message.getPayload()));
            } catch (IOException e) {
                System.err.println("Không thể gửi thông báo tới Client");
            }
        }
    }
}