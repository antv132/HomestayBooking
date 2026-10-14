package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HousekeepingScheduleResponse {

    private LocalDate date;
    private Summary summary;
    private List<HousekeepingTaskSummaryResponse> unassigned;
    private List<EmployeeColumn> employees;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private long pending;
        private long inProgress;
        private long completed;
        private long unassigned;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeColumn {
        private UUID employeeId;
        private String employeeName;
        private List<HousekeepingTaskSummaryResponse> tasks;
    }
}
