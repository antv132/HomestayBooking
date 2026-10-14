package com.homestay.controller;

import com.homestay.dto.request.PatchPromotionActiveRequest;
import com.homestay.dto.request.PromotionRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.PromotionResponse;
import com.homestay.service.AdminPromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/** FR-18 SCR-57/58 — Admin Promotion Management. */
@RestController
@RequestMapping({"/api/v1/admin/promotions", "/api/admin/promotions"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPromotionController {

    private final AdminPromotionService adminPromotionService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PromotionResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 100));
        return ResponseEntity.ok(ApiResponse.ok(adminPromotionService.listForAdmin(pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PromotionResponse>> create(
            @Valid @RequestBody PromotionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo promotion thành công", adminPromotionService.create(req)));
    }

    /** Upload banner image → {@code { url: "/uploads/promotions/..." }}. */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(
            @RequestParam("file") MultipartFile file) {
        String url = adminPromotionService.uploadImage(file);
        return ResponseEntity.ok(ApiResponse.ok("Upload ảnh thành công", Map.of("url", url)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody PromotionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Cập nhật promotion thành công",
                adminPromotionService.update(id, req)));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<PromotionResponse>> patchActive(
            @PathVariable UUID id,
            @Valid @RequestBody PatchPromotionActiveRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Cập nhật trạng thái thành công",
                adminPromotionService.patchActive(id, req.getIsActive())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminPromotionService.deleteForAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
