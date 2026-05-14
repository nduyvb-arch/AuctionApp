
package org.example.server.network;

import org.example.common.Message;
import org.example.server.manager.AuctionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionServer {
    // Khởi tạo máy ghi log
    private static final Logger logger = LoggerFactory.getLogger(AuctionServer.class);

    private int port;
    private AuctionNotifier notifier;

    public AuctionServer(int port) {
        this.port = port;
        this.notifier = new AuctionNotifier();
    }

    public void startServer() {
        // Đánh thức bộ não & Database
        AuctionManager.getInstance();
        logger.info("Dữ liệu Database được tải lên RAM thành công");

        startExpirationWatcher();


        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Server đấu giá đang chạy trên cổng {}...", port);
            logger.info("Đang chờ người dùng kết nối tới Server...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Có kết nối từ IP: {}", clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, notifier);
                notifier.registerObserver(handler);

                new Thread(handler).start();
            }
        } catch (IOException e) {
            logger.error("Lỗi khi khởi động Server: {}", e.getMessage(), e);
        }
    }

    private void startExpirationWatcher()
    {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() ->
        {
            try
            {
                //Manager sẽ kiểm tra các phiên đấu giá đã hết hạn và thông báo cho các ClientHandler
                List<String> notifications = AuctionManager.getInstance().checkAndCloseExpiredAuctions();
                for (String msg : notifications)
                {
                    logger.info(["[THÔNG BÁO HỆ THỐNG] {}", msg)
                    // gửi thông báo về cho Client
                    notifier.notifyObservers(new Message("SYSTEM_NOTIFICATION", msg));
                }
            }
            catch(Exception e)
            {
                logger.error("Lỗi trong quá trình kiểm tra đấu giá hết hạn: {}", e.getMessage(), e);
            }
        }, 0, 10, TimeUnit.SECONDS); // Kiểm tra mỗi 10 giây
    }
}