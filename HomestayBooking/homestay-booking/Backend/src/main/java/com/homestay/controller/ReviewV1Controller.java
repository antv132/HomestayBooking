package com.homestay.controller;

import com.homestay.dto.request.CreateReviewRequest;
import com.homestay.dto.request.UpdateReviewRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.MyReviewResponse;
import com.homestay.entity.User;
import com.homestay.service.ReviewService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** SCR-25 — Customer review write (api-spec v1). */
@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewV1Controller {

    private final ReviewService reviewService;

    public ReviewV1Controller(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<MyReviewResponse>> submitReview(
            @jakarta.validation.Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal User currentUser) {
        MyReviewResponse data = reviewService.submitReview(request, currentUser);
        return ResponseEntity.status(201).body(ApiResponse.ok("Review submitted successfully.", data));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<MyReviewResponse>> getReviewById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        MyReviewResponse data = reviewService.getReviewByIdForCustomer(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<MyReviewResponse>> updateReview(
            @PathVariable UUID id,
            @jakarta.validation.Valid @RequestBody UpdateReviewRequest request,
            @AuthenticationPrincipal User currentUser) {
        MyReviewResponse data = reviewService.updateReview(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Review updated successfully.", data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        reviewService.deleteReview(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Review deleted successfully."));
    }
}
