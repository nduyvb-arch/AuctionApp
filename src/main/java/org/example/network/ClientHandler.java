package org.example.network;

import org.example.manager.AuctionManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable, Observer {
    private Socket clientSocket;
    private AuctionNotifier notifier;
    private PrintWriter out; // gửi tin nhắn về Client
    private BufferedReader in; // đọc tin nhắn từ Client

    public ClientHandler(Socket clientSocket, AuctionNotifier notifier) {
        this.clientSocket = clientSocket;
        this.notifier = notifier;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            out.println("Chào mừng bạn đã kết nối vào sàn đấu giá");
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Nhận được lệnh từ Client: " + inputLine);
                if (inputLine.startsWith("BID")) {
                    String[] parts = inputLine.split("\\s+");
                    if (parts.length == 4) {
                        String itemId = parts[1];
                        double bidAmount = Double.parseDouble(parts[2]);
                        String username = parts[3];
                        String result = AuctionManager.getInstance().placeBid(itemId, bidAmount, username);
                        out.println(result);
                        if (result.contains("thành công")) {
                            Message msg = new Message("NOTIFICATION", "Người dùng " + username + " vừa đặt giá " + bidAmount + " cho " + itemId);
                            notifier.notifyObservers(msg);
                        }
                    } else {
                        out.println("Sai cú pháp! Hãy nhập: BID [ItemID] [SốTiền] [Username]");
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Client đã ngắt kết nối: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void update(Message message) {
        if (out != null) {
            out.println("THÔNG BÁO TỪ SERVER: " + message.getPayload());
        }
    }
}