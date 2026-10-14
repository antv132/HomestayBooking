package com.homestay.controller;

import com.homestay.dto.request.CreateEmployeeDamageReportRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.EmployeeDamageReportResponse;
import com.homestay.dto.response.EmployeeEligibleDamageRoomResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.DamageReport;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.service.EmployeeDamageReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** SCR-63 GET + SCR-64 create/upload/eligible. Resource /api/v1/employees/damage-reports. */
@RestController
@RequestMapping("/api/v1/employees/damage-reports")
@PreAuthorize("hasRole('EMPLOYEE')")
@RequiredArgsConstructor
public class EmployeeDamageReportController {

    private final EmployeeDamageReportService employeeDamageReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployeeDamageReportResponse>>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        DamageReport.Status statusFilter = parseStatus(status);
        PageResponse<EmployeeDamageReportResponse> data = employeeDamageReportService.list(
                currentUser, statusFilter, search, PageRequest.of(safePage, safeSize));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    private static DamageReport.Status parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return DamageReport.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Trạng thái không hợp lệ");
        }
    }

    /** SCR-64 — Rooms with FAILED inspection that still need a damage report. */
    @GetMapping("/eligible-rooms")
    public ResponseEntity<ApiResponse<List<EmployeeEligibleDamageRoomResponse>>> eligibleRooms(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(
                employeeDamageReportService.listEligibleRooms(currentUser)));
    }

    /** SCR-64 — Upload evidence photos before create. */
    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<String>>> uploadPhotos(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("files") List<MultipartFile> files) {
        List<String> urls = employeeDamageReportService.uploadPhotos(currentUser, files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đã tải ảnh lên", urls));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeDamageReportResponse>> create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateEmployeeDamageReportRequest request) {
        EmployeeDamageReportResponse data = employeeDamageReportService.create(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đã tạo báo cáo hư hại", data));
    }
}
