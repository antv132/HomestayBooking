package com.homestay.exception;

/** SCR-35 — xung đột nghiệp vụ (vd. inspection chưa hoàn tất). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
