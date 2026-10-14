package com.homestay.controller;

import com.homestay.dto.request.CreateFloorRequest;
import com.homestay.dto.request.UpdateFloorRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.FloorResponse;
import com.homestay.entity.User;
import com.homestay.service.FloorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Legacy floor endpoints. SCR-28 prefers {@code /api/v1/manager/floors}.
 * All operations require Manager + ACTIVE property assignment.
 */
@RestController
@RequestMapping("/api/floors")
public class FloorController {

    private final FloorService floorService;

    public FloorController(FloorService floorService) {
        this.floorService = floorService;
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<FloorResponse>>> getByProperty(
            @RequestParam UUID propertyId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(floorService.getByPropertyForManager(currentUser, propertyId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FloorResponse>> create(
            @Valid @RequestBody CreateFloorRequest request,
            @AuthenticationPrincipal User currentUser) {
        FloorResponse res = floorService.createForManager(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Tạo tầng thành công", res));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FloorResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFloorRequest request,
            @AuthenticationPrincipal User currentUser) {
        FloorResponse res = floorService.updateForManager(currentUser, null, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật tầng thành công", res));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        floorService.deleteForManager(currentUser, null, id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa tầng thành công"));
    }
}
