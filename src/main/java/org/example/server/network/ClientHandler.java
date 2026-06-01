package org.example.server.network;

import org.example.common.Message;
import org.example.common.model.item.Item;
import org.example.common.model.chat.AuctionChatMessage;
import org.example.common.model.user.User;
import org.example.server.manager.AuctionManager;
import org.example.server.manager.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable, Observer {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private static final String IMAGE_DIR = "images"; // Thư mục lưu ảnh trong repo
    private static final int MAX_CHAT_HISTORY_PER_ROOM = 100;
    private static final Map<String, List<AuctionChatMessage>> AUCTION_CHAT_HISTORY = new ConcurrentHashMap<>();

    private Socket clientSocket;
    private AuctionNotifier notifier;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User currentUser;

    public ClientHandler(Socket clientSocket, AuctionNotifier notifier) {
        this.clientSocket = clientSocket;
        this.notifier = notifier;
        // Đảm bảo thư mục lưu ảnh tồn tại
        File imageFolder = new File(IMAGE_DIR);
        if (!imageFolder.exists()) {
            imageFolder.mkdirs();
        }
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

                if (isCurrentUserBanned() && !isLoginOrRegisterAction(inputMessage.getAction())) {
                    sendMessage(new Message("FORCE_LOGOUT", currentUser.getId()));
                    break;
                }

                switch (inputMessage.getAction()) {
                    case "LOGIN":
                        String[] loginData = (String[]) inputMessage.getPayload();
                        User loggedInUser = UserManager.getInstance().login(loginData[0], loginData[1]);
                        String normalizedRoleFromDb = UserManager.getInstance().getNormalizedRoleFromDB(loginData[0]);
                        currentUser = loggedInUser;

                        logger.info("LOGIN user={} objectRole={} dbRole={}",
                                loginData[0],
                                loggedInUser == null ? null : loggedInUser.getRole(),
                                normalizedRoleFromDb);

                        sendMessage(new Message("LOGIN_RESPONSE", new Object[]{loggedInUser, normalizedRoleFromDb}));
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
                            User refreshedBidder = UserManager.getInstance().findUserById(bidderId);

                            if (currentUser != null && refreshedBidder != null
                                    && currentUser.getId().equals(refreshedBidder.getId())) {
                                currentUser = refreshedBidder;
                            }

                            if (refreshedBidder != null) {
                                sendMessage(new Message("ACCOUNT_INFO_RESPONSE", refreshedBidder));
                            }

                            /*
                             * Khi có người đặt giá cao hơn, server đã hoàn tiền cho người đang dẫn trước.
                             * Nếu chỉ cập nhật tài khoản cho người vừa đặt, client của người bị vượt sẽ vẫn
                             * hiển thị số dư cũ cho đến khi bấm refresh/đăng nhập lại. Vì vậy broadcast lại
                             * thông tin tài khoản; hàm update() bên dưới sẽ lọc để mỗi client chỉ nhận user của chính nó.
                             */
                            notifyAllAccountInfo();

                            Item updatedItem = AuctionManager.getInstance().getAllItems().stream()
                                    .filter(i -> i.getId().equals(itemId))
                                    .findFirst()
                                    .orElse(null);

                            if (updatedItem != null) {
                                notifier.notifyObservers(new Message("ITEM_UPDATE", updatedItem));
                                notifier.notifyObservers(new Message(
                                        "ITEM_BID_HISTORY_UPDATE",
                                        new Object[]{itemId, AuctionManager.getInstance().getBidHistoryForItem(itemId)}
                                ));
                                notifier.notifyObservers(new Message("BID_HISTORY_REFRESH_REQUIRED", itemId));
                            }
                        }
                        break;

                    case "GET_ALL_ITEMS":
                        java.util.List<String> closedNotifications = AuctionManager.getInstance().checkAndCloseExpiredAuctions();

                        for (String notification : closedNotifications) {
                            notifier.notifyObservers(new Message("AUCTION_RESULT_NOTIFICATION", notification));
                        }

                        if (!closedNotifications.isEmpty()) {
                            notifyAllAccountInfo();
                        }

                        sendMessage(new Message(
                                "GET_ALL_ITEMS_RESPONSE",
                                new ArrayList<>(AuctionManager.getInstance().getAllItems())
                        ));
                        break;

                    case "GET_MY_BID_HISTORY":
                        String historyUserId = (String) inputMessage.getPayload();
                        sendMessage(new Message(
                                "MY_BID_HISTORY_RESPONSE",
                                AuctionManager.getInstance().getBidHistoryForUser(historyUserId)
                        ));
                        break;

                    case "GET_ITEM_BID_HISTORY":
                        String historyItemId = String.valueOf(inputMessage.getPayload());
                        sendMessage(new Message(
                                "ITEM_BID_HISTORY_RESPONSE",
                                new Object[]{historyItemId, AuctionManager.getInstance().getBidHistoryForItem(historyItemId)}
                        ));
                        break;

                    case "JOIN_AUCTION_ROOM":
                        sendAuctionChatHistory(String.valueOf(inputMessage.getPayload()));
                        break;

                    case "SEND_AUCTION_CHAT":
                        handleAuctionChatMessage(inputMessage.getPayload());
                        break;

                    case "GET_ACCOUNT_INFO":
                        String accountUserId = (String) inputMessage.getPayload();
                        sendMessage(new Message(
                                "ACCOUNT_INFO_RESPONSE",
                                UserManager.getInstance().findUserById(accountUserId)
                        ));
                        break;

                    case "TOP_UP":
                        Object[] topUpData = (Object[]) inputMessage.getPayload();

                        String topUpUserId = (String) topUpData[0];
                        double topUpAmount = (Double) topUpData[1];
                        String topUpMethod = (String) topUpData[2];

                        User updatedUser = UserManager.getInstance().topUpBalance(topUpUserId, topUpAmount, topUpMethod);

                        if (updatedUser != null) {
                            if (currentUser != null && currentUser.getId().equals(updatedUser.getId())) {
                                currentUser = updatedUser;
                            }

                            sendMessage(new Message("TOP_UP_RESPONSE", new Object[]{
                                    true,
                                    "Đã nạp thành công " + topUpAmount + " VNĐ bằng phương thức: " + topUpMethod,
                                    updatedUser
                            }));
                        } else {
                            sendMessage(new Message("TOP_UP_RESPONSE", new Object[]{
                                    false,
                                    "Nạp tiền thất bại. Vui lòng kiểm tra lại tài khoản hoặc số tiền.",
                                    null
                            }));
                        }
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
                        String sellerId = String.valueOf(itemData[5]);
                        byte[] imageBytes = null;
                        if (itemData.length >= 8 && itemData[7] instanceof byte[]) {
                            // Tương thích với client cũ: [6] từng là thời gian mặc định.
                            imageBytes = (byte[]) itemData[7];
                        } else if (itemData.length >= 7 && itemData[6] instanceof byte[]) {
                            imageBytes = (byte[]) itemData[6];
                        }

                        String savedImagePath = null;
                        if (imageBytes != null && imageBytes.length > 0) {
                            try {
                                String fileName = UUID.randomUUID().toString() + ".png"; // Tạo tên file duy nhất
                                File imageFile = new File(IMAGE_DIR, fileName);
                                try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                                    fos.write(imageBytes);
                                }
                                savedImagePath = IMAGE_DIR + "/" + fileName; // Đường dẫn tương đối
                                logger.info("Đã lưu ảnh sản phẩm vào: {}", savedImagePath);
                            } catch (IOException e) {
                                logger.error("Lỗi khi lưu ảnh sản phẩm: {}", e.getMessage(), e);
                            }
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
                        // Không đặt thời gian kết thúc khi đăng sản phẩm.
                        // Thời gian chỉ được thiết lập khi người bán bấm "Bắt đầu đấu giá".
                        newItem.setEndTime(null);
                        newItem.setImagePath(savedImagePath); // Gán đường dẫn ảnh đã lưu

                        AuctionManager.getInstance().addItem(newItem);
                        sendMessage(new Message("ADD_ITEM_RESPONSE", "Đăng sản phẩm thành công! Mã SP: " + newItem.getId()));
                        notifier.notifyObservers(new Message("NEW_ITEM_ADDED", newItem));
                        break;

                    case "GET_IMAGE":
                        String imagePath = (String) inputMessage.getPayload();
                        byte[] imageData = null;
                        if (imagePath != null && !imagePath.isBlank()) {
                            try {
                                File imageFile = new File(imagePath);
                                if (imageFile.exists()) {
                                    imageData = Files.readAllBytes(imageFile.toPath());
                                } else {
                                    logger.warn("Không tìm thấy tệp ảnh: {}", imagePath);
                                }
                            } catch (IOException e) {
                                logger.error("Lỗi khi đọc tệp ảnh: {}", e.getMessage(), e);
                            }
                        }
                        sendMessage(new Message("GET_IMAGE_RESPONSE", imageData));
                        break;

                    case "GET_ALL_USERS":
                        if (currentUser == null || !"admin".equalsIgnoreCase(currentUser.getRole())) {
                            sendMessage(new Message("GET_ALL_USERS_RESPONSE", new ArrayList<User>()));
                            break;
                        }
                        sendMessage(new Message("GET_ALL_USERS_RESPONSE", new ArrayList<>(UserManager.getInstance().getAllUsers())));
                        break;

                    case "BAN_USER":
                        if (currentUser == null || !"admin".equalsIgnoreCase(currentUser.getRole())) {
                            sendMessage(new Message("BAN_USER_RESPONSE", "Cảnh báo: Chỉ admin mới có quyền khóa tài khoản."));
                            break;
                        }
                        String banUserId = (String) inputMessage.getPayload();
                        String banResult = UserManager.getInstance().banUser(banUserId);
                        boolean banSuccess = "success".equals(banResult);
                        sendMessage(new Message("BAN_USER_RESPONSE", banSuccess ? "Đã khóa tài khoản thành công." : banResult));

                        if (banSuccess) {
                            notifier.notifyObservers(new Message("FORCE_LOGOUT", banUserId));
                        }
                        break;

                    case "UNBAN_USER":
                        if (currentUser == null || !"admin".equalsIgnoreCase(currentUser.getRole())) {
                            sendMessage(new Message("UNBAN_USER_RESPONSE", "Cảnh báo: Chỉ admin mới có quyền mở khóa tài khoản."));
                            break;
                        }
                        String unbanUserId = (String) inputMessage.getPayload();
                        String unbanResult = UserManager.getInstance().unbanUser(unbanUserId);
                        sendMessage(new Message("UNBAN_USER_RESPONSE", "success".equals(unbanResult) ? "Đã mở khóa tài khoản thành công." : unbanResult));
                        break;

                    case "GET_ALL_ITEMS_ADMIN":
                        if (currentUser == null || !"admin".equalsIgnoreCase(currentUser.getRole())) {
                            sendMessage(new Message("GET_ALL_ITEMS_ADMIN_RESPONSE", new ArrayList<Item>()));
                            break;
                        }

                        java.util.List<String> adminClosedNotifications = AuctionManager.getInstance().checkAndCloseExpiredAuctions();

                        for (String notification : adminClosedNotifications) {
                            notifier.notifyObservers(new Message("AUCTION_RESULT_NOTIFICATION", notification));
                        }

                        if (!adminClosedNotifications.isEmpty()) {
                            notifyAllAccountInfo();
                        }

                        sendMessage(new Message(
                                "GET_ALL_ITEMS_ADMIN_RESPONSE",
                                new ArrayList<>(AuctionManager.getInstance().getAllItems())
                        ));
                        break;

                    case "CANCEL_AUCTION_ADMIN":
                        if (currentUser == null || !"admin".equalsIgnoreCase(currentUser.getRole())) {
                            sendMessage(new Message("CANCEL_AUCTION_RESPONSE", "Cảnh báo: Chỉ admin mới có quyền hủy phiên đấu giá."));
                            break;
                        }

                        String adminCancelItemId = (String) inputMessage.getPayload();
                        String adminCancelResult = AuctionManager.getInstance().cancelAuctionByAdmin(adminCancelItemId);

                        if ("success".equals(adminCancelResult)) {
                            logger.info("Admin {} đã hủy phiên đấu giá mã {}", currentUser.getUsername(), adminCancelItemId);
                            sendMessage(new Message("CANCEL_AUCTION_RESPONSE", "Đã hủy phiên đấu giá thành công."));
                            notifier.notifyObservers(new Message("SYSTEM_NOTIFICATION", "⚠️ [ADMIN] Phiên đấu giá mã " + adminCancelItemId + " đã bị hủy."));
                            notifyAllAccountInfo();

                            Item adminCanceledItem = AuctionManager.getInstance().getAllItems().stream()
                                    .filter(i -> i.getId().equals(adminCancelItemId))
                                    .findFirst()
                                    .orElse(null);

                            if (adminCanceledItem != null) {
                                notifier.notifyObservers(new Message("ITEM_UPDATE", adminCanceledItem));
                            } else {
                                notifier.notifyObservers(new Message("NEW_ITEM_ADDED", null));
                            }
                        } else {
                            sendMessage(new Message("CANCEL_AUCTION_RESPONSE", adminCancelResult));
                        }
                        break;

                    case "END_AUCTION_ADMIN":
                        if (currentUser == null || !"admin".equalsIgnoreCase(currentUser.getRole())) {
                            sendMessage(new Message("END_AUCTION_ADMIN_RESPONSE", "Cảnh báo: Chỉ admin mới có quyền kết thúc phiên đấu giá."));
                            break;
                        }

                        String endItemId = (String) inputMessage.getPayload();
                        String endResult = AuctionManager.getInstance().endAuctionByAdmin(endItemId);
                        sendMessage(new Message("END_AUCTION_ADMIN_RESPONSE", endResult));
                        notifyAllAccountInfo();

                        Item endedItem = AuctionManager.getInstance().getAllItems().stream()
                                .filter(i -> i.getId().equals(endItemId))
                                .findFirst()
                                .orElse(null);

                        if (endedItem != null) {
                            notifier.notifyObservers(new Message("ITEM_UPDATE", endedItem));
                        } else {
                            notifier.notifyObservers(new Message("NEW_ITEM_ADDED", null));
                        }
                        break;

                    case "DELETE_ITEM_ADMIN":
                        if (currentUser == null || !"admin".equalsIgnoreCase(currentUser.getRole())) {
                            sendMessage(new Message("DELETE_ITEM_ADMIN_RESPONSE", "Cảnh báo: Chỉ admin mới có quyền xóa sản phẩm."));
                            break;
                        }

                        String deleteItemId = (String) inputMessage.getPayload();
                        String deleteResult = AuctionManager.getInstance().deleteItemByAdmin(deleteItemId);
                        sendMessage(new Message("DELETE_ITEM_ADMIN_RESPONSE", deleteResult));
                        notifyAllAccountInfo();
                        notifier.notifyObservers(new Message("NEW_ITEM_ADDED", null));
                        break;

                    case "CANCEL_AUCTION":
                        if (currentUser == null || !"admin".equalsIgnoreCase(currentUser.getRole())) {
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
                            notifyAllAccountInfo();

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

    private void sendAuctionChatHistory(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            sendMessage(new Message("AUCTION_CHAT_HISTORY", new Object[]{itemId, new ArrayList<AuctionChatMessage>()}));
            return;
        }

        List<AuctionChatMessage> history = AUCTION_CHAT_HISTORY.get(itemId);
        List<AuctionChatMessage> snapshot = new ArrayList<>();

        if (history != null) {
            synchronized (history) {
                snapshot.addAll(history);
            }
        }

        sendMessage(new Message("AUCTION_CHAT_HISTORY", new Object[]{itemId, snapshot}));
    }

    private void handleAuctionChatMessage(Object payload) {
        if (currentUser == null) {
            sendMessage(new Message("AUCTION_CHAT_ERROR", "Bạn cần đăng nhập để chat trong phòng đấu giá."));
            return;
        }

        refreshCurrentUserFromStore();

        if (!(payload instanceof Object[])) {
            sendMessage(new Message("AUCTION_CHAT_ERROR", "Dữ liệu tin nhắn không hợp lệ."));
            return;
        }

        Object[] data = (Object[]) payload;
        if (data.length < 2) {
            sendMessage(new Message("AUCTION_CHAT_ERROR", "Dữ liệu tin nhắn không đủ thông tin."));
            return;
        }

        String itemId = String.valueOf(data[0]);
        String content = String.valueOf(data[1]).trim();
        String selectedRole = data.length >= 3 && data[2] != null ? String.valueOf(data[2]) : null;

        if (itemId.isBlank() || content.isBlank()) {
            return;
        }

        Item chatItem = AuctionManager.getInstance().getAllItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElse(null);

        if (chatItem != null && currentUser.getId() != null && currentUser.getId().equals(chatItem.getSellerId())) {
            sendMessage(new Message("AUCTION_CHAT_ERROR", "Người bán không thể chat trong phòng đấu giá sản phẩm của chính mình."));
            return;
        }

        boolean storedRoleIsBidder = "bidder".equalsIgnoreCase(currentUser.getRole());
        boolean selectedRoleIsBidder = "bidder".equalsIgnoreCase(selectedRole);

        if (!storedRoleIsBidder && !selectedRoleIsBidder) {
            sendMessage(new Message("AUCTION_CHAT_ERROR", "Chỉ người đấu giá mới được chat trong phòng đấu giá."));
            return;
        }

        if (content.length() > 300) {
            content = content.substring(0, 300);
        }

        AuctionChatMessage chatMessage = new AuctionChatMessage(
                itemId,
                currentUser.getId(),
                currentUser.getUsername(),
                content,
                LocalDateTime.now()
        );

        List<AuctionChatMessage> history = AUCTION_CHAT_HISTORY.computeIfAbsent(
                itemId,
                key -> Collections.synchronizedList(new ArrayList<>())
        );

        synchronized (history) {
            history.add(chatMessage);
            while (history.size() > MAX_CHAT_HISTORY_PER_ROOM) {
                history.remove(0);
            }
        }

        notifier.notifyObservers(new Message("AUCTION_CHAT_MESSAGE", chatMessage));
    }

    private void refreshCurrentUserFromStore() {
        if (currentUser == null || currentUser.getId() == null) {
            return;
        }

        User latestUser = UserManager.getInstance().findUserById(currentUser.getId());
        if (latestUser != null) {
            currentUser = latestUser;
        }
    }

    private boolean isLoginOrRegisterAction(String action) {
        return "LOGIN".equals(action) || "REGISTER".equals(action);
    }

    private boolean isCurrentUserBanned() {
        if (currentUser == null || currentUser.getId() == null) {
            return false;
        }

        User latestUser = UserManager.getInstance().findUserById(currentUser.getId());
        return latestUser != null && latestUser.isBanned();
    }

    private void notifyAllAccountInfo() {
        for (User user : UserManager.getInstance().getAllUsers()) {
            notifier.notifyObservers(new Message("ACCOUNT_INFO_RESPONSE", user));
        }
    }

    public synchronized void sendMessage(Message message) {
        try {
            if (out != null && !clientSocket.isClosed()) {
                out.reset();
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            logger.error("Không thể gửi tin nhắn tới Client {}", clientSocket.getInetAddress(), e);
        }
    }

    @Override
    public void update(Message message) {
        if ("ACCOUNT_INFO_RESPONSE".equals(message.getAction()) && message.getPayload() instanceof User) {
            User updatedUser = (User) message.getPayload();

            if (currentUser != null && currentUser.getId() != null
                    && currentUser.getId().equals(updatedUser.getId())) {
                currentUser = updatedUser;
                sendMessage(message);
            }

            return;
        }

        sendMessage(message);
    }
}
