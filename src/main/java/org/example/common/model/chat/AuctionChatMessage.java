package org.example.common.model.chat;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AuctionChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String itemId;
    private final String senderId;
    private final String senderName;
    private final String content;
    private final LocalDateTime sentAt;

    public AuctionChatMessage(String itemId, String senderId, String senderName, String content, LocalDateTime sentAt) {
        this.itemId = itemId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.sentAt = sentAt;
    }

    public String getItemId() {
        return itemId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
