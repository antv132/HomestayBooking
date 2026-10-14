package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.OutboxEvent;
import com.homestay.entity.User;
import com.homestay.service.OutboxAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/** SCR-56 — Admin notification outbox monitor. */
@RestController
@RequestMapping("/api/v1/admin/outbox-events")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOutboxController {

    private final OutboxAdminService outboxAdminService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Map<String, Object>>>> list(
            @RequestParam(defaultValue = "FAILED") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        OutboxEvent.Status st;
        try {
            st = OutboxEvent.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            st = OutboxEvent.Status.FAILED;
        }
        PageRequest pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.ok(outboxAdminService.list(st, pageable)));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<Map<String, Object>>> retry(
            @PathVariable UUID id,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã đưa event về PENDING để retry",
                outboxAdminService.retry(id, admin)));
    }
}
