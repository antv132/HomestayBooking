package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** FR-05 CalendarDayStatus — one day, one of 8 statuses. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDayStatusResponse {

    private String date;
    /** AVAILABLE | PENDING_DEPOSIT | RESERVED | OCCUPIED | PENDING_CLEANING | CLEANING_IN_PROGRESS | MAINTENANCE | OUT_OF_SERVICE */
    private String status;
    private boolean bookable;
}
