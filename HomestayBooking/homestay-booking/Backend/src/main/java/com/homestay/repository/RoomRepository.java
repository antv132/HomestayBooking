package com.homestay.repository;

import com.homestay.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    // Tìm phòng theo property
    Page<Room> findByPropertyId(UUID propertyId, Pageable pageable);

    long countByPropertyId(UUID propertyId);

    long countByPropertyIdAndStatus(UUID propertyId, Room.Status status);

    // Tìm phòng theo floor
    List<Room> findByFloorId(UUID floorId);

    // Lọc phòng theo status
    Page<Room> findByStatus(Room.Status status, Pageable pageable);

    /** Featured / discovery — only rooms on ACTIVE properties (FR-001 / FR-009 / SC-006). */
    @Query("""
        SELECT r FROM Room r
        WHERE r.status = :status
        AND r.deletedAt IS NULL
        AND r.property.status = 'ACTIVE'
        """)
    Page<Room> findByStatusAndActiveProperty(@Param("status") Room.Status status, Pageable pageable);

    boolean existsByPropertyIdAndRoomNumberAndDeletedAtIsNull(UUID propertyId, String roomNumber);

    boolean existsByPropertyIdAndRoomNumberAndDeletedAtIsNullAndIdNot(
            UUID propertyId, String roomNumber, UUID id);

    /** Active rooms on floor (floor delete guard). */
    long countByFloorIdAndDeletedAtIsNull(UUID floorId);

    // Đếm phòng theo status — dùng cho Dashboard KPI
    long countByStatus(Room.Status status);

    // Tìm phòng available theo property
    Page<Room> findByPropertyIdAndStatus(UUID propertyId, Room.Status status, Pageable pageable);

    // Tìm kiếm phòng theo tên phòng, loại phòng
    Page<Room> findByRoomNumberContainingIgnoreCaseOrRoomTypeContainingIgnoreCase(
            String roomNumber, String roomType, Pageable pageable);

    /** Pessimistic lock — SQL Server race safety on create booking. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Half-open inventory [checkIn, checkOut) — Spec Sync 2026-07-23.
     * Overlap when {@code other.checkIn < checkOut AND other.checkOut > checkIn}.
     */
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.room.id = :roomId
        AND b.status NOT IN ('CANCELLED', 'CHECKED_OUT', 'NO_SHOW')
        AND b.checkInDate < :checkOut
        AND b.checkOutDate > :checkIn
    """)
    boolean existsOverlapBooking(@Param("roomId") UUID roomId,
                                 @Param("checkIn") LocalDate checkIn,
                                 @Param("checkOut") LocalDate checkOut);

    /** Overlap check excluding a booking (FR-015 modify) — half-open. */
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.room.id = :roomId
        AND b.id <> :excludeBookingId
        AND b.status NOT IN ('CANCELLED', 'CHECKED_OUT', 'NO_SHOW')
        AND b.checkInDate < :checkOut
        AND b.checkOutDate > :checkIn
    """)
    boolean existsOverlapBookingExcluding(
            @Param("roomId") UUID roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("excludeBookingId") UUID excludeBookingId);

    /** True if room still has any inventory-blocking booking. */
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.room.id = :roomId
        AND b.status NOT IN ('CANCELLED', 'CHECKED_OUT', 'NO_SHOW')
    """)
    boolean hasBlockingBookings(@Param("roomId") UUID roomId);

    // Lấy các khoảng ngày đã đặt kèm trạng thái booking (dùng cho SCR-10 calendar)
    @Query("""
        SELECT b.checkInDate, b.checkOutDate, b.status FROM Booking b
        WHERE b.room.id = :roomId
        AND b.status NOT IN ('CANCELLED', 'CHECKED_OUT', 'NO_SHOW')
        AND b.checkOutDate >= CURRENT_DATE
        ORDER BY b.checkInDate
    """)
    List<Object[]> findBookedDateRanges(@Param("roomId") UUID roomId);

    /**
     * Active booking overlaps inclusive block [startDate, endDate] using half-open stays
     * [checkIn, checkOut): overlap when checkIn &lt;= endDate AND checkOut &gt; startDate.
     */
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.room.id = :roomId
        AND b.status IN ('PENDING_DEPOSIT', 'CONFIRMED', 'CHECKED_IN', 'PENDING_INSPECTION', 'PENDING_DAMAGE_PAYMENT')
        AND b.checkInDate <= :endDate
        AND b.checkOutDate > :startDate
        """)
    boolean existsActiveBookingOverlapHalfOpen(
            @Param("roomId") UUID roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // SCR-09: blocking bookings overlapping a date window
    @Query("""
        SELECT b.checkInDate, b.checkOutDate, b.status FROM Booking b
        WHERE b.room.id = :roomId
        AND b.status IN ('PENDING_DEPOSIT', 'CONFIRMED', 'CHECKED_IN', 'PENDING_INSPECTION', 'PENDING_DAMAGE_PAYMENT')
        AND b.checkInDate <= :endDate
        AND b.checkOutDate > :startDate
        ORDER BY b.checkInDate
    """)
    List<Object[]> findBlockingBookingsInRange(@Param("roomId") UUID roomId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    /** Same window as {@link #findBlockingBookingsInRange} but includes booking id for staff tooltips. */
    @Query("""
        SELECT b.id, b.checkInDate, b.checkOutDate, b.status FROM Booking b
        WHERE b.room.id = :roomId
        AND b.status IN ('PENDING_DEPOSIT', 'CONFIRMED', 'CHECKED_IN', 'PENDING_INSPECTION', 'PENDING_DAMAGE_PAYMENT')
        AND b.checkInDate <= :endDate
        AND b.checkOutDate > :startDate
        ORDER BY b.checkInDate
    """)
    List<Object[]> findBlockingBookingsWithIdInRange(@Param("roomId") UUID roomId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    // Public listing filter — SCR-07/SCR-09 (keyword: tên/địa chỉ homestay, số phòng, loại phòng)
    @Query("""
        SELECT r FROM Room r
        WHERE (
            :keyword1 IS NULL OR
            LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword1, '%')) OR
            LOWER(r.roomType) LIKE LOWER(CONCAT('%', :keyword1, '%')) OR
            LOWER(r.property.name) LIKE LOWER(CONCAT('%', :keyword1, '%')) OR
            LOWER(r.property.address) LIKE LOWER(CONCAT('%', :keyword1, '%')) OR
            (:keyword2 IS NOT NULL AND (
                LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword2, '%')) OR
                LOWER(r.roomType) LIKE LOWER(CONCAT('%', :keyword2, '%')) OR
                LOWER(r.property.name) LIKE LOWER(CONCAT('%', :keyword2, '%')) OR
                LOWER(r.property.address) LIKE LOWER(CONCAT('%', :keyword2, '%'))
            )) OR
            (:keyword3 IS NOT NULL AND (
                LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword3, '%')) OR
                LOWER(r.roomType) LIKE LOWER(CONCAT('%', :keyword3, '%')) OR
                LOWER(r.property.name) LIKE LOWER(CONCAT('%', :keyword3, '%')) OR
                LOWER(r.property.address) LIKE LOWER(CONCAT('%', :keyword3, '%'))
            )) OR
            (:keyword4 IS NOT NULL AND (
                LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword4, '%')) OR
                LOWER(r.roomType) LIKE LOWER(CONCAT('%', :keyword4, '%')) OR
                LOWER(r.property.name) LIKE LOWER(CONCAT('%', :keyword4, '%')) OR
                LOWER(r.property.address) LIKE LOWER(CONCAT('%', :keyword4, '%'))
            )) OR
            (:keyword5 IS NOT NULL AND (
                LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword5, '%')) OR
                LOWER(r.roomType) LIKE LOWER(CONCAT('%', :keyword5, '%')) OR
                LOWER(r.property.name) LIKE LOWER(CONCAT('%', :keyword5, '%')) OR
                LOWER(r.property.address) LIKE LOWER(CONCAT('%', :keyword5, '%'))
            ))
        )
        AND r.property.status = 'ACTIVE'
        AND r.deletedAt IS NULL
        AND (:status IS NULL OR r.status = :status)
        AND (:propertyId IS NULL OR r.property.id = :propertyId)
        AND (:roomTypes IS NULL OR CONCAT(',', :roomTypes, ',') LIKE CONCAT('%,', r.roomType, ',%'))
        AND (:minPrice IS NULL OR r.pricePerNight >= :minPrice)
        AND (:maxPrice IS NULL OR r.pricePerNight <= :maxPrice)
        AND (:capacity IS NULL OR r.capacity >= :capacity)
        AND (:checkIn IS NULL OR :checkOut IS NULL OR r.status NOT IN (
            'MAINTENANCE', 'OUT_OF_SERVICE', 'PENDING_CLEANING', 'CLEANING_IN_PROGRESS'
        ))
        AND (:checkIn IS NULL OR :checkOut IS NULL OR NOT EXISTS (
            SELECT b FROM Booking b
            WHERE b.room = r
            AND b.status NOT IN ('CANCELLED', 'CHECKED_OUT', 'NO_SHOW')
            AND b.checkInDate < :checkOut
            AND b.checkOutDate > :checkIn
        ))
    """)
    Page<Room> findPublicWithFilters(
            @Param("keyword1")   String keyword1,
            @Param("keyword2")   String keyword2,
            @Param("keyword3")   String keyword3,
            @Param("keyword4")   String keyword4,
            @Param("keyword5")   String keyword5,
            @Param("status")     Room.Status status,
            @Param("propertyId") UUID propertyId,
            @Param("roomTypes")  String roomTypes,
            @Param("minPrice")   java.math.BigDecimal minPrice,
            @Param("maxPrice")   java.math.BigDecimal maxPrice,
            @Param("capacity")   Integer capacity,
            @Param("checkIn")    LocalDate checkIn,
            @Param("checkOut")   LocalDate checkOut,
            Pageable pageable
    );

    // SCR-39: Combined filter dành cho Manager — hỗ trợ search + status + propertyId + floorId + roomType
    @Query("""
        SELECT r FROM Room r
        WHERE r.deletedAt IS NULL
        AND (:search IS NULL OR
               LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(r.roomType)   LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:status    IS NULL OR r.status        = :status)
        AND (:propertyId IS NULL OR r.property.id = :propertyId)
        AND (:floorId    IS NULL OR r.floor.id    = :floorId)
        AND (:roomType   IS NULL OR LOWER(r.roomType) = LOWER(:roomType))
    """)
    Page<Room> findWithFilters(
            @Param("search")     String search,
            @Param("status")     Room.Status status,
            @Param("propertyId") UUID propertyId,
            @Param("floorId")    UUID floorId,
            @Param("roomType")   String roomType,
            Pageable pageable
    );

    // SCR-29: Manager list scoped to assigned properties
    @Query("""
        SELECT r FROM Room r
        WHERE r.deletedAt IS NULL
        AND (:search IS NULL OR
               LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(r.roomType)   LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:status IS NULL OR r.status = :status)
        AND r.property.id IN :propertyIds
        AND (:floorId IS NULL OR r.floor.id = :floorId)
        AND (:roomType IS NULL OR LOWER(r.roomType) = LOWER(:roomType))
        """)
    Page<Room> findWithFiltersInProperties(
            @Param("search")      String search,
            @Param("status")      Room.Status status,
            @Param("propertyIds") List<UUID> propertyIds,
            @Param("floorId")     UUID floorId,
            @Param("roomType")    String roomType,
            Pageable pageable
    );

    // Lấy khoảng giá thực tế từ DB — dùng cho slider bộ lọc (chỉ property ACTIVE)
    @Query("""
        SELECT MIN(r.pricePerNight) FROM Room r
        WHERE r.status = :status AND r.deletedAt IS NULL AND r.property.status = 'ACTIVE'
        """)
    java.math.BigDecimal findMinPrice(@Param("status") Room.Status status);

    @Query("""
        SELECT MAX(r.pricePerNight) FROM Room r
        WHERE r.status = :status AND r.deletedAt IS NULL AND r.property.status = 'ACTIVE'
        """)
    java.math.BigDecimal findMaxPrice(@Param("status") Room.Status status);

    /**
     * FR-08 delete guard — blocking booking statuses (canonical enum).
     * Aligns with {@link #hasBlockingBookings}; do not use legacy COMPLETED.
     */
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.room.id = :roomId
        AND b.status NOT IN ('CANCELLED', 'CHECKED_OUT', 'NO_SHOW')
    """)
    boolean hasActiveBookings(@Param("roomId") UUID roomId);
}

