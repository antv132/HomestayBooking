package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** SCR-27 — Manager Dashboard KPIs per property (FR-16). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyKpisResponse {

    private UUID propertyId;
    private long totalRooms;
    private double occupancyRate;
    private long revenue;
    private int pendingCheckIns;
    private int pendingApprovals;
}
