package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.EmployeeRoomResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.StaffRoomMonthCalendarResponse;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.service.EmployeeRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/** SCR-65 / FR-05 US4 — Employee property room list + read-only calendar. */
@RestController
@RequestMapping("/api/v1/employees/rooms")
@PreAuthorize("hasRole('EMPLOYEE')")
@RequiredArgsConstructor
public class EmployeeRoomController {

    private static final int MAX_PAGE_SIZE = 200;

    private final EmployeeRoomService employeeRoomService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployeeRoomResponse>>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        PageResponse<EmployeeRoomResponse> data =
                employeeRoomService.list(currentUser, PageRequest.of(safePage, safeSize));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /** FR-05 US4 — read-only month calendar (days[]), assigned property only. */
    @GetMapping("/{id}/calendar")
    public ResponseEntity<ApiResponse<StaffRoomMonthCalendarResponse>> getCalendar(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User currentUser) {
        LocalDate[] window = resolveCalendarWindow(month, year, startDate, endDate);
        StaffRoomMonthCalendarResponse data =
                employeeRoomService.getMonthCalendar(currentUser, id, window[0], window[1]);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    private static LocalDate[] resolveCalendarWindow(
            Integer month, Integer year, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return new LocalDate[]{startDate, endDate};
        }
        if (month != null && year != null) {
            YearMonth ym = YearMonth.of(year, month);
            return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
        }
        throw new BusinessException("Cần month+year hoặc startDate+endDate");
    }
}
