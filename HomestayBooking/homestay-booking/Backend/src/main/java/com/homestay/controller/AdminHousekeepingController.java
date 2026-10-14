package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.HousekeepingTaskSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.HousekeepingTask;
import com.homestay.service.HousekeepingTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * FR-012 / SCR-40 Admin — read-only housekeeping task list.
 * No assign / create / cancel endpoints.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminHousekeepingController {

    private final HousekeepingTaskService housekeepingTaskService;

    @GetMapping("/housekeeping-tasks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<HousekeepingTaskSummaryResponse>>> list(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) HousekeepingTask.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<HousekeepingTaskSummaryResponse> data =
                housekeepingTaskService.listTasksForAdmin(propertyId, status, page, size);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
