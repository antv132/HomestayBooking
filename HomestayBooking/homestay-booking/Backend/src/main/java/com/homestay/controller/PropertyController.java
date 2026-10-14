package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.FeaturedPropertyResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.PropertyResponse;
import com.homestay.dto.response.PropertyStructureResponse;
import com.homestay.entity.User;
import com.homestay.service.FloorService;
import com.homestay.service.PropertyService;
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

import java.util.List;
import java.util.UUID;

/**
 * Public property discovery + legacy structure endpoint.
 * FR-06: Manager create/update/delete removed — use Admin APIs.
 * Manager scoped reads: {@code /api/v1/manager/properties}.
 */
@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;
    private final FloorService floorService;

    public PropertyController(PropertyService propertyService, FloorService floorService) {
        this.propertyService = propertyService;
        this.floorService = floorService;
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<FeaturedPropertyResponse>>> getFeatured(
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(propertyService.getFeatured(limit)));
    }

    /** Guest discovery — defaults to ACTIVE only (FR-06 / FR-03). */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PropertyResponse>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(propertyService.getAllPublic(search, status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(propertyService.getPublicById(id)));
    }

    /** Legacy structure tree — Manager + assignment (prefer /api/v1/properties/{id}/tree) */
    @GetMapping("/{id}/structure")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PropertyStructureResponse>> getStructure(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(floorService.getStructureForManager(currentUser, id)));
    }
}
