package com.homestay.service;

import com.homestay.dto.request.ModerateReviewRequest;
import com.homestay.dto.response.ModerationReviewResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Review;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/** FR-14 — Manager/Admin Hide/Show reviews + lists. */
@Service
public class ReviewModerationService {

    private final ReviewRepository reviewRepository;
    private final RoomRatingAggregateService roomRatingAggregateService;
    private final ReportPropertyScopeValidator scopeValidator;
    private final ActivityLogService activityLogService;

    public ReviewModerationService(
            ReviewRepository reviewRepository,
            RoomRatingAggregateService roomRatingAggregateService,
            ReportPropertyScopeValidator scopeValidator,
            ActivityLogService activityLogService) {
        this.reviewRepository = reviewRepository;
        this.roomRatingAggregateService = roomRatingAggregateService;
        this.scopeValidator = scopeValidator;
        this.activityLogService = activityLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ModerationReviewResponse> listForManager(
            User manager, UUID propertyId, String status, Pageable pageable) {
        if (propertyId == null) {
            throw new BusinessException("propertyId là bắt buộc");
        }
        scopeValidator.validateManagerAccess(manager, propertyId);
        Review.Status statusFilter = parseStatusOrNull(status);
        Page<Review> page = reviewRepository.findByPropertyIdAndOptionalStatus(
                propertyId, statusFilter, pageable);
        return toPage(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<ModerationReviewResponse> listForAdmin(String status, Pageable pageable) {
        Review.Status statusFilter = parseStatusOrNull(status);
        Page<Review> page = reviewRepository.findAllByOptionalStatus(statusFilter, pageable);
        return toPage(page);
    }

    @Transactional
    public ModerationReviewResponse moderateForManager(
            UUID reviewId, ModerateReviewRequest request, User manager) {
        Review review = requireReview(reviewId);
        UUID propertyId = review.getRoom().getProperty().getId();
        scopeValidator.validateManagerAccess(manager, propertyId);
        return applyStatus(review, request, manager);
    }

    @Transactional
    public ModerationReviewResponse moderateForAdmin(
            UUID reviewId, ModerateReviewRequest request, User admin) {
        Review review = requireReview(reviewId);
        return applyStatus(review, request, admin);
    }

    private ModerationReviewResponse applyStatus(Review review, ModerateReviewRequest request, User actor) {
        Review.Status newStatus = parseStatusRequired(request.getStatus());
        Review.Status previous = review.getStatus();
        if (previous == newStatus) {
            return ModerationReviewResponse.fromEntity(review);
        }

        review.setStatus(newStatus);
        review.setModeratedBy(actor);
        review.setModeratedAt(LocalDateTime.now());
        Review saved = reviewRepository.save(review);

        roomRatingAggregateService.recalculate(saved.getRoom().getId());

        String action = newStatus == Review.Status.HIDDEN
                ? ActivityLogService.REVIEW_HIDDEN
                : ActivityLogService.REVIEW_PUBLISHED;
        activityLogService.log(
                actor,
                action,
                "Review",
                saved.getId(),
                previous.name() + " → " + newStatus.name());

        return reviewRepository.findWithDetailsById(saved.getId())
                .map(ModerationReviewResponse::fromEntity)
                .orElseGet(() -> ModerationReviewResponse.fromEntity(saved));
    }

    private Review requireReview(UUID reviewId) {
        return reviewRepository.findWithDetailsById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
    }

    private PageResponse<ModerationReviewResponse> toPage(Page<Review> page) {
        return new PageResponse<>(
                page.getContent().stream().map(ModerationReviewResponse::fromEntity).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private Review.Status parseStatusOrNull(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return parseStatusRequired(status);
    }

    private Review.Status parseStatusRequired(String status) {
        try {
            Review.Status parsed = Review.Status.valueOf(status.trim().toUpperCase());
            if (parsed != Review.Status.PUBLISHED && parsed != Review.Status.HIDDEN) {
                throw new BusinessException("Trạng thái kiểm duyệt không hợp lệ");
            }
            return parsed;
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Trạng thái kiểm duyệt không hợp lệ");
        }
    }
}
