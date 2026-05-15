package org.example.server.network;

import org.example.server.manager.AuctionManager;
import org.example.server.manager.UserManager;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;
import org.example.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable, Observer {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private Socket clientSocket;
    private AuctionNotifier notifier;

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
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            notifier.registerObserver(this);

            Message inputMessage;
            while ((inputMessage = (Message) in.readObject()) != null) {
                logger.info("Nhận được lệnh từ Client: {}", inputMessage.getAction());

                switch (inputMessage.getAction()) {

                    case "LOGIN":
                        String[] loginData = (String[]) inputMessage.getPayload();
                        User loggedInUser = UserManager.getInstance().login(loginData[0], loginData[1]);
                        currentUser = loggedInUser;
                        out.writeObject(new Message("LOGIN_RESPONSE", loggedInUser));
                        break;

                    case "REGISTER":
                        String[] regData = (String[]) inputMessage.getPayload();
                        String regResult = UserManager.getInstance().createAccount(regData[0], regData[1], regData[2]);
                        out.writeObject(new Message("REGISTER_RESPONSE", regResult));
                        break;

                    case "SWITCH_ROLE":
                        String newRole = (String) inputMessage.getPayload();
                        if (currentUser != null) {
                            String roleUpdateResult = UserManager.getInstance().updateUserRole(currentUser.getId(), newRole);
                            boolean success = roleUpdateResult.equals("Cập nhật quyền thành công!");

                            if (success) {
                                currentUser.setRole(newRole);
                                out.writeObject(new Message("SWITCH_ROLE_RESPONSE", "success"));
                                // SỬA: Dùng Logger
                                logger.info("Người dùng {} đã đổi vai trò thành: {}", currentUser.getUsername(), newRole);
                            } else {
                                out.writeObject(new Message("SWITCH_ROLE_RESPONSE", "Lỗi: Không thể cập nhật vai trò"));
                            }
                        } else {
                            out.writeObject(new Message("SWITCH_ROLE_RESPONSE", "Lỗi: Chưa đăng nhập"));
                        }
                        break;

                    case "BID":
                        Object[] bidData = (Object[]) inputMessage.getPayload();
                        String itemId = (String) bidData[0];
                        double bidAmount = (Double) bidData[1];
                        String bidderId = (String) bidData[2];

                        String bidResult = AuctionManager.getInstance().placeBid(itemId, bidAmount, bidderId);
                        out.writeObject(new Message("BID_RESPONSE", bidResult));

                        if (bidResult.startsWith("Đặt giá thành công")) {
                            Item updatedItem = AuctionManager.getInstance().getAllItems().stream()
                                    .filter(i -> i.getId().equals(itemId)).findFirst().orElse(null);
                            if (updatedItem != null) {
                                notifier.notifyObservers(new Message("ITEM_UPDATE", updatedItem));
                            }
                        }
                        break;

                    case "GET_ALL_ITEMS":
                        out.writeObject(new Message("GET_ALL_ITEMS_RESPONSE", new ArrayList<>(AuctionManager.getInstance().getAllItems())));
                        break;

                    case "START_AUCTION":
                        Object[] startData = (Object[]) inputMessage.getPayload();
                        String sItemId = (String) startData[0];
                        int duration = (Integer) startData[1];

                        String startResult = AuctionManager.getInstance().startAuction(sItemId, duration);
                        out.writeObject(new Message("START_AUCTION_RESPONSE", startResult));

                        if (startResult.startsWith("Đã bắt đầu")) {
                            notifier.notifyObservers(new Message("SYSTEM_NOTIFICATION", startResult));
                            Item startedItem = AuctionManager.getInstance().getAllItems().stream()
                                    .filter(i -> i.getId().equals(sItemId)).findFirst().orElse(null);
                            if (startedItem != null) {
                                notifier.notifyObservers(new Message("ITEM_UPDATE", startedItem));
                            }
                        }
                        break;

                    case "ADD_ITEM":
                        Object[] itemData = (Object[]) inputMessage.getPayload();
                        String type = (String) itemData[0];
                        String name = (String) itemData[1];
                        String desc = (String) itemData[2];
                        double startPrice = (Double) itemData[3];
                        double increment = (Double) itemData[4];
                        String sellerId = (String) itemData[5];

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

                        AuctionManager.getInstance().addItem(newItem);
                        out.writeObject(new Message("ADD_ITEM_RESPONSE", "Đăng sản phẩm thành công! Mã SP: " + newItem.getId()));
                        notifier.notifyObservers(new Message("SYSTEM_NOTIFICATION", "Có sản phẩm mới vửa lên sàn: [" + name + "]"));
                        break;

                    case "CANCEL_AUCTION":
                        // 1. Chốt chặn quyền Admin
                        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
                            out.writeObject(new Message("CANCEL_AUCTION_RESPONSE", "Cảnh báo: Chỉ admin mới có quyền hủy phiên đấu giá"));
                            break;
                        }

                        // 2. Nhận ID sản phẩm cần hủy
                        String itemToCancelId = (String) inputMessage.getPayload();

                        // 3. Gọi hàm xử lý và LẤY KẾT QUẢ lưu vào biến cancelResult
                        String cancelResult = AuctionManager.getInstance().cancelAuctionByAdmin(itemToCancelId);

                        // 4. Xử lý phản hồi dựa trên kết quả
                        if ("success".equals(cancelResult)) {
                            logger.info("Admin {} đã hủy khẩn cấp phiên đấu giá mã {}", currentUser.getUsername(), itemToCancelId);
                            out.writeObject(new Message("CANCEL_AUCTION_RESPONSE", "Đã hủy phiên đấu giá thành công!"));

                            // Bắn thông báo toàn Server
                            notifier.notifyObservers(new Message("SYSTEM_NOTIFICATION", "⚠️ [THÔNG BÁO TỪ ADMIN] Phiên đấu giá mã " + itemToCancelId + " đã bị hủy bỏ!"));

                            // Cập nhật lại Item để UI vô hiệu hóa nút Đặt Giá
                            Item canceledItem = AuctionManager.getInstance().getAllItems().stream()
                                    .filter(i -> i.getId().equals(itemToCancelId)).findFirst().orElse(null);
                            if (canceledItem != null) {
                                notifier.notifyObservers(new Message("ITEM_UPDATE", canceledItem));
                            }
                        } else {
                            // Báo lỗi nếu việc hủy thất bại
                            out.writeObject(new Message("CANCEL_AUCTION_RESPONSE", cancelResult));
                        }
                        break;

                    default:
                        out.writeObject(new Message("ERROR", "Lệnh không hợp lệ!"));
                        break;
                }
            }
        } catch (Exception e) {
            logger.warn("Client đã ngắt kết nối: {}", e.getMessage());
        } finally {
            try {
                notifier.removeObserver(this);
                clientSocket.close();
            } catch (IOException e) {
                logger.error("Lỗi khi đóng socket", e);
            }
        }
    }

    @Override
    public void update(Message message) {
        if (out != null) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                logger.error("Không thể gửi thông báo tới Client", e);
            }
        }
    }
}