package com.homestay.exception;

// Ném khi không tìm thấy entity (Room, Booking, User, ...)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
