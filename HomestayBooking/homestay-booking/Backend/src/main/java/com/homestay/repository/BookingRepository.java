package com.homestay.repository;

import com.homestay.entity.Booking;
import com.homestay.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByCustomerOrderByCreatedAtDesc(User customer);

    /**
     * SCR-23 — Active bookings for create-maintenance dropdown (JOIN FETCH room+property).
     * Avoid DISTINCT: SQL Server rejects DISTINCT when entity has TEXT columns
     * (special_requests, cancel_reason). ManyToOne FETCH joins do not duplicate rows.
     */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.room r
            JOIN FETCH r.property
            WHERE b.customer.id = :customerId
              AND b.status IN :statuses
            ORDER BY b.createdAt DESC
            """)
    List<Booking> findActiveWithRoomPropertyByCustomerId(
            @Param("customerId") UUID customerId,
            @Param("statuses") List<Booking.Status> statuses);

    /** SCR-23 — Load booking with room+property for create maintenance. */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.room r
            JOIN FETCH r.property
            JOIN FETCH b.customer
            WHERE b.id = :id
            """)
    java.util.Optional<Booking> findByIdWithRoomAndCustomer(@Param("id") UUID id);

    Page<Booking> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Booking> findByCustomerIdAndStatus(UUID customerId, Booking.Status status, Pageable pageable);

    Page<Booking> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    // SCR-40: Booking history for a specific room (Manager view)
    Page<Booking> findByRoomIdOrderByCheckInDateDesc(UUID roomId, Pageable pageable);

    long countByCustomerId(UUID customerId);

    /** SCR-51 — Batch count bookings per customer (Admin Directory). */
    @Query("""
            SELECT b.customer.id, COUNT(b)
            FROM Booking b
            WHERE b.customer.id IN :customerIds
            GROUP BY b.customer.id
            """)
    List<Object[]> countBookingsByCustomerIds(@Param("customerIds") List<UUID> customerIds);

    /** SCR-51 — Recent bookings for Customer Directory drawer. */
    @EntityGraph(attributePaths = {"room", "room.property"})
    @Query("""
            SELECT b FROM Booking b
            WHERE b.customer.id = :customerId
            ORDER BY b.createdAt DESC
            """)
    Page<Booking> findByCustomerIdWithDetails(
            @Param("customerId") UUID customerId,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT b FROM Booking b WHERE " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:search IS NULL OR LOWER(b.customer.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(b.room.roomNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Booking> findAllWithFilters(@Param("status") Booking.Status status, @Param("search") String search, Pageable pageable);

    /** SCR-34 — Manager list scoped by assigned properties. */
    @Query("""
        SELECT b FROM Booking b
        JOIN b.room r
        JOIN b.customer c
        WHERE r.property.id IN :propertyIds
          AND (:status IS NULL OR b.status = :status)
          AND (:search IS NULL OR
               LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(c.phone, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(CAST(b.id AS string)) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:checkInFrom IS NULL OR b.checkInDate >= :checkInFrom)
          AND (:checkInTo IS NULL OR b.checkInDate <= :checkInTo)
        """)
    Page<Booking> findForManagerWithFilters(
            @Param("propertyIds") List<UUID> propertyIds,
            @Param("status") Booking.Status status,
            @Param("search") String search,
            @Param("checkInFrom") LocalDate checkInFrom,
            @Param("checkInTo") LocalDate checkInTo,
            Pageable pageable);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.customer.id = :customerId
        AND b.status IN ('CONFIRMED', 'CHECKED_IN')
        """)
    long countActiveByCustomerId(@Param("customerId") UUID customerId);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.customer.id = :customerId
        AND b.status = 'PENDING_DEPOSIT'
        """)
    long countPendingDepositByCustomerId(@Param("customerId") UUID customerId);

    java.util.Optional<Booking> findFirstByCustomerIdAndStatusAndCheckInDateGreaterThanEqualOrderByCheckInDateAsc(
            UUID customerId, Booking.Status status, LocalDate checkInDate);

    java.util.Optional<Booking> findFirstByCustomerIdAndStatusAndCheckOutDateGreaterThanEqualOrderByCheckOutDateAsc(
            UUID customerId, Booking.Status status, LocalDate checkOutDate);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.customer.id = :customerId
        AND b.status IN ('PENDING_DEPOSIT', 'CONFIRMED', 'CHECKED_IN')
        AND b.checkOutDate >= :today
        AND (b.status = 'CHECKED_IN' OR b.checkInDate >= :today)
        ORDER BY b.checkInDate ASC
        """)
    List<Booking> findUpcomingByCustomerId(
            @Param("customerId") UUID customerId,
            @Param("today") LocalDate today,
            Pageable pageable);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        JOIN b.room r
        WHERE r.property.id = :propertyId
          AND b.checkInDate = :today
          AND b.status = :status
        """)
    long countPendingCheckInsByProperty(
            @Param("propertyId") UUID propertyId,
            @Param("today") LocalDate today,
            @Param("status") Booking.Status status);

    @Query("""
        SELECT b.status, COUNT(b) FROM Booking b
        JOIN b.room r
        WHERE r.property.id = :propertyId
          AND b.createdAt >= :from
          AND b.createdAt <= :to
        GROUP BY b.status
        """)
    List<Object[]> countByStatusForPropertyInRange(
            @Param("propertyId") UUID propertyId,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to);

    /** SCR-44 — Occupancy: các booking chiếm phòng overlapping khoảng ngày (đêm-phòng). */
    @Query("""
        SELECT b.checkInDate, b.checkOutDate FROM Booking b
        JOIN b.room r
        WHERE r.property.id = :propertyId
          AND b.status IN :statuses
          AND b.checkInDate < :rangeEndExclusive
          AND b.checkOutDate > :rangeStart
        """)
    List<Object[]> findOccupancyRawData(
            @Param("propertyId") UUID propertyId,
            @Param("statuses") List<Booking.Status> statuses,
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEndExclusive") LocalDate rangeEndExclusive);

    /**
     * SCR-44 / FR-010 — Booking Trend: thời điểm tạo booking counted
     * (CONFIRMED, CHECKED_IN, CHECKED_OUT) trong khoảng.
     */
    @Query("""
        SELECT b.createdAt FROM Booking b
        JOIN b.room r
        WHERE r.property.id = :propertyId
          AND b.status IN :statuses
          AND b.createdAt >= :from
          AND b.createdAt <= :to
        """)
    List<java.time.LocalDateTime> findBookingCreationTimes(
            @Param("propertyId") UUID propertyId,
            @Param("statuses") List<Booking.Status> statuses,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to);

    /** Hold timeout job — unpaid PENDING_DEPOSIT past holdExpiresAt. */
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.room
        JOIN FETCH b.customer
        WHERE b.status = 'PENDING_DEPOSIT'
          AND b.holdExpiresAt IS NOT NULL
          AND b.holdExpiresAt < :now
        """)
    List<Booking> findExpiredPendingDeposits(@Param("now") java.time.LocalDateTime now);

    /**
     * No-show job candidates — CONFIRMED with check-in on or before maxCheckInDate.
     * Service applies exact 24h (configurable) threshold from checkInDate.atStartOfDay().
     */
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.room
        JOIN FETCH b.customer
        WHERE b.status = 'CONFIRMED'
          AND b.checkInDate <= :maxCheckInDate
        """)
    List<Booking> findConfirmedEligibleForNoShow(@Param("maxCheckInDate") LocalDate maxCheckInDate);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status AND b.checkInDate >= :date")
    long countByStatusAndCheckInDateGreaterThanEqual(@Param("status") Booking.Status status, @Param("date") LocalDate date);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status AND b.checkOutDate >= :date")
    long countByStatusAndCheckOutDateGreaterThanEqual(@Param("status") Booking.Status status, @Param("date") LocalDate date);
}
