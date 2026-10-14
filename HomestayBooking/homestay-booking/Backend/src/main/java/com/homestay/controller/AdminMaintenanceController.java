package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.service.MaintenanceTicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.homestay.entity.User;

import java.util.Map;
import java.util.UUID;

/** FR-017 — Admin read-only global maintenance list/detail. */
@RestController
@RequestMapping("/api/v1/admin/maintenance-tickets")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMaintenanceController {

    private final MaintenanceTicketService ticketService;

    public AdminMaintenanceController(MaintenanceTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.listForAdmin(status, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> detail(
            @AuthenticationPrincipal User admin,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getTicketDetail(admin, id)));
    }
}
