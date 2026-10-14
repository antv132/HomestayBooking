package com.homestay.dto.response;

import com.homestay.entity.Review;
import com.homestay.entity.RoomImage;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class MyReviewResponse {
    private UUID id;
    private UUID bookingId;
    private String roomNumber;
    private String propertyName;
    private Integer rating;
    private String comment;
    private String status;
    private LocalDateTime createdAt;
    private String roomImageUrl;

    public static MyReviewResponse fromEntity(Review review) {
        MyReviewResponse res = new MyReviewResponse();
        res.setId(review.getId());
        res.setBookingId(review.getBooking().getId());
        res.setRoomNumber(review.getRoom().getRoomNumber());
        res.setPropertyName(review.getRoom().getProperty().getName());
        res.setRating(review.getRating());
        res.setComment(review.getComment());
        res.setStatus(review.getStatus().name());
        res.setCreatedAt(review.getCreatedAt());

        if (review.getRoom().getRoomImages() != null && !review.getRoom().getRoomImages().isEmpty()) {
            review.getRoom().getRoomImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .findFirst()
                    .map(RoomImage::getImageUrl)
                    .ifPresentOrElse(res::setRoomImageUrl, () -> {
                        res.setRoomImageUrl(review.getRoom().getRoomImages().get(0).getImageUrl());
                    });
        }
        return res;
    }
}
