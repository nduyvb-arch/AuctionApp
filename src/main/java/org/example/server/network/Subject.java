package org.example.server.network;

import org.example.common.Message;

public interface Subject {
    void registerObserver(Observer o);

    void removeObserver(Observer o);

    void notifyObservers(Message message);
}