package com.homestay.controller;

import com.homestay.dto.response.ActivityLogEntryResponse;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.service.ActivityLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping({"/api/admin/activity-logs", "/api/v1/admin/activity-logs"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminActivityLogController {

    private final ActivityLogQueryService activityLogQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ActivityLogEntryResponse>>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize);
        return ResponseEntity.ok(ApiResponse.ok(
                activityLogQueryService.listLogs(from, to, action, userId, search, pageable)));
    }
}
