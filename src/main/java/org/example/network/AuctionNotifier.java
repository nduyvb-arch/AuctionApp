package org.example.network;

import java.util.ArrayList;
import java.util.List;

public class AuctionNotifier implements Subject {
    // Danh sách lưu trữ những ai đang theo dõi phiên đấu giá
    private final List<Observer> observers = new ArrayList<>();

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
        // Gửi chiếc phong bì Message đến toàn bộ những người trong danh sách
        for (Observer o : observers) {
            o.update(message);
        }
    }
}