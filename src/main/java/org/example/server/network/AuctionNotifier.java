package org.example.server.network;

import org.example.common.Message;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionNotifier implements Subject {
    private final CopyOnWriteArrayList<Observer> observers = new CopyOnWriteArrayList<>();

    @Override
    public void registerObserver(Observer o) {
        if (!observers.contains(o)) {
            observers.add(o);
        }
    }

    @Override
    public void removeObserver(Observer o) {
        observers.remove(o);
    }

    @Override
    public void notifyObservers(Message message) {
        for (Observer o : observers) {
            o.update(message);
        }
    }
}