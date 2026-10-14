package com.homestay.controller;

import com.homestay.dto.request.ModerateReviewRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.ModerationReviewResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.ReviewModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** FR-14 SCR-65 — Manager property-scoped review moderation. */
@RestController
@RequestMapping("/api/v1/manager/reviews")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class ManagerReviewController {

    private final ReviewModerationService reviewModerationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ModerationReviewResponse>>> list(
            @AuthenticationPrincipal User manager,
            @RequestParam UUID propertyId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return ResponseEntity.ok(ApiResponse.ok(
                reviewModerationService.listForManager(
                        manager, propertyId, status, PageRequest.of(safePage, safeSize))));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ModerationReviewResponse>> moderate(
            @PathVariable UUID id,
            @Valid @RequestBody ModerateReviewRequest request,
            @AuthenticationPrincipal User manager) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Cập nhật trạng thái đánh giá thành công",
                reviewModerationService.moderateForManager(id, request, manager)));
    }
}
