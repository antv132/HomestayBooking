package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** SCR-27 — Booking status breakdown for pie chart. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatusCountResponse {

    private String status;
    private long count;
}
