package org.example.network;

import java.io.*;

public class DataTest {
    public static void main(String[] args) {
        // 1. Tạo một gói tin thử nghiệm
        Message msgOut = new Message("TEST_CONNECTION", "Xin chào Server, tôi là Client!");
        String filePath = "test_data.dat";

        // 2. Ghi Object xuống file (File I/O - Ghi)
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(msgOut);
            System.out.println("Đã ghi gói tin ra file thành công!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 3. Đọc Object lên từ file (File I/O - Đọc)
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            Message msgIn = (Message) ois.readObject();
            System.out.println("\nĐã đọc file lên thành công!");
            System.out.println("Hành động: " + msgIn.getAction());
            System.out.println("Dữ liệu: " + msgIn.getPayload());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}