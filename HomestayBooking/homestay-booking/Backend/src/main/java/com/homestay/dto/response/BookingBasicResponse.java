package com.homestay.dto.response;

import com.homestay.entity.Booking;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class BookingBasicResponse {
    private UUID id;
    private UUID roomId;
    private String roomName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String status;

    public static BookingBasicResponse fromEntity(Booking booking) {
        BookingBasicResponse response = new BookingBasicResponse();
        response.setId(booking.getId());
        response.setRoomId(booking.getRoom().getId());
        response.setRoomName(booking.getRoom().getRoomNumber());
        response.setCheckInDate(booking.getCheckInDate());
        response.setCheckOutDate(booking.getCheckOutDate());
        response.setStatus(booking.getStatus().name());
        return response;
    }
}
