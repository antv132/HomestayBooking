package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** FR-015 — result of manager modify Confirmed booking. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModifyBookingResponse {

    private ManagerBookingDetailResponse booking;
    /** Positive = customer owes more; negative = credit on remaining. */
    private BigDecimal priceDelta;
    private String message;
}
