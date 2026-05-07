package org.example.network;

public class ServerMain {
    public static void main(String[] args) {

        int port = 8888;
        System.out.println("Đang khởi động hệ thống máy chủ...");
        AuctionServer server = new AuctionServer(port);

        server.startServer();
    }
}
