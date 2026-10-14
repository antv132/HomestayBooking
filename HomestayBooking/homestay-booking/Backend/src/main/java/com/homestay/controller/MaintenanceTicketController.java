package com.homestay.controller;

import com.homestay.dto.request.UpdateMaintenanceStatusRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.entity.User;
import com.homestay.service.MaintenanceTicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class MaintenanceTicketController {

    private final MaintenanceTicketService ticketService;

    public MaintenanceTicketController(MaintenanceTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/api/maintenance-tickets")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomerTickets(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Map<String, Object> result = ticketService.getCustomerTicketsPaged(currentUser.getId(), status, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/api/maintenance-tickets")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Object>> createTicket(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("roomId") UUID roomId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos) {
        Object ticket = ticketService.createTicketFromForm(currentUser.getId(), roomId, title, description, photos);
        return ResponseEntity.ok(ApiResponse.ok("Maintenance request submitted successfully", ticket));
    }

    @GetMapping("/api/maintenance-tickets/{id}")
    public ResponseEntity<ApiResponse<Object>> getTicketDetail(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        Object ticket = ticketService.getTicketDetail(currentUser, id);
        return ResponseEntity.ok(ApiResponse.ok(ticket));
    }

    @PutMapping("/api/maintenance-tickets/{id}")
    public ResponseEntity<ApiResponse<Object>> updateTicket(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "existingPhotoUrls", required = false) List<String> existingPhotoUrls,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos) {
        Object ticket = ticketService.updateTicketContent(currentUser.getId(), id, title, description, existingPhotoUrls, photos);
        return ResponseEntity.ok(ApiResponse.ok("Maintenance request updated", ticket));
    }

    @DeleteMapping("/api/maintenance-tickets/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        ticketService.deleteTicket(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Maintenance request deleted successfully"));
    }

    /** @deprecated Use GET /api/v1/manager/maintenance-tasks?propertyId= */
    @GetMapping("/api/maintenance-tickets/all")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Object>> getAllTickets(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Object result = ticketService.getAllTicketsPaged(status, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** Legacy — only RESOLVED→CLOSED (delegates to SCR-41 service). */
    @PutMapping("/api/maintenance-tickets/{id}/status")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Object>> updateTicketStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMaintenanceStatusRequest request) {
        Object ticket = ticketService.updateTicketStatusAndReturn(currentUser, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Status updated successfully", ticket));
    }

    @GetMapping("/api/bookings/my-active")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Object>> getMyActiveBookings(
            @AuthenticationPrincipal User currentUser) {
        Object bookings = ticketService.getActiveBookingsForCustomer(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(bookings));
    }
}
