package com.homestay.controller;

import com.homestay.dto.request.CreateFloorRequest;
import com.homestay.dto.request.UpdateFloorRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.FloorResponse;
import com.homestay.entity.User;
import com.homestay.service.FloorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** SCR-28 — Floor CRUD v1 (Manager, property-scoped). */
@RestController
@RequestMapping("/api/v1/managers/properties/{propertyId}/floors")
@RequiredArgsConstructor
public class FloorV1Controller {

    private final FloorService floorService;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<FloorResponse>>> getByProperty(
            @PathVariable UUID propertyId,
            @AuthenticationPrincipal User currentUser) {
        List<FloorResponse> data = floorService.getByPropertyForManager(currentUser, propertyId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FloorResponse>> create(
            @PathVariable UUID propertyId,
            @Valid @RequestBody CreateFloorRequest request,
            @AuthenticationPrincipal User currentUser) {
        request.setPropertyId(propertyId.toString());
        FloorResponse res = floorService.createForManager(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Tạo tầng thành công", res));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FloorResponse>> update(
            @PathVariable UUID propertyId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFloorRequest request,
            @AuthenticationPrincipal User currentUser) {
        FloorResponse res = floorService.updateForManager(currentUser, propertyId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật tầng thành công", res));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID propertyId,
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        floorService.deleteForManager(currentUser, propertyId, id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa tầng thành công"));
    }
}
