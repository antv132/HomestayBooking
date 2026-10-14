package com.homestay.repository;

import com.homestay.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query("SELECT p FROM Payment p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:search IS NULL OR LOWER(p.customer.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(CAST(p.booking.id AS string)) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Payment> findAllWithFilters(@Param("status") Payment.Status status,
                                     @Param("search") String search,
                                     Pageable pageable);

    /** SCR-36 — Manager list scoped by assigned properties. */
    @Query("""
        SELECT p FROM Payment p
        JOIN p.booking b
        JOIN b.room r
        WHERE r.property.id IN :propertyIds
          AND (:status IS NULL OR p.status = :status)
          AND (:type IS NULL OR p.type = :type)
          AND (:method IS NULL OR p.method = :method)
          AND (:search IS NULL OR
               LOWER(p.customer.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(CAST(p.id AS string)) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(CAST(b.id AS string)) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Payment> findForManagerWithFilters(
            @Param("propertyIds") List<UUID> propertyIds,
            @Param("status") Payment.Status status,
            @Param("type") Payment.Type type,
            @Param("method") Payment.Method method,
            @Param("search") String search,
            Pageable pageable);

    Optional<Payment> findByOrderRef(String orderRef);

    @Query("SELECT p FROM Payment p WHERE p.booking.id = :bookingId ORDER BY p.createdAt DESC")
    List<Payment> findByBookingIdOrderByCreatedAtDesc(@Param("bookingId") UUID bookingId);

    long countByCustomerIdAndStatus(UUID customerId, Payment.Status status);

    /** SCR-51 — Batch sum PAID payments per customer (Admin Directory). */
    @Query("""
            SELECT p.customer.id, COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.customer.id IN :customerIds
              AND p.status = 'PAID'
            GROUP BY p.customer.id
            """)
    List<Object[]> sumPaidAmountByCustomerIds(@Param("customerIds") List<UUID> customerIds);

    /** SCR-51 — Total spend for one customer. */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.customer.id = :customerId
              AND p.status = 'PAID'
            """)
    java.math.BigDecimal sumPaidAmountByCustomerId(@Param("customerId") UUID customerId);

    Page<Payment> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    Page<Payment> findByCustomerIdAndStatusOrderByCreatedAtDesc(
            UUID customerId, Payment.Status status, Pageable pageable);

    // ── SCR-59: Revenue Report queries ─────────────────────────────────────────

    /**
     * Tổng doanh thu (PAID) theo loại trong khoảng thời gian, tuỳ chọn filter property.
     * Trả về BigDecimal (có thể null nếu không có payment nào).
     */
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        JOIN p.booking b
        JOIN b.room r
        WHERE p.status = 'PAID'
          AND (:from IS NULL OR p.paidAt >= :from)
          AND (:to   IS NULL OR p.paidAt <= :to)
          AND (:propertyId IS NULL OR r.property.id = :propertyId)
          AND (:type IS NULL OR p.type = :type)
        """)
    BigDecimal sumRevenueByType(
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to,
            @Param("propertyId") UUID propertyId,
            @Param("type")       Payment.Type type
    );

    /**
     * Đếm số booking phân biệt có ít nhất 1 payment PAID trong kỳ.
     */
    @Query("""
        SELECT COUNT(DISTINCT p.booking.id)
        FROM Payment p
        JOIN p.booking b
        JOIN b.room r
        WHERE p.status = 'PAID'
          AND (:from IS NULL OR p.paidAt >= :from)
          AND (:to   IS NULL OR p.paidAt <= :to)
          AND (:propertyId IS NULL OR r.property.id = :propertyId)
        """)
    long countDistinctBookings(
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to,
            @Param("propertyId") UUID propertyId
    );

    /**
     * Danh sách payment PAID trong kỳ để service tự group by month/week.
     * Trả về: [paidAt, amount, type, propertyId, propertyName, bookingId]
     */
    @Query("""
        SELECT p.paidAt, p.amount, p.type,
               r.property.id, r.property.name,
               b.id
        FROM Payment p
        JOIN p.booking b
        JOIN b.room r
        WHERE p.status = 'PAID'
          AND (:from IS NULL OR p.paidAt >= :from)
          AND (:to   IS NULL OR p.paidAt <= :to)
          AND (:propertyId IS NULL OR r.property.id = :propertyId)
        ORDER BY p.paidAt ASC
        """)
    List<Object[]> findRevenueRawData(
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to,
            @Param("propertyId") UUID propertyId
    );

    /** FR-16 Manager “all assigned” — SUM PAID trong tập property được gán. */
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        JOIN p.booking b
        JOIN b.room r
        WHERE p.status = 'PAID'
          AND r.property.id IN :propertyIds
          AND (:from IS NULL OR p.paidAt >= :from)
          AND (:to   IS NULL OR p.paidAt <= :to)
          AND (:type IS NULL OR p.type = :type)
        """)
    BigDecimal sumRevenueByTypeForProperties(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("propertyIds") List<UUID> propertyIds,
            @Param("type") Payment.Type type
    );

    @Query("""
        SELECT COUNT(DISTINCT p.booking.id)
        FROM Payment p
        JOIN p.booking b
        JOIN b.room r
        WHERE p.status = 'PAID'
          AND r.property.id IN :propertyIds
          AND (:from IS NULL OR p.paidAt >= :from)
          AND (:to   IS NULL OR p.paidAt <= :to)
        """)
    long countDistinctBookingsForProperties(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("propertyIds") List<UUID> propertyIds
    );

    @Query("""
        SELECT p.paidAt, p.amount, p.type,
               r.property.id, r.property.name,
               b.id
        FROM Payment p
        JOIN p.booking b
        JOIN b.room r
        WHERE p.status = 'PAID'
          AND r.property.id IN :propertyIds
          AND (:from IS NULL OR p.paidAt >= :from)
          AND (:to   IS NULL OR p.paidAt <= :to)
        ORDER BY p.paidAt ASC
        """)
    List<Object[]> findRevenueRawDataForProperties(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("propertyIds") List<UUID> propertyIds
    );

    @Query("""
        SELECT COUNT(p) FROM Payment p
        JOIN p.booking b
        JOIN b.room r
        WHERE r.property.id = :propertyId
          AND p.status = 'PENDING'
        """)
    long countPendingByPropertyId(@Param("propertyId") UUID propertyId);

    // ── SCR-52: Payment Reconciliation (VNPAY discrepancies) ────────────────────
    // Payment VNPAY bi lech: (a) gateway '00' nhung chua PAID; (b) PAID nhung gateway khong xac nhan;
    // (c) PENDING va chua nhan IPN (timeout). enum truyen qua param; JOIN FETCH booking tranh N+1.
    @Query(value = """
        SELECT p FROM Payment p
        JOIN FETCH p.booking
        WHERE p.method = :vnpay
          AND (
            (p.gatewayResponseCode = '00' AND p.status <> :paid)
            OR (p.status = :paid AND (p.gatewayResponseCode IS NULL OR p.gatewayResponseCode <> '00'))
            OR (p.status = :pending AND p.ipnReceivedAt IS NULL)
          )
        """,
        countQuery = """
        SELECT COUNT(p) FROM Payment p
        WHERE p.method = :vnpay
          AND (
            (p.gatewayResponseCode = '00' AND p.status <> :paid)
            OR (p.status = :paid AND (p.gatewayResponseCode IS NULL OR p.gatewayResponseCode <> '00'))
            OR (p.status = :pending AND p.ipnReceivedAt IS NULL)
          )
        """)
    Page<Payment> findVnpayDiscrepancies(
            @Param("vnpay") Payment.Method vnpay,
            @Param("paid") Payment.Status paid,
            @Param("pending") Payment.Status pending,
            Pageable pageable);
}
