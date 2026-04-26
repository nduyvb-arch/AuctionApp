package org.example.data;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
public class StorageManager {
    /**
     * Hàm lưu mọi loại dữ liệu (Object) xuống ổ cứng
     */
    public static boolean saveData(Object data, String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(data);
            System.out.println("Đã lưu dữ liệu an toàn vào: " + filePath);
            return true;
        } catch (IOException e) {
            System.out.println("Lỗi khi lưu dữ liệu: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    /**
     * Hàm khôi phục dữ liệu từ ổ cứng lên RAM
     */
    public static Object loadData(String filePath) {
        File file = new File(filePath);
        // Nếu Server vừa cài đặt, chưa có file dữ liệu cũ thì báo không tìm thấy
        if (!file.exists()) {
            System.out.println("⚠Không tìm thấy file dữ liệu cũ. Sẽ khởi tạo hệ thống trắng...");
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            Object data = ois.readObject();
            System.out.println("Đã khôi phục dữ liệu thành công từ: " + filePath);
            return data;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Lỗi khi đọc file dữ liệu: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}