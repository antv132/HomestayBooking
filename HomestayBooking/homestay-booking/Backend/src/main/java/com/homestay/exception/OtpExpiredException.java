package com.homestay.exception;

/**
 * Ném khi mã OTP đã hết hạn.
 * HTTP 410 Gone — frontend nhận biết để hiện thông báo và nút "Yêu cầu mã mới".
 */
public class OtpExpiredException extends RuntimeException {
    public OtpExpiredException(String message) {
        super(message);
    }
}
