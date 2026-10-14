package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.PropertyDetailResponse;
import com.homestay.dto.response.PropertyResponse;
import com.homestay.entity.User;
import com.homestay.service.ManagerPropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** FR-06 US3 — Manager scoped property read API. */
@RestController
@RequestMapping("/api/v1/manager/properties")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class ManagerPropertyController {

    private final ManagerPropertyService managerPropertyService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PropertyResponse>>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return ResponseEntity.ok(ApiResponse.ok(
                managerPropertyService.listAssigned(currentUser, search, status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> getById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(managerPropertyService.getById(currentUser, id)));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> getDetail(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(managerPropertyService.getDetail(currentUser, id)));
    }
}
