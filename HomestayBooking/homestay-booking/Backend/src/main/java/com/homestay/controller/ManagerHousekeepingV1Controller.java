package com.homestay.controller;
import com.homestay.dto.request.AssignHousekeepingTaskRequest;
import com.homestay.dto.request.CancelHousekeepingTaskRequest;
import com.homestay.dto.request.CreateHousekeepingTaskRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.HousekeepingScheduleResponse;
import com.homestay.dto.response.HousekeepingTaskSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.HousekeepingTask;
import com.homestay.entity.User;
import com.homestay.service.HousekeepingTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.UUID;
/** SCR-38 schedule + SCR-40 housekeeping tasks v1. */
@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
public class ManagerHousekeepingV1Controller {
    private final HousekeepingTaskService housekeepingTaskService;
    @GetMapping("/housekeeping/schedule")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<HousekeepingScheduleResponse>> getSchedule(
            @AuthenticationPrincipal User currentUser,
            @RequestParam UUID propertyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        HousekeepingScheduleResponse data = housekeepingTaskService.getScheduleForManager(
                currentUser, propertyId, date);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
    @GetMapping("/housekeeping-tasks")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<HousekeepingTaskSummaryResponse>>> listTasks(
            @AuthenticationPrincipal User currentUser,
            @RequestParam UUID propertyId,
            @RequestParam(required = false) HousekeepingTask.Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        PageResponse<HousekeepingTaskSummaryResponse> data = housekeepingTaskService.listTasksForManager(
                currentUser, propertyId, status, fromDate, toDate, page, size);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
    @PostMapping("/housekeeping-tasks")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<HousekeepingTaskSummaryResponse>> createTask(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateHousekeepingTaskRequest body) {
        HousekeepingTaskSummaryResponse data = housekeepingTaskService.createTaskForManager(currentUser, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Tạo tác vụ thành công", data));
    }
    @PatchMapping("/housekeeping-tasks/{id}/assign")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<HousekeepingTaskSummaryResponse>> assign(
            @PathVariable UUID id,
            @Valid @RequestBody AssignHousekeepingTaskRequest body,
            @AuthenticationPrincipal User currentUser) {
        HousekeepingTaskSummaryResponse data = housekeepingTaskService.assignTaskForManager(
                currentUser, id, body);
        return ResponseEntity.ok(ApiResponse.ok("Gán nhân viên thành công", data));
    }
    @PatchMapping("/housekeeping-tasks/{id}/cancel")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<HousekeepingTaskSummaryResponse>> cancel(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelHousekeepingTaskRequest body,
            @AuthenticationPrincipal User currentUser) {
        HousekeepingTaskSummaryResponse data = housekeepingTaskService.cancelTaskForManager(
                currentUser, id, body);
        return ResponseEntity.ok(ApiResponse.ok("Đã hủy tác vụ", data));
    }
}
