
package org.example.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionServer {
    private int port;
    private AuctionNotifier notifier;

    public AuctionServer(int port) {
        this.port = port;
        this.notifier = new AuctionNotifier();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server đấu giá đang chạy trên cổng " + port + "...");
            System.out.println("Đang chờ người dùng kết nối tới Server...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Có kết nối từ IP: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, notifier);
                notifier.registerObserver(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.out.println("Lỗi khi khởi động Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}