package org.example.exception;

// Ngoại lệ khi giá đặt không hợp lệ (thấp hơn giá hiện tại)
public class InvalidBidException extends Exception {
    public InvalidBidException(String message) {
        super(message);
    }
}
