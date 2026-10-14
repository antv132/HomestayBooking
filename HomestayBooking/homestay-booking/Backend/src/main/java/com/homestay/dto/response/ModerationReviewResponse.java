package com.homestay.dto.response;

import com.homestay.entity.Review;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/** FR-14 SCR-65 / SCR-56 — moderation list row. */
@Data
public class ModerationReviewResponse {

    private UUID id;
    private UUID bookingId;
    private UUID roomId;
    private String roomNumber;
    private String propertyName;
    private UUID propertyId;
    private String customerName;
    private Integer rating;
    private String comment;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime moderatedAt;

    public static ModerationReviewResponse fromEntity(Review review) {
        ModerationReviewResponse res = new ModerationReviewResponse();
        res.setId(review.getId());
        res.setBookingId(review.getBooking().getId());
        res.setRoomId(review.getRoom().getId());
        res.setRoomNumber(review.getRoom().getRoomNumber());
        res.setPropertyName(review.getRoom().getProperty().getName());
        res.setPropertyId(review.getRoom().getProperty().getId());
        res.setCustomerName(review.getCustomer().getFullName());
        res.setRating(review.getRating());
        res.setComment(review.getComment());
        res.setStatus(review.getStatus().name());
        res.setCreatedAt(review.getCreatedAt());
        res.setModeratedAt(review.getModeratedAt());
        return res;
    }
}
