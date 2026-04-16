package org.example.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionSever {
    private int port;
    private AuctionNotifier notifier;

    public AuctionSever(int port)
    {
        this.port = port;
        this.notifier = new AuctionNotifier();
    }

    public void startSever()
    {
        try (ServerSocket serverSocket = new ServerSocket(port))
        {
            System.out.println("Server đấu giá đang chạy trên cổng " + port + "...");
            System.out.println("Đang chờ người dùng kết nối tới Server...");

            while (true)
            {
                Socket clientSocket = serverSocket.accept();

                System.out.println("Có kết nối từ IP: " + clientSocket.getInetAddress());
                // TODO: Tạo một luồng (Thread) mới để phục vụ Client này,
                // giúp Server không bị "đứng hình" khi có nhiều người cùng kết nối.
            }
        }
        catch (IOException e) {
            System.out.println("lỗi khi khởi động Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
