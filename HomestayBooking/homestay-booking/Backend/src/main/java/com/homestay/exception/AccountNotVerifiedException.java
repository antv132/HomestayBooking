package com.homestay.exception;

/**
 * Ném khi tài khoản chưa xác thực email (status = INACTIVE).
 * Frontend sẽ bắt exception này để redirect sang trang nhập OTP.
 */
public class AccountNotVerifiedException extends RuntimeException {

    private final String email;

    public AccountNotVerifiedException(String email) {
        super("Tài khoản chưa xác thực email. Vui lòng kiểm tra hộp thư và nhập mã OTP.");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
