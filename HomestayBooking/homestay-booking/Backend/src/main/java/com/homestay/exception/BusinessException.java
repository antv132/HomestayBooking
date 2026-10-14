package com.homestay.exception;

// Ném khi vi phạm quy tắc nghiệp vụ
// Ví dụ: đặt phòng trùng ngày, cọc đã thanh toán, review 2 lần, ...
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
