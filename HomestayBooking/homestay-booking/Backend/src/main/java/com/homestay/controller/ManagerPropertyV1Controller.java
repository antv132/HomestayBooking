package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.AssignedPropertyResponse;
import com.homestay.entity.User;
import com.homestay.service.PropertyKpisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** SCR-27 — Manager assigned properties for Property Selector. */
@RestController
@RequestMapping("/api/v1/managers")
@RequiredArgsConstructor
public class ManagerPropertyV1Controller {

    private final PropertyKpisService propertyKpisService;

    @GetMapping("/me/properties")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<AssignedPropertyResponse>>> getMyProperties(
            @AuthenticationPrincipal User currentUser) {
        List<AssignedPropertyResponse> data = propertyKpisService.getMyAssignedProperties(currentUser);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
