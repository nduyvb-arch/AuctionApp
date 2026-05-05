package org.example.network;

public class ServerMain {
    public static void main(String[] args) {

        int port = 8888; // Đổi cổng thành 8888 để khớp với Client
        System.out.println("Đang khởi động hệ thống máy chủ...");
        AuctionServer server = new AuctionServer(port);

        // Bấm nút Start!
        server.startServer();
    }
}
