package com.homestay.dto.response;

import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.RoomImage;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class FeaturedPropertyResponse {

    private UUID id;
    private String name;
    private String address;
    private int roomCount;
    private int availableRoomCount;
    private String coverImageUrl;

    public static FeaturedPropertyResponse fromEntity(Property property) {
        FeaturedPropertyResponse res = new FeaturedPropertyResponse();
        res.setId(property.getId());
        res.setName(property.getName());
        res.setAddress(property.getAddress());

        List<Room> rooms = property.getRooms() != null ? property.getRooms() : List.of();
        res.setRoomCount(rooms.size());
        res.setAvailableRoomCount((int) rooms.stream()
                .filter(r -> r.getStatus() == Room.Status.AVAILABLE)
                .count());

        // Lấy ảnh bìa: ưu tiên ảnh primary, fallback ảnh đầu tiên có trong bất kỳ phòng nào
        String coverImage = rooms.stream()
                .filter(r -> r.getRoomImages() != null && !r.getRoomImages().isEmpty())
                .flatMap(r -> r.getRoomImages().stream())
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(RoomImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> rooms.stream()
                        .filter(r -> r.getRoomImages() != null && !r.getRoomImages().isEmpty())
                        .flatMap(r -> r.getRoomImages().stream())
                        .map(RoomImage::getImageUrl)
                        .findFirst()
                        .orElse(null));

        res.setCoverImageUrl(coverImage);
        return res;
    }
}
