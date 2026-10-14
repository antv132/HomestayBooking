package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** SCR-59: Employee Dashboard KPIs (khop FE EmployeeKpis). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeKpisResponse {

    private long pendingHousekeeping;
    private long pendingMaintenance;
    private long pendingInspections;
}