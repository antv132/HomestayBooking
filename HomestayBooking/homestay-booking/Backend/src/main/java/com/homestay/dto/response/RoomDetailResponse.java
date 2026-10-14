package com.homestay.dto.response;

import com.homestay.entity.Review;
import com.homestay.entity.Room;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// Response chi tiết dùng trong trang chi tiết phòng (SCR-08)
@Data
public class RoomDetailResponse {

    private UUID id;
    private UUID propertyId;
    private String propertyName;
    private String propertyAddress;
    private UUID floorId;
    private Integer floorNumber;
    private String roomNumber;
    private String roomType;
    private BigDecimal pricePerNight;
    private Integer capacity;
    private BigDecimal area;
    private String description;
    private List<String> amenities;
    private String status;
    private List<RoomImageInfo> images;
    private double averageRating;
    private int totalReviews;
    private List<ReviewInfo> reviews;
    private LocalDateTime createdAt;

    @Data
    public static class RoomImageInfo {
        private UUID id;
        private String imageUrl;
        private Boolean isPrimary;
        private Integer sortOrder;
    }

    @Data
    public static class ReviewInfo {
        private UUID id;
        private String customerName;
        private Integer rating;
        private String comment;
        private LocalDateTime createdAt;
    }

    public static RoomDetailResponse fromEntity(Room room) {
        RoomDetailResponse res = new RoomDetailResponse();
        res.setId(room.getId());
        res.setPropertyId(room.getProperty().getId());
        res.setPropertyName(room.getProperty().getName());
        res.setPropertyAddress(room.getProperty().getAddress());
        res.setFloorId(room.getFloor().getId());
        res.setFloorNumber(room.getFloor().getFloorNumber());
        res.setRoomNumber(room.getRoomNumber());
        res.setRoomType(room.getRoomType());
        res.setPricePerNight(room.getPricePerNight());
        res.setCapacity(room.getCapacity());
        res.setArea(room.getArea());
        res.setDescription(room.getDescription());
        res.setAmenities(room.getAmenities() != null
                ? room.getAmenities()
                : Collections.emptyList());
        res.setStatus(room.getStatus().name());
        res.setCreatedAt(room.getCreatedAt());

        // Map ảnh
        if (room.getRoomImages() != null) {
            List<RoomImageInfo> images = room.getRoomImages().stream().map(img -> {
                RoomImageInfo info = new RoomImageInfo();
                info.setId(img.getId());
                info.setImageUrl(img.getImageUrl());
                info.setIsPrimary(img.getIsPrimary());
                info.setSortOrder(img.getSortOrder());
                return info;
            }).collect(Collectors.toList());
            res.setImages(images);
        }

        // FR-14 aggregates — prefer denormalized room columns; fallback to in-memory Published
        if (room.getTotalReviews() != null && room.getTotalReviews() > 0 && room.getAverageRating() != null) {
            res.setAverageRating(room.getAverageRating().doubleValue());
            res.setTotalReviews(room.getTotalReviews());
        } else if (room.getReviews() != null) {
            List<Review> published = room.getReviews().stream()
                    .filter(r -> r.getStatus() == Review.Status.PUBLISHED)
                    .collect(Collectors.toList());

            double avg = published.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0);
            res.setAverageRating(Math.round(avg * 10.0) / 10.0);
            res.setTotalReviews(published.size());
        }

        if (room.getReviews() != null) {
            List<ReviewInfo> reviewInfos = room.getReviews().stream()
                    .filter(r -> r.getStatus() == Review.Status.PUBLISHED)
                    .map(r -> {
                        ReviewInfo info = new ReviewInfo();
                        info.setId(r.getId());
                        info.setCustomerName(r.getCustomer().getFullName());
                        info.setRating(r.getRating());
                        info.setComment(r.getComment());
                        info.setCreatedAt(r.getCreatedAt());
                        return info;
                    }).collect(Collectors.toList());
            res.setReviews(reviewInfos);
        }

        return res;
    }
}
