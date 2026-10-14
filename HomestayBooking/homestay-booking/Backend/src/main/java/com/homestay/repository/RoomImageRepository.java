package com.homestay.repository;

import com.homestay.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImage, UUID> {

    // Lấy tất cả ảnh của phòng, sắp theo thứ tự hiển thị
    List<RoomImage> findByRoomIdOrderBySortOrderAsc(UUID roomId);

    long countByRoomId(UUID roomId);

    // Bỏ tất cả ảnh primary của phòng trước khi set ảnh mới làm primary
    @Modifying
    @Query("UPDATE RoomImage ri SET ri.isPrimary = false WHERE ri.room.id = :roomId")
    void clearPrimaryByRoomId(@Param("roomId") UUID roomId);

    // Xóa tất cả ảnh của phòng
    void deleteAllByRoomId(UUID roomId);
}
