package org.example.network;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L; // Đảm bảo tính nhất quán khi gửi/nhận

    private String action; // Ví dụ: "LOGIN", "PLACE_BID", "GET_ITEMS"
    private Object payload; // Dữ liệu đi kèm (Có thể là chuỗi String, hoặc object User, Item, BidTransaction...)

    public Message(String action, Object payload) {
        this.action = action;
        this.payload = payload;
    }

    public String getAction() {
        return action;
    }

    public Object getPayload() {
        return payload;
    }
}