package com.homestay.repository;

import com.homestay.entity.Floor;
import com.homestay.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FloorRepository extends JpaRepository<Floor, UUID> {

    // Lấy tất cả tầng của một property, sắp xếp theo số tầng
    List<Floor> findByPropertyOrderByFloorNumberAsc(Property property);

    // Kiểm tra số tầng đã tồn tại trong property chưa
    boolean existsByPropertyAndFloorNumber(Property property, Integer floorNumber);

    // Đếm số phòng trong tầng (tránh xóa tầng có phòng)
    boolean existsByPropertyId(UUID propertyId);

    // SCR-37: Lấy tất cả tầng + rooms của property — 1 query, tránh N+1 và MultipleBagFetchException
    // Chỉ fetch MỘT collection (rooms) nên an toàn với Hibernate List/Bag
    @Query("SELECT DISTINCT f FROM Floor f " +
           "LEFT JOIN FETCH f.rooms " +
           "WHERE f.property.id = :propertyId " +
           "ORDER BY f.floorNumber ASC")
    List<Floor> findByPropertyIdWithRooms(@Param("propertyId") UUID propertyId);
}

