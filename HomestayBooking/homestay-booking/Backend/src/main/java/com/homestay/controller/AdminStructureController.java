package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.PropertyStructureResponse;
import com.homestay.service.FloorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** FR-07 / SCR-28 — Admin read-only structure tree (all properties). */
@RestController
@RequestMapping("/api/v1/admin/properties")
@RequiredArgsConstructor
public class AdminStructureController {

    private final FloorService floorService;

    @GetMapping("/{propertyId}/tree")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PropertyStructureResponse>> getTree(@PathVariable UUID propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(floorService.getStructure(propertyId)));
    }
}
