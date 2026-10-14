package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** FR-22 SCR-59 — Composite employee dashboard payload. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDashboardResponse {

    private String fullName;
    private int pendingHousekeeping;
    private int pendingMaintenance;
    private int pendingInspections;

    @Builder.Default
    private List<HousekeepingTaskSummary> housekeepingTasks = new ArrayList<>();
    @Builder.Default
    private List<MaintenanceTicketSummary> maintenanceTickets = new ArrayList<>();
    @Builder.Default
    private List<RoomInspectionSummary> inspections = new ArrayList<>();
    @Builder.Default
    private List<EmployeeTaskSummary> todayTasks = new ArrayList<>();

    private AwaitingSummary awaiting;
    private CompletedTodaySummary completedToday;
    private boolean noPropertyAssignment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HousekeepingTaskSummary {
        private UUID id;
        private String roomNumber;
        private String propertyName;
        private String status;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaintenanceTicketSummary {
        private UUID id;
        private String title;
        private String roomNumber;
        private String propertyName;
        private String status;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomInspectionSummary {
        private UUID id;
        private UUID bookingId;
        private String roomNumber;
        private String propertyName;
        private LocalDate checkOutDate;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeTaskSummary {
        private String type; // HOUSEKEEPING | MAINTENANCE | INSPECTION
        private UUID id;
        private String roomNumber;
        private String propertyName;
        private String status;
        private String label;
        private LocalDate checkOutDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AwaitingSummary {
        private int housekeepingCount;
        private int maintenanceCount;
        private int inspectionCount;
        @Builder.Default
        private List<EmployeeTaskSummary> previews = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompletedTodaySummary {
        private int housekeepingCount;
        private int maintenanceCount;
        private int inspectionCount;
        @Builder.Default
        private List<EmployeeTaskSummary> previews = new ArrayList<>();
    }
}
