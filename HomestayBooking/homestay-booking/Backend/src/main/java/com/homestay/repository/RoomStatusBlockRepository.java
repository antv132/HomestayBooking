package com.homestay.repository;

import com.homestay.entity.RoomStatusBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RoomStatusBlockRepository extends JpaRepository<RoomStatusBlock, UUID> {

    @Query("""
            SELECT b FROM RoomStatusBlock b
            WHERE b.room.id = :roomId
              AND b.active = true
              AND b.startDate <= :endDate
              AND b.endDate >= :startDate
            ORDER BY b.startDate
            """)
    List<RoomStatusBlock> findActiveOverlappingRange(
            @Param("roomId") UUID roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * True if an active block overlaps half-open stay nights {@code [checkIn, checkOut)}.
     * Block dates are inclusive {@code [startDate, endDate]}.
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM RoomStatusBlock b
            WHERE b.room.id = :roomId
              AND b.active = true
              AND b.startDate < :checkOut
              AND b.endDate >= :checkIn
            """)
    boolean existsActiveOverlappingStay(
            @Param("roomId") UUID roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut);

    List<RoomStatusBlock> findByRoom_IdAndActiveTrue(UUID roomId);
}
