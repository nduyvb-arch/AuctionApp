package org.example.client.network;

import javafx.application.Platform;
import org.example.common.Message;
import org.example.client.controllers.BidObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class NetworkClient {

    private static final Logger logger = LoggerFactory.getLogger(NetworkClient.class);
    private static NetworkClient instance;
    private Socket socket;

    // Luồng xuất nhập Object Stream
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isRunning;

    // Danh sách các màn hình (Controller) đang đăng kí nhận thông báo chung
    private final List<MessageListener> listeners = new ArrayList<>();

    // Người lắng nghe chuyên biệt cho biểu đồ Lịch sử đặt giá Realtime
    private BidObserver bidObserver;

    private NetworkClient(){}

    public static synchronized NetworkClient getInstance()
    {
        if (instance == null)
        {
            instance = new NetworkClient();
        }
        return instance;
    }

    /**
     * Hàm Setter cho phép màn hình BidHistoryController đăng ký nhận tin cập nhật biểu đồ
     */
    public void setBidObserver(BidObserver observer) {
        this.bidObserver = observer;
    }

    // Hàm kết nối tới server ( chạy 1 lần khi bật App )
    public void connect(String serverAddress, int port)
    {
        if (socket != null && !socket.isClosed())
        {
            logger.info("Mạng đã được kết nối, không cần kết nối lại");
            return;
        }
        try
        {
            socket = new Socket(serverAddress, port);

            // Mở out trước in để tránh deadlock
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            isRunning = true;

            // Khởi chạy luồng nghe độc lập để tránh treo giao diện
            Thread listenerThread = new Thread(this::listenForMessages);
            listenerThread.setDaemon(true); // Thread tự tắt khi đóng App chính thức
            listenerThread.start();

            logger.info("Kết nối tới Server thành công tại {}:{}", serverAddress, port);
        }
        catch(IOException e)
        {
            logger.error("Lỗi kết nối Server: {}", e.getMessage(), e);
        }
    }

    public synchronized void sendMessage(Message message)
    {
        if (out != null)
        {
            try
            {
                out.writeObject(message);
                out.flush();
                out.reset();
            }
            catch(IOException e)
            {
                logger.error("Lỗi khi gửi tin nhắn: {}", e.getMessage(), e);
            }
        }
    }

    // Luồng lắng nghe Realtime
    private void listenForMessages()
    {
        try
        {
            Message incomingMessage;
            while (isRunning && (incomingMessage = (Message) in.readObject()) != null)
            {
                Message finalMsg = incomingMessage;

                // --- XỬ LÝ ĐẨY DỮ LIỆU SANG BIỂU ĐỒ REALTIME ---
                // Khớp chính xác phương thức getAction() và getPayload() của nhóm bạn
                if ("BID_UPDATE".equals(finalMsg.getAction()) || finalMsg.getPayload() instanceof org.example.client.controllers.BidHistoryController.BidHistoryRecord) {

                    Object dataPayload = finalMsg.getPayload();

                    if (dataPayload instanceof org.example.client.controllers.BidHistoryController.BidHistoryRecord) {
                        org.example.client.controllers.BidHistoryController.BidHistoryRecord record =
                                (org.example.client.controllers.BidHistoryController.BidHistoryRecord) dataPayload;

                        // Bắn thông tin về đồ thị qua interface
                        if (bidObserver != null) {
                            // Ép chạy trong luồng chính JavaFX UI an toàn
                            Platform.runLater(() -> bidObserver.onNewBidReceived(record));
                        }
                    }
                }

                // Đẩy dữ liệu vào luồng giao diện cho các Listener chung khác của nhóm bạn
                Platform.runLater(() -> {
                    for (MessageListener listener : listeners)
                    {
                        listener.onMessageReceived(finalMsg);
                    }
                });
            }
        }
        catch (Exception e)
        {
            logger.error("Mất kết nối với Server hoặc luồng bị ngắt: {}", e.getMessage(), e);
        }
    }

    public void addMessageListener(MessageListener listener)
    {
        if (!listeners.contains(listener))
        {
            listeners.add(listener);
        }
    }

    public void removeMessageListener(MessageListener listener)
    {
        listeners.remove(listener);
    }

    public interface MessageListener
    {
        void onMessageReceived(Message message);
    }
}