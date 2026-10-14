package com.homestay.service;

import com.homestay.dto.response.EmployeeDashboardResponse;
import com.homestay.dto.response.EmployeeDashboardResponse.AwaitingSummary;
import com.homestay.dto.response.EmployeeDashboardResponse.CompletedTodaySummary;
import com.homestay.dto.response.EmployeeDashboardResponse.EmployeeTaskSummary;
import com.homestay.dto.response.EmployeeDashboardResponse.HousekeepingTaskSummary;
import com.homestay.dto.response.EmployeeDashboardResponse.MaintenanceTicketSummary;
import com.homestay.dto.response.EmployeeDashboardResponse.RoomInspectionSummary;
import com.homestay.dto.response.EmployeeKpisResponse;
import com.homestay.entity.EmployeePropertyAssignment;
import com.homestay.entity.HousekeepingTask;
import com.homestay.entity.MaintenanceTicket;
import com.homestay.entity.RoomInspection;
import com.homestay.entity.User;
import com.homestay.repository.EmployeePropertyAssignmentRepository;
import com.homestay.repository.HousekeepingTaskRepository;
import com.homestay.repository.MaintenanceTicketRepository;
import com.homestay.repository.RoomInspectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FR-22 SCR-59 — Employee Dashboard composite + KPI subset.
 * Scope: JWT employee + FR-20 ACTIVE property assignments.
 * Timezone: Asia/Ho_Chi_Minh for "today".
 */
@Service
@RequiredArgsConstructor
public class EmployeeDashboardService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<HousekeepingTask.Status> HK_OPEN =
            List.of(HousekeepingTask.Status.PENDING, HousekeepingTask.Status.IN_PROGRESS);
    private static final List<MaintenanceTicket.Status> MT_OPEN =
            List.of(MaintenanceTicket.Status.ASSIGNED, MaintenanceTicket.Status.IN_PROGRESS);
    private static final List<RoomInspection.Status> RI_OPEN =
            List.of(RoomInspection.Status.PENDING, RoomInspection.Status.IN_PROGRESS);

    private final HousekeepingTaskRepository housekeepingTaskRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final RoomInspectionRepository roomInspectionRepository;
    private final EmployeePropertyAssignmentRepository employeePropertyAssignmentRepository;

    @Transactional(readOnly = true)
    public EmployeeKpisResponse getKpis(User employee) {
        EmployeeDashboardResponse dash = getDashboard(employee);
        return EmployeeKpisResponse.builder()
                .pendingHousekeeping(dash.getPendingHousekeeping())
                .pendingMaintenance(dash.getPendingMaintenance())
                .pendingInspections(dash.getPendingInspections())
                .build();
    }

    @Transactional(readOnly = true)
    public EmployeeDashboardResponse getDashboard(User employee) {
        UUID employeeId = employee.getId();
        String fullName = employee.getFullName() != null ? employee.getFullName() : "";

        List<UUID> propertyIds = employeePropertyAssignmentRepository
                .findPropertyIdsByEmployeeIdAndStatus(employeeId, EmployeePropertyAssignment.Status.ACTIVE);

        if (propertyIds.isEmpty()) {
            return emptyDashboard(fullName, true);
        }

        LocalDate today = LocalDate.now(VN);
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        PageRequest preview5 = PageRequest.of(0, 5);
        PageRequest preview3 = PageRequest.of(0, 3);
        PageRequest today10 = PageRequest.of(0, 10);

        int pendingHk = (int) housekeepingTaskRepository.countForDashboard(employeeId, propertyIds, HK_OPEN);
        int pendingMt = (int) maintenanceTicketRepository.countForDashboard(employeeId, propertyIds, MT_OPEN);
        int pendingRi = (int) roomInspectionRepository.countForEmployeeDashboard(propertyIds, RI_OPEN, employeeId);

        List<HousekeepingTask> hkPreview =
                housekeepingTaskRepository.findPreviewForDashboard(employeeId, propertyIds, HK_OPEN, preview5);
        List<MaintenanceTicket> mtPreview =
                maintenanceTicketRepository.findPreviewForDashboard(employeeId, propertyIds, MT_OPEN, preview5);
        List<RoomInspection> riPreview = roomInspectionRepository
                .findForEmployee(propertyIds, RI_OPEN, employeeId, preview5)
                .getContent();

        int hkDone = (int) housekeepingTaskRepository.countCompletedToday(employeeId, propertyIds, dayStart, dayEnd);
        int mtDone = (int) maintenanceTicketRepository.countCompletedToday(employeeId, propertyIds, dayStart, dayEnd);
        int riDone = (int) roomInspectionRepository.countCompletedToday(employeeId, propertyIds, dayStart, dayEnd);

        List<HousekeepingTask> hkDoneList =
                housekeepingTaskRepository.findCompletedToday(employeeId, propertyIds, dayStart, dayEnd, preview3);
        List<MaintenanceTicket> mtDoneList =
                maintenanceTicketRepository.findCompletedToday(employeeId, propertyIds, dayStart, dayEnd, preview3);
        List<RoomInspection> riDoneList =
                roomInspectionRepository.findCompletedToday(employeeId, propertyIds, dayStart, dayEnd, preview3);

        List<EmployeeTaskSummary> todayTasks = buildTodayTasks(
                housekeepingTaskRepository.findTodayForDashboard(
                        employeeId, propertyIds, today, dayStart, dayEnd, today10),
                maintenanceTicketRepository.findTodayForDashboard(
                        employeeId, propertyIds, dayStart, dayEnd, today10),
                roomInspectionRepository.findTodayForDashboard(
                        propertyIds, employeeId, today, dayStart, dayEnd, today10));

        List<EmployeeTaskSummary> awaitingPreviews = new ArrayList<>();
        hkPreview.stream().limit(3).forEach(t -> awaitingPreviews.add(toTask(t)));
        mtPreview.stream().limit(3).forEach(t -> awaitingPreviews.add(toTask(t)));
        riPreview.stream().limit(3).forEach(t -> awaitingPreviews.add(toTask(t)));

        List<EmployeeTaskSummary> completedPreviews = new ArrayList<>();
        hkDoneList.forEach(t -> completedPreviews.add(toTask(t)));
        mtDoneList.forEach(t -> completedPreviews.add(toTask(t)));
        riDoneList.forEach(t -> completedPreviews.add(toTask(t)));

        return EmployeeDashboardResponse.builder()
                .fullName(fullName)
                .pendingHousekeeping(pendingHk)
                .pendingMaintenance(pendingMt)
                .pendingInspections(pendingRi)
                .housekeepingTasks(hkPreview.stream().map(this::toHkSummary).toList())
                .maintenanceTickets(mtPreview.stream().map(this::toMtSummary).toList())
                .inspections(riPreview.stream().map(this::toRiSummary).toList())
                .todayTasks(todayTasks)
                .awaiting(AwaitingSummary.builder()
                        .housekeepingCount(pendingHk)
                        .maintenanceCount(pendingMt)
                        .inspectionCount(pendingRi)
                        .previews(awaitingPreviews)
                        .build())
                .completedToday(CompletedTodaySummary.builder()
                        .housekeepingCount(hkDone)
                        .maintenanceCount(mtDone)
                        .inspectionCount(riDone)
                        .previews(completedPreviews)
                        .build())
                .noPropertyAssignment(false)
                .build();
    }

    private EmployeeDashboardResponse emptyDashboard(String fullName, boolean noProperty) {
        return EmployeeDashboardResponse.builder()
                .fullName(fullName)
                .pendingHousekeeping(0)
                .pendingMaintenance(0)
                .pendingInspections(0)
                .housekeepingTasks(List.of())
                .maintenanceTickets(List.of())
                .inspections(List.of())
                .todayTasks(List.of())
                .awaiting(AwaitingSummary.builder()
                        .housekeepingCount(0)
                        .maintenanceCount(0)
                        .inspectionCount(0)
                        .previews(List.of())
                        .build())
                .completedToday(CompletedTodaySummary.builder()
                        .housekeepingCount(0)
                        .maintenanceCount(0)
                        .inspectionCount(0)
                        .previews(List.of())
                        .build())
                .noPropertyAssignment(noProperty)
                .build();
    }

    private List<EmployeeTaskSummary> buildTodayTasks(
            List<HousekeepingTask> hk,
            List<MaintenanceTicket> mt,
            List<RoomInspection> ri) {
        List<EmployeeTaskSummary> out = new ArrayList<>();
        for (HousekeepingTask t : hk) {
            if (out.size() >= 10) break;
            out.add(toTask(t));
        }
        for (MaintenanceTicket t : mt) {
            if (out.size() >= 10) break;
            out.add(toTask(t));
        }
        for (RoomInspection t : ri) {
            if (out.size() >= 10) break;
            out.add(toTask(t));
        }
        return out;
    }

    private HousekeepingTaskSummary toHkSummary(HousekeepingTask t) {
        return HousekeepingTaskSummary.builder()
                .id(t.getId())
                .roomNumber(t.getRoom() != null ? t.getRoom().getRoomNumber() : null)
                .propertyName(t.getProperty() != null ? t.getProperty().getName() : null)
                .status(t.getStatus() != null ? t.getStatus().name() : null)
                .createdAt(t.getCreatedAt())
                .build();
    }

    private MaintenanceTicketSummary toMtSummary(MaintenanceTicket t) {
        String propertyName = null;
        String roomNumber = null;
        if (t.getRoom() != null) {
            roomNumber = t.getRoom().getRoomNumber();
            if (t.getRoom().getProperty() != null) {
                propertyName = t.getRoom().getProperty().getName();
            }
        }
        return MaintenanceTicketSummary.builder()
                .id(t.getId())
                .title(t.getTitle())
                .roomNumber(roomNumber)
                .propertyName(propertyName)
                .status(t.getStatus() != null ? t.getStatus().name() : null)
                .createdAt(t.getCreatedAt())
                .build();
    }

    private RoomInspectionSummary toRiSummary(RoomInspection ri) {
        return RoomInspectionSummary.builder()
                .id(ri.getId())
                .bookingId(ri.getBooking() != null ? ri.getBooking().getId() : null)
                .roomNumber(ri.getRoom() != null ? ri.getRoom().getRoomNumber() : null)
                .propertyName(ri.getProperty() != null ? ri.getProperty().getName() : null)
                .checkOutDate(ri.getBooking() != null ? ri.getBooking().getCheckOutDate() : null)
                .status(ri.getStatus() != null ? ri.getStatus().name() : null)
                .build();
    }

    private EmployeeTaskSummary toTask(HousekeepingTask t) {
        return EmployeeTaskSummary.builder()
                .type("HOUSEKEEPING")
                .id(t.getId())
                .roomNumber(t.getRoom() != null ? t.getRoom().getRoomNumber() : null)
                .propertyName(t.getProperty() != null ? t.getProperty().getName() : null)
                .status(t.getStatus() != null ? t.getStatus().name() : null)
                .label("Dọn phòng")
                .checkOutDate(t.getBooking() != null ? t.getBooking().getCheckOutDate() : null)
                .build();
    }

    private EmployeeTaskSummary toTask(MaintenanceTicket t) {
        String propertyName = null;
        String roomNumber = null;
        if (t.getRoom() != null) {
            roomNumber = t.getRoom().getRoomNumber();
            if (t.getRoom().getProperty() != null) {
                propertyName = t.getRoom().getProperty().getName();
            }
        }
        return EmployeeTaskSummary.builder()
                .type("MAINTENANCE")
                .id(t.getId())
                .roomNumber(roomNumber)
                .propertyName(propertyName)
                .status(t.getStatus() != null ? t.getStatus().name() : null)
                .label(t.getTitle() != null ? t.getTitle() : "Bảo trì")
                .checkOutDate(null)
                .build();
    }

    private EmployeeTaskSummary toTask(RoomInspection ri) {
        return EmployeeTaskSummary.builder()
                .type("INSPECTION")
                .id(ri.getId())
                .roomNumber(ri.getRoom() != null ? ri.getRoom().getRoomNumber() : null)
                .propertyName(ri.getProperty() != null ? ri.getProperty().getName() : null)
                .status(ri.getStatus() != null ? ri.getStatus().name() : null)
                .label("Kiểm tra phòng")
                .checkOutDate(ri.getBooking() != null ? ri.getBooking().getCheckOutDate() : null)
                .build();
    }
}
