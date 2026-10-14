package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** SCR-09 — month view availability (flat lists + FR-05 days[]). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthAvailabilityResponse {

    /** @deprecated Prefer {@link #days} — kept for older FE clients. */
    private List<String> bookedDates = new ArrayList<>();
    /** @deprecated Prefer {@link #days}. */
    private List<String> maintenanceDates = new ArrayList<>();
    /** FR-05: one status per day (8 values). */
    private List<CalendarDayStatusResponse> days = new ArrayList<>();

    public MonthAvailabilityResponse(List<String> bookedDates, List<String> maintenanceDates) {
        this.bookedDates = bookedDates;
        this.maintenanceDates = maintenanceDates;
        this.days = new ArrayList<>();
    }
}
