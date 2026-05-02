package org.example.network;

public class ServerMain {
    public static void main(String[] args) {
        // Chốt hạ mở cổng 8080 cho Server
        int port = 8080;

        System.out.println("Đang khởi động hệ thống máy chủ...");

        // Gọi thợ đến xây Server dựa trên bản vẽ AuctionServer của sếp
        AuctionServer server = new AuctionServer(port);

        // Bấm nút Start!
        server.startServer();
    }
}