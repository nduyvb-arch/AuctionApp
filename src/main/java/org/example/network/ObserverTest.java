package org.example.network;

// Giả lập một người dùng mạng đang chờ thông báo
class MockClient implements Observer {
    private final String name;

    public MockClient(String name) {
        this.name = name;
    }

    @Override
    public void update(Message message) {
        System.out.println("🔔 " + name + " nhận được tin: [" + message.getAction() + "] " + message.getPayload());
    }
}

public class ObserverTest {
    public static void main(String[] args) {
        // 1. Khởi tạo đài phát thanh
        AuctionNotifier notifier = new AuctionNotifier();

        // 2. Có 2 Client vừa truy cập vào xem món đồ
        MockClient client1 = new MockClient("Người mua A");
        MockClient client2 = new MockClient("Người mua B");

        // 3. Cho 2 Client đăng ký nhận thông báo
        notifier.registerObserver(client1);
        notifier.registerObserver(client2);

        System.out.println("--- Hệ thống bắt đầu hoạt động ---");

        // 4. Giả sử Dev 1 báo có người vừa đặt giá mới, hệ thống sẽ phát loa!
        Message updateMsg = new Message("NEW_BID", "Giá mới của bức tranh là 500$");
        notifier.notifyObservers(updateMsg);

        System.out.println("\n--- Người mua A thoát ứng dụng ---");
        notifier.removeObserver(client1); // Client A hủy đăng ký

        // 5. Cập nhật giá lần nữa, lúc này chỉ có B nhận được
        Message updateMsg2 = new Message("NEW_BID", "Giá mới của bức tranh là 600$");
        notifier.notifyObservers(updateMsg2);
    }
}