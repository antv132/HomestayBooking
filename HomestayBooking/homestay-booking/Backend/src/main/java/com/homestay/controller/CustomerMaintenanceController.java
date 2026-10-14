package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.entity.User;
import com.homestay.service.MaintenanceTicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** SCR-22/23 — Customer maintenance tickets (api-spec v1). */
@RestController
@RequestMapping("/api/v1/maintenance-tickets")
public class CustomerMaintenanceController {

    private final MaintenanceTicketService ticketService;

    public CustomerMaintenanceController(MaintenanceTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyTickets(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> result = ticketService.getCustomerTicketsPaged(
                currentUser.getId(), status, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Object>> getTicketDetail(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getTicketDetail(currentUser, id)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTicket(
            @AuthenticationPrincipal User currentUser,
            @RequestParam UUID bookingId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        Map<String, Object> data = ticketService.createTicketFromBooking(
                currentUser.getId(), bookingId, title, description, files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo yêu cầu bảo trì thành công", data));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Object>> updateTicket(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "existingPhotoUrls", required = false) List<String> existingPhotoUrls,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos) {
        List<MultipartFile> uploads = files != null ? files : photos;
        Object data = ticketService.updateTicketContent(
                currentUser.getId(), id, title, description, existingPhotoUrls, uploads);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật yêu cầu bảo trì thành công", data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        ticketService.deleteTicket(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa yêu cầu bảo trì"));
    }
}
