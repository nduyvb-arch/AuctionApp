package org.example.server.network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerMain {
    private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);
    public static void main(String[] args) {
        int port = 8888;
        logger.info("Đang khởi động hệ thống máy chủ...");
        AuctionServer server = new AuctionServer(port);

        server.startServer();
    }
}
