package com.homestay.service;

import com.homestay.dto.request.CreateReviewRequest;
import com.homestay.dto.request.UpdateReviewRequest;
import com.homestay.dto.response.MyReviewResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.Review;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ConflictException;
import com.homestay.exception.ForbiddenException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final RoomRatingAggregateService roomRatingAggregateService;
    private final ActivityLogService activityLogService;

    public ReviewService(
            ReviewRepository reviewRepository,
            BookingRepository bookingRepository,
            RoomRatingAggregateService roomRatingAggregateService,
            ActivityLogService activityLogService) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.roomRatingAggregateService = roomRatingAggregateService;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public MyReviewResponse submitReview(CreateReviewRequest request, User currentUser) {
        Booking booking = bookingRepository.findByIdWithRoomAndCustomer(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Đặt phòng không tồn tại"));

        if (!booking.getCustomer().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Bạn không thể đánh giá đơn đặt phòng của người khác");
        }

        if (booking.getStatus() != Booking.Status.CHECKED_OUT) {
            throw new BusinessException("Chỉ được đánh giá sau khi hoàn tất lưu trú (Đã trả phòng)");
        }

        if (reviewRepository.existsByBooking_Id(request.getBookingId())) {
            throw new ConflictException("Đơn đặt phòng này đã được đánh giá");
        }

        String comment = requireComment(request.getComment());

        Review review = new Review();
        review.setBooking(booking);
        review.setCustomer(currentUser);
        review.setRoom(booking.getRoom());
        review.setRating(request.getRating());
        review.setComment(comment);
        review.setStatus(Review.Status.PUBLISHED);

        Review saved = reviewRepository.save(review);
        roomRatingAggregateService.recalculate(saved.getRoom().getId());
        activityLogService.log(
                currentUser,
                ActivityLogService.REVIEW_CREATED,
                "Review",
                saved.getId(),
                "rating=" + saved.getRating());

        return reviewRepository.findByIdAndCustomer_Id(saved.getId(), currentUser.getId())
                .map(MyReviewResponse::fromEntity)
                .orElseGet(() -> MyReviewResponse.fromEntity(saved));
    }

    @Transactional(readOnly = true)
    public PageResponse<MyReviewResponse> getMyReviews(User currentUser, Pageable pageable) {
        Page<Review> result = reviewRepository.findByCustomer_IdOrderByCreatedAtDesc(currentUser.getId(), pageable);
        return new PageResponse<>(
                result.getContent().stream().map(MyReviewResponse::fromEntity).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public MyReviewResponse getReviewByIdForCustomer(UUID id, User currentUser) {
        Review review = reviewRepository.findByIdAndCustomer_Id(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
        return MyReviewResponse.fromEntity(review);
    }

    @Transactional
    public MyReviewResponse updateReview(UUID id, UpdateReviewRequest request, User currentUser) {
        Review review = reviewRepository.findByIdAndCustomer_Id(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));

        String comment = requireComment(request.getComment());

        review.setRating(request.getRating());
        review.setComment(comment);

        Review saved = reviewRepository.save(review);
        roomRatingAggregateService.recalculate(saved.getRoom().getId());
        activityLogService.log(
                currentUser,
                ActivityLogService.REVIEW_UPDATED,
                "Review",
                saved.getId(),
                "rating=" + saved.getRating());

        return reviewRepository.findByIdAndCustomer_Id(saved.getId(), currentUser.getId())
                .map(MyReviewResponse::fromEntity)
                .orElseGet(() -> MyReviewResponse.fromEntity(saved));
    }

    @Transactional
    public void deleteReview(UUID id, User currentUser) {
        Review review = reviewRepository.findByIdAndCustomer_Id(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));

        UUID roomId = review.getRoom().getId();
        UUID reviewId = review.getId();
        reviewRepository.delete(review);
        roomRatingAggregateService.recalculate(roomId);
        activityLogService.log(
                currentUser,
                ActivityLogService.REVIEW_DELETED,
                "Review",
                reviewId,
                "deleted");
    }

    private String requireComment(String comment) {
        if (comment == null || comment.isBlank()) {
            throw new BusinessException("Nội dung đánh giá là bắt buộc");
        }
        String normalized = comment.trim();
        if (normalized.length() > 1000) {
            throw new BusinessException("Bình luận không được vượt quá 1000 ký tự");
        }
        return normalized;
    }
}
