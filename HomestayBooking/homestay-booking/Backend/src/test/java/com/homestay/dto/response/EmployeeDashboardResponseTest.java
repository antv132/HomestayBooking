package com.homestay.dto.response;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeDashboardResponseTest {

    @Test
    void builder_createsInstanceWithAllFields() {
        EmployeeDashboardResponse response = EmployeeDashboardResponse.builder()
                .fullName("John Employee")
                .pendingHousekeeping(5)
                .pendingMaintenance(3)
                .pendingInspections(2)
                .noPropertyAssignment(false)
                .build();

        assertThat(response.getFullName()).isEqualTo("John Employee");
        assertThat(response.getPendingHousekeeping()).isEqualTo(5);
        assertThat(response.getPendingMaintenance()).isEqualTo(3);
        assertThat(response.getPendingInspections()).isEqualTo(2);
        assertThat(response.isNoPropertyAssignment()).isFalse();
    }

    @Test
    void builder_initializesListFieldsWithDefaults() {
        EmployeeDashboardResponse response = EmployeeDashboardResponse.builder().build();

        assertThat(response.getHousekeepingTasks()).isNotNull().isEmpty();
        assertThat(response.getMaintenanceTickets()).isNotNull().isEmpty();
        assertThat(response.getInspections()).isNotNull().isEmpty();
        assertThat(response.getTodayTasks()).isNotNull().isEmpty();
    }

    @Test
    void housekeepingTaskSummaryBuilder_createsInstance() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        EmployeeDashboardResponse.HousekeepingTaskSummary task =
                EmployeeDashboardResponse.HousekeepingTaskSummary.builder()
                        .id(id)
                        .roomNumber("101")
                        .propertyName("Test Property")
                        .status("PENDING")
                        .createdAt(createdAt)
                        .build();

        assertThat(task.getId()).isEqualTo(id);
        assertThat(task.getRoomNumber()).isEqualTo("101");
        assertThat(task.getPropertyName()).isEqualTo("Test Property");
        assertThat(task.getStatus()).isEqualTo("PENDING");
        assertThat(task.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void maintenanceTicketSummaryBuilder_createsInstance() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        EmployeeDashboardResponse.MaintenanceTicketSummary ticket =
                EmployeeDashboardResponse.MaintenanceTicketSummary.builder()
                        .id(id)
                        .title("Fix AC")
                        .roomNumber("202")
                        .propertyName("Test Property")
                        .status("ASSIGNED")
                        .createdAt(createdAt)
                        .build();

        assertThat(ticket.getId()).isEqualTo(id);
        assertThat(ticket.getTitle()).isEqualTo("Fix AC");
        assertThat(ticket.getRoomNumber()).isEqualTo("202");
        assertThat(ticket.getPropertyName()).isEqualTo("Test Property");
        assertThat(ticket.getStatus()).isEqualTo("ASSIGNED");
        assertThat(ticket.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void roomInspectionSummaryBuilder_createsInstance() {
        UUID id = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        LocalDate checkOutDate = LocalDate.now();

        EmployeeDashboardResponse.RoomInspectionSummary inspection =
                EmployeeDashboardResponse.RoomInspectionSummary.builder()
                        .id(id)
                        .bookingId(bookingId)
                        .roomNumber("303")
                        .propertyName("Test Property")
                        .checkOutDate(checkOutDate)
                        .status("PENDING")
                        .build();

        assertThat(inspection.getId()).isEqualTo(id);
        assertThat(inspection.getBookingId()).isEqualTo(bookingId);
        assertThat(inspection.getRoomNumber()).isEqualTo("303");
        assertThat(inspection.getPropertyName()).isEqualTo("Test Property");
        assertThat(inspection.getCheckOutDate()).isEqualTo(checkOutDate);
        assertThat(inspection.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void employeeTaskSummaryBuilder_createsInstance() {
        UUID id = UUID.randomUUID();
        LocalDate checkOutDate = LocalDate.now();

        EmployeeDashboardResponse.EmployeeTaskSummary task =
                EmployeeDashboardResponse.EmployeeTaskSummary.builder()
                        .type("HOUSEKEEPING")
                        .id(id)
                        .roomNumber("404")
                        .propertyName("Test Property")
                        .status("PENDING")
                        .label("Clean room 404")
                        .checkOutDate(checkOutDate)
                        .build();

        assertThat(task.getType()).isEqualTo("HOUSEKEEPING");
        assertThat(task.getId()).isEqualTo(id);
        assertThat(task.getRoomNumber()).isEqualTo("404");
        assertThat(task.getPropertyName()).isEqualTo("Test Property");
        assertThat(task.getStatus()).isEqualTo("PENDING");
        assertThat(task.getLabel()).isEqualTo("Clean room 404");
        assertThat(task.getCheckOutDate()).isEqualTo(checkOutDate);
    }

    @Test
    void awaitingSummaryBuilder_createsInstanceWithDefaults() {
        List<EmployeeDashboardResponse.EmployeeTaskSummary> previews = new ArrayList<>();
        previews.add(EmployeeDashboardResponse.EmployeeTaskSummary.builder()
                .type("HOUSEKEEPING")
                .id(UUID.randomUUID())
                .roomNumber("101")
                .build());

        EmployeeDashboardResponse.AwaitingSummary awaiting =
                EmployeeDashboardResponse.AwaitingSummary.builder()
                        .housekeepingCount(5)
                        .maintenanceCount(3)
                        .inspectionCount(2)
                        .previews(previews)
                        .build();

        assertThat(awaiting.getHousekeepingCount()).isEqualTo(5);
        assertThat(awaiting.getMaintenanceCount()).isEqualTo(3);
        assertThat(awaiting.getInspectionCount()).isEqualTo(2);
        assertThat(awaiting.getPreviews()).hasSize(1);
    }

    @Test
    void completedTodaySummaryBuilder_createsInstanceWithDefaults() {
        EmployeeDashboardResponse.CompletedTodaySummary completed =
                EmployeeDashboardResponse.CompletedTodaySummary.builder()
                        .housekeepingCount(2)
                        .maintenanceCount(1)
                        .inspectionCount(0)
                        .build();

        assertThat(completed.getHousekeepingCount()).isEqualTo(2);
        assertThat(completed.getMaintenanceCount()).isEqualTo(1);
        assertThat(completed.getInspectionCount()).isEqualTo(0);
        assertThat(completed.getPreviews()).isNotNull().isEmpty();
    }

    @Test
    void fullDashboard_withAllNestedObjects() {
        EmployeeDashboardResponse.AwaitingSummary awaiting =
                EmployeeDashboardResponse.AwaitingSummary.builder()
                        .housekeepingCount(5)
                        .maintenanceCount(3)
                        .inspectionCount(2)
                        .build();

        EmployeeDashboardResponse.CompletedTodaySummary completed =
                EmployeeDashboardResponse.CompletedTodaySummary.builder()
                        .housekeepingCount(8)
                        .maintenanceCount(4)
                        .inspectionCount(1)
                        .build();

        EmployeeDashboardResponse response = EmployeeDashboardResponse.builder()
                .fullName("Jane Employee")
                .pendingHousekeeping(5)
                .pendingMaintenance(3)
                .pendingInspections(2)
                .awaiting(awaiting)
                .completedToday(completed)
                .noPropertyAssignment(false)
                .build();

        assertThat(response.getFullName()).isEqualTo("Jane Employee");
        assertThat(response.getAwaiting()).isNotNull();
        assertThat(response.getAwaiting().getHousekeepingCount()).isEqualTo(5);
        assertThat(response.getCompletedToday()).isNotNull();
        assertThat(response.getCompletedToday().getHousekeepingCount()).isEqualTo(8);
    }
}
