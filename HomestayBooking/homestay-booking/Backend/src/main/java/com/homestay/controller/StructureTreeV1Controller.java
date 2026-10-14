package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.PropertyStructureResponse;
import com.homestay.entity.User;
import com.homestay.service.FloorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** SCR-28 — Structure tree v1 (Property → Floor → Room). */
@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
public class StructureTreeV1Controller {

    private final FloorService floorService;

    @GetMapping("/{propertyId}/tree")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PropertyStructureResponse>> getTree(
            @PathVariable UUID propertyId,
            @AuthenticationPrincipal User currentUser) {
        PropertyStructureResponse data = floorService.getStructureForManager(currentUser, propertyId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{propertyId}/structure")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PropertyStructureResponse>> getStructure(
            @PathVariable UUID propertyId,
            @AuthenticationPrincipal User currentUser) {
        PropertyStructureResponse data = floorService.getStructureForManager(currentUser, propertyId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
