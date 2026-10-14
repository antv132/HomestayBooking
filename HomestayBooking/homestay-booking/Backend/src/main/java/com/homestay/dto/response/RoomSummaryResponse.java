package com.homestay.dto.response;

import com.homestay.entity.Room;
import com.homestay.entity.RoomImage;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

// Response tóm tắt dùng trong listing card (SCR-07, SCR-09)
@Data
public class RoomSummaryResponse {

    private UUID id;
    private UUID propertyId;
    private String propertyName;
    private UUID floorId;
    private Integer floorNumber;
    private String roomNumber;
    private String roomType;
    private BigDecimal pricePerNight;
    private Integer capacity;
    private BigDecimal area;
    private String status;
    private String primaryImageUrl;  // ảnh bìa
    /** FR-14 denormalized Published aggregates */
    private Double averageRating;
    private Integer totalReviews;

    public static RoomSummaryResponse fromEntity(Room room) {
        RoomSummaryResponse res = new RoomSummaryResponse();
        res.setId(room.getId());
        res.setPropertyId(room.getProperty().getId());
        res.setPropertyName(room.getProperty().getName());
        res.setFloorId(room.getFloor().getId());
        res.setFloorNumber(room.getFloor().getFloorNumber());
        res.setRoomNumber(room.getRoomNumber());
        res.setRoomType(room.getRoomType());
        res.setPricePerNight(room.getPricePerNight());
        res.setCapacity(room.getCapacity());
        res.setArea(room.getArea());
        res.setStatus(room.getStatus().name());
        res.setTotalReviews(room.getTotalReviews() != null ? room.getTotalReviews() : 0);
        res.setAverageRating(room.getAverageRating() != null ? room.getAverageRating().doubleValue() : 0.0);

        // Lấy ảnh primary
        if (room.getRoomImages() != null) {
            room.getRoomImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .findFirst()
                    .map(RoomImage::getImageUrl)
                    .ifPresent(res::setPrimaryImageUrl);
        }

        return res;
    }
}
