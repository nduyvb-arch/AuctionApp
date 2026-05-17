package org.example.server.network;

import org.example.common.Message;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;
import org.example.server.manager.AuctionManager;
import org.example.server.manager.UserManager;
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
                        sendMessage(new Message("LOGIN_RESPONSE", loggedInUser));
                        break;

                    case "REGISTER":
                        String[] regData = (String[]) inputMessage.getPayload();
                        String regResult = UserManager.getInstance().createAccount(regData[0], regData[1], regData[2]);
                        sendMessage(new Message("REGISTER_RESPONSE", regResult));
                        break;

                    case "SWITCH_ROLE":
                        String newRole = (String) inputMessage.getPayload();

                        if (currentUser != null) {
                            String roleUpdateResult = UserManager.getInstance().updateUserRole(currentUser.getId(), newRole);
                            boolean success = roleUpdateResult.equals("Cập nhật quyền thành công!");

                            if (success) {
                                currentUser.setRole(newRole);
                                sendMessage(new Message("SWITCH_ROLE_RESPONSE", "success"));
                                logger.info("Người dùng {} đã đổi vai trò thành: {}", currentUser.getUsername(), newRole);
                            } else {
                                sendMessage(new Message("SWITCH_ROLE_RESPONSE", "Lỗi: Không thể cập nhật vai trò"));
                            }
                        } else {
                            sendMessage(new Message("SWITCH_ROLE_RESPONSE", "Lỗi: Chưa đăng nhập"));
                        }
                        break;

                    case "BID":
                        Object[] bidData = (Object[]) inputMessage.getPayload();

                        String itemId = (String) bidData[0];
                        double bidAmount = (Double) bidData[1];
                        String bidderId = (String) bidData[2];

                        String bidResult = AuctionManager.getInstance().placeBid(itemId, bidAmount, bidderId);
                        sendMessage(new Message("BID_RESPONSE", bidResult));

                        if (bidResult.startsWith("Đặt giá thành công")) {
                            Item updatedItem = AuctionManager.getInstance().getAllItems().stream()
                                    .filter(i -> i.getId().equals(itemId))
                                    .findFirst()
                                    .orElse(null);

                            if (updatedItem != null) {
                                notifier.notifyObservers(new Message("ITEM_UPDATE", updatedItem));
                            }
                        }
                        break;

                    case "GET_ALL_ITEMS":
                        sendMessage(new Message(
                                "GET_ALL_ITEMS_RESPONSE",
                                new ArrayList<>(AuctionManager.getInstance().getAllItems())
                        ));
                        break;

                    case "START_AUCTION":
                        Object[] startData = (Object[]) inputMessage.getPayload();

                        String sItemId = (String) startData[0];
                        int duration = (Integer) startData[1];

                        String startResult = AuctionManager.getInstance().startAuction(sItemId, duration);
                        sendMessage(new Message("START_AUCTION_RESPONSE", startResult));

                        if (startResult.startsWith("Đã bắt đầu")) {
                            notifier.notifyObservers(new Message("SYSTEM_NOTIFICATION", startResult));

                            Item startedItem = AuctionManager.getInstance().getAllItems().stream()
                                    .filter(i -> i.getId().equals(sItemId))
                                    .findFirst()
                                    .orElse(null);

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

                        /*
                         * itemData[6] là duration do AddItemViewController gửi lên.
                         * Code server hiện tại chưa tự start auction ở đây, nên giữ nguyên luồng cũ.
                         *
                         * itemData[7] là imagePath mới thêm.
                         */
                        String imagePath = null;
                        if (itemData.length > 7 && itemData[7] != null) {
                            imagePath = (String) itemData[7];
                        }

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

                        newItem.setSellerId(sellerId);
                        newItem.setImagePath(imagePath);

                        AuctionManager.getInstance().addItem(newItem);

                        sendMessage(new Message("ADD_ITEM_RESPONSE", "Đăng sản phẩm thành công! Mã SP: " + newItem.getId()));
                        notifier.notifyObservers(new Message("NEW_ITEM_ADDED", null));
                        break;

                    case "CANCEL_AUCTION":
                        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
                            sendMessage(new Message("CANCEL_AUCTION_RESPONSE", "Cảnh báo: Chỉ admin mới có quyền hủy phiên đấu giá"));
                            break;
                        }

                        String itemToCancelId = (String) inputMessage.getPayload();
                        String cancelResult = AuctionManager.getInstance().cancelAuctionByAdmin(itemToCancelId);

                        if ("success".equals(cancelResult)) {
                            logger.info("Admin {} đã hủy khẩn cấp phiên đấu giá mã {}", currentUser.getUsername(), itemToCancelId);

                            sendMessage(new Message("CANCEL_AUCTION_RESPONSE", "Đã hủy phiên đấu giá thành công!"));

                            notifier.notifyObservers(new Message(
                                    "SYSTEM_NOTIFICATION",
                                    "⚠️ [THÔNG BÁO TỪ ADMIN] Phiên đấu giá mã " + itemToCancelId + " đã bị hủy bỏ!"
                            ));

                            Item canceledItem = AuctionManager.getInstance().getAllItems().stream()
                                    .filter(i -> i.getId().equals(itemToCancelId))
                                    .findFirst()
                                    .orElse(null);

                            if (canceledItem != null) {
                                notifier.notifyObservers(new Message("ITEM_UPDATE", canceledItem));
                            }
                        } else {
                            sendMessage(new Message("CANCEL_AUCTION_RESPONSE", cancelResult));
                        }
                        break;

                    default:
                        sendMessage(new Message("ERROR", "Lệnh không hợp lệ!"));
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

    public synchronized void sendMessage(Message message) {
        try {
            if (out != null && !clientSocket.isClosed()) {
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            logger.error("Không thể gửi tin nhắn tới Client {}", clientSocket.getInetAddress(), e);
        }
    }

    @Override
    public void update(Message message) {
        sendMessage(message);
    }
}
