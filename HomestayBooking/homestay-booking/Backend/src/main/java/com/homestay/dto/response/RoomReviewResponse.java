package com.homestay.dto.response;

import com.homestay.entity.Review;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RoomReviewResponse {

    private UUID id;
    private String customerName;
    private String avatarUrl;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    public static RoomReviewResponse fromEntity(Review review) {
        RoomReviewResponse res = new RoomReviewResponse();
        res.setId(review.getId());
        res.setCustomerName(review.getCustomer().getFullName());
        res.setAvatarUrl(review.getCustomer().getAvatarUrl());
        res.setRating(review.getRating());
        res.setComment(review.getComment());
        res.setCreatedAt(review.getCreatedAt());
        return res;
    }
}
