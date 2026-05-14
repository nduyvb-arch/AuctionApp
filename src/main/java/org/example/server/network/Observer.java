package org.example.server.network;

import org.example.common.Message;

public interface Observer {
    void update(Message message);
}
