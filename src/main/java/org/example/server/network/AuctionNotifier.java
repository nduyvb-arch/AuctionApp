package org.example.server.network;

import org.example.common.Message;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionNotifier implements Subject {
    // Danh sách lưu trữ những ai đang theo dõi phiên đấu giá
    private final CopyOnWriteArrayList<Observer> observers = new CopyOnWriteArrayList<>();

    @Override
    public void registerObserver(Observer o) {
        observers.add(o);
    }

    @Override
    public void removeObserver(Observer o) {
        observers.remove(o);
    }

    @Override
    public void notifyObservers(Message message) {
        // Gửi Message đến toàn bộ những người trong danh sách
        for (Observer o : observers) {
            o.update(message);
        }
    }
}