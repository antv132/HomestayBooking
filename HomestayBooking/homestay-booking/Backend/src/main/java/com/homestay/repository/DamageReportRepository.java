package com.homestay.repository;

import com.homestay.entity.DamageReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DamageReportRepository extends JpaRepository<DamageReport, UUID> {

    // SCR-43: Danh sách báo cáo hư hại cho Manager, scope theo property (qua inspection.property).
    // JOIN FETCH quan hệ single-valued để tránh N+1; enum + escalated truyền qua parameter.
    @Query(value = """
            SELECT dr FROM DamageReport dr
            JOIN FETCH dr.inspection i
            JOIN FETCH i.property p
            JOIN FETCH i.room r
            LEFT JOIN FETCH i.inspectedBy ins
            JOIN FETCH dr.booking b
            LEFT JOIN FETCH dr.approvedBy ab
            WHERE p.id = :propertyId
              AND (:status IS NULL OR dr.status = :status)
              AND (:escalated IS NULL OR dr.requiresAdminEscalation = :escalated)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(CAST(dr.id AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(CAST(b.id AS string)) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY dr.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(dr) FROM DamageReport dr
            WHERE dr.inspection.property.id = :propertyId
              AND (:status IS NULL OR dr.status = :status)
              AND (:escalated IS NULL OR dr.requiresAdminEscalation = :escalated)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(dr.inspection.room.roomNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(CAST(dr.id AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(CAST(dr.booking.id AS string)) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<DamageReport> findForManagerBoard(
            @Param("propertyId") UUID propertyId,
            @Param("status") DamageReport.Status status,
            @Param("escalated") Boolean escalated,
            @Param("search") String search,
            Pageable pageable);

    // Chi tiết 1 báo cáo (kèm items) cho Drawer — single row nên fetch collection an toàn.
    @Query("""
            SELECT dr FROM DamageReport dr
            LEFT JOIN FETCH dr.items it
            JOIN FETCH dr.inspection i
            JOIN FETCH i.property p
            JOIN FETCH i.room r
            LEFT JOIN FETCH i.inspectedBy ins
            JOIN FETCH dr.booking b
            LEFT JOIN FETCH dr.approvedBy ab
            WHERE dr.id = :id
            """)
    Optional<DamageReport> findDetailById(@Param("id") UUID id);

    // SCR-53: hang doi cho Admin co-approve = escalated (>5M) va dang PENDING_APPROVAL.
    // JOIN FETCH to-one tranh N+1; enum truyen qua param; countQuery rieng khong JOIN FETCH.
    @Query(value = """
            SELECT dr FROM DamageReport dr
            JOIN FETCH dr.inspection i
            JOIN FETCH i.property p
            JOIN FETCH i.room r
            LEFT JOIN FETCH i.inspectedBy ins
            JOIN FETCH dr.booking b
            WHERE dr.requiresAdminEscalation = true
              AND dr.status = :pending
            ORDER BY dr.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(dr) FROM DamageReport dr
            WHERE dr.requiresAdminEscalation = true
              AND dr.status = :pending
            """)
    Page<DamageReport> findEscalatedForAdmin(
            @Param("pending") DamageReport.Status pending,
            Pageable pageable);

    // SCR-63: danh sách damage report của Employee (qua inspection.inspectedBy).
    // EntityGraph to-one — tránh JOIN FETCH + Pageable (pagination sai / in-memory).
    // Items load lazy trong @Transactional (không FETCH collection trên Page).
    @EntityGraph(attributePaths = {
            "inspection", "inspection.room", "inspection.inspectedBy", "booking"
    })
    @Query(
            value = """
            SELECT dr FROM DamageReport dr
            WHERE dr.inspection.inspectedBy.id = :employeeId
              AND (:status IS NULL OR dr.status = :status)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(dr.inspection.room.roomNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(CAST(dr.id AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(CAST(dr.booking.id AS string)) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY dr.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(dr) FROM DamageReport dr
            WHERE dr.inspection.inspectedBy.id = :employeeId
              AND (:status IS NULL OR dr.status = :status)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(dr.inspection.room.roomNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(CAST(dr.id AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(CAST(dr.booking.id AS string)) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<DamageReport> findForEmployee(
            @Param("employeeId") UUID employeeId,
            @Param("status") DamageReport.Status status,
            @Param("search") String search,
            Pageable pageable);

    // SCR-64: check inspection already has a damage report (unique constraint enforcement)
    boolean existsByInspection_Id(UUID inspectionId);

    /** Used after DAMAGE_FEE payment succeeds — mark approved report as PAID. */
    Optional<DamageReport> findFirstByBooking_IdAndStatus(UUID bookingId, DamageReport.Status status);

    /** Latest damage report for booking — checkout gate (FR-23). */
    Optional<DamageReport> findFirstByBooking_IdOrderByCreatedAtDesc(UUID bookingId);

    /** Customer list — reports for bookings owned by customer. */
    @EntityGraph(attributePaths = {
            "inspection", "inspection.room", "inspection.property", "inspection.inspectedBy", "booking", "booking.customer"
    })
    @Query("""
            SELECT dr FROM DamageReport dr
            WHERE dr.booking.customer.id = :customerId
            ORDER BY dr.createdAt DESC
            """)
    java.util.List<DamageReport> findForCustomer(@Param("customerId") UUID customerId);
}
