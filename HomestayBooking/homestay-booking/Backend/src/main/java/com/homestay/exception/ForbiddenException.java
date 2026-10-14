package com.homestay.exception;

// Ném khi user không có quyền thực hiện hành động
// Ví dụ: customer cố xem booking của người khác
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
