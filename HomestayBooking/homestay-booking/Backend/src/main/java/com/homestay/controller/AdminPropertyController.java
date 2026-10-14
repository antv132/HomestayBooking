package com.homestay.controller;

import com.homestay.dto.request.AdminCreatePropertyRequest;
import com.homestay.dto.request.AdminUpdatePropertyRequest;
import com.homestay.dto.request.AssignManagerRequest;
import com.homestay.dto.response.AdminPropertyResponse;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.ManagerAssignmentResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.AdminPropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** FR-06 SCR-46–49 — Admin Property Management. */
@RestController
@RequestMapping({"/api/v1/admin/properties", "/api/admin/properties"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPropertyController {

    private final AdminPropertyService adminPropertyService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminPropertyResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID managerId,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        String kw = (keyword != null && !keyword.isBlank()) ? keyword : search;
        PageRequest pageable = PageRequest.of(page, size, parseAdminPropertySort(sort));
        return ResponseEntity.ok(ApiResponse.ok(
                adminPropertyService.listProperties(kw, status, managerId, pageable)));
    }

    private static Sort parseAdminPropertySort(String sort) {
        String field = "createdAt";
        Sort.Direction direction = Sort.Direction.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String requested = parts[0].trim();
            if ("name".equals(requested) || "createdAt".equals(requested)) {
                field = requested;
            }
            if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                direction = Sort.Direction.ASC;
            }
        }
        return Sort.by(direction, field);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminPropertyResponse>> create(
            @Valid @RequestBody AdminCreatePropertyRequest req,
            @AuthenticationPrincipal User currentUser) {
        AdminPropertyResponse data = adminPropertyService.createProperty(req, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo property thành công", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminPropertyResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminPropertyService.getProperty(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminPropertyResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdatePropertyRequest req,
            @AuthenticationPrincipal User currentUser) {
        AdminPropertyResponse data = adminPropertyService.updateProperty(id, req, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật property thành công", data));
    }

    @PatchMapping("/{id}/manager")
    public ResponseEntity<ApiResponse<AdminPropertyResponse>> assignManager(
            @PathVariable UUID id,
            @Valid @RequestBody AssignManagerRequest req,
            @AuthenticationPrincipal User currentUser) {
        AdminPropertyResponse data = adminPropertyService.assignManager(id, req.getManagerId(), currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Gán manager thành công", data));
    }

    @GetMapping("/{id}/assignments")
    public ResponseEntity<ApiResponse<PageResponse<ManagerAssignmentResponse>>> assignmentHistory(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(
                adminPropertyService.listAssignmentHistory(id, pageable)));
    }
}
