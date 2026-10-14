package com.homestay.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/** FR-015 — Manager modify Confirmed booking (dates and/or room). */
@Data
public class ModifyBookingRequest {

    private LocalDate checkIn;
    private LocalDate checkOut;
    private UUID roomId;
}
