package com.homestay.repository;

import com.homestay.entity.MaintenanceTicket;
import com.homestay.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, UUID> {
    List<MaintenanceTicket> findByCustomerOrderByCreatedAtDesc(User customer);
    List<MaintenanceTicket> findAllByOrderByCreatedAtDesc();

    /** SCR-22 — Customer ticket list with DB pagination and optional status filter. */
    @Query(value = """
            SELECT m FROM MaintenanceTicket m
            WHERE m.customer = :customer
              AND m.deletedAt IS NULL
              AND (:status IS NULL OR m.status = :status)
            ORDER BY m.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(m) FROM MaintenanceTicket m
            WHERE m.customer = :customer
              AND m.deletedAt IS NULL
              AND (:status IS NULL OR m.status = :status)
            """)
    Page<MaintenanceTicket> findByCustomerPaged(
            @Param("customer") User customer,
            @Param("status") MaintenanceTicket.Status status,
            Pageable pageable);

    long countByCustomerIdAndStatusIn(UUID customerId, java.util.Collection<MaintenanceTicket.Status> statuses);

    @Query("""
        SELECT COUNT(m) FROM MaintenanceTicket m
        JOIN m.room r
        WHERE r.property.id = :propertyId
          AND m.status = 'OPEN'
          AND m.deletedAt IS NULL
        """)
    long countOpenByPropertyId(@Param("propertyId") UUID propertyId);

    // SCR-41: Danh sách bảo trì Manager — EntityGraph thay JOIN FETCH để pagination đúng trên DB.
    @EntityGraph(attributePaths = {"room", "room.property", "customer", "assignedEmployee"})
    @Query(value = """
            SELECT m FROM MaintenanceTicket m
            WHERE m.room.property.id = :propertyId
              AND m.deletedAt IS NULL
              AND (:status IS NULL OR m.status = :status)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(m.title) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY m.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(m) FROM MaintenanceTicket m
            WHERE m.room.property.id = :propertyId
              AND m.deletedAt IS NULL
              AND (:status IS NULL OR m.status = :status)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(m.title) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<MaintenanceTicket> findForManagerBoard(
            @Param("propertyId") UUID propertyId,
            @Param("status") MaintenanceTicket.Status status,
            @Param("search") String search,
            Pageable pageable);

    /** SCR-41 — Load ticket + room/property/customer/assignee for assign & verify-close. */
    @EntityGraph(attributePaths = {"room", "room.property", "customer", "assignedEmployee"})
    @Query("SELECT m FROM MaintenanceTicket m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<MaintenanceTicket> findByIdWithDetails(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"room", "room.property", "customer", "assignedEmployee"})
    @Query("SELECT m FROM MaintenanceTicket m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<MaintenanceTicket> findActiveById(@Param("id") UUID id);

    // Admin read-only global list
    @EntityGraph(attributePaths = {"room", "room.property", "customer", "assignedEmployee"})
    @Query(value = """
            SELECT m FROM MaintenanceTicket m
            WHERE m.deletedAt IS NULL
              AND (:status IS NULL OR m.status = :status)
            ORDER BY m.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(m) FROM MaintenanceTicket m
            WHERE m.deletedAt IS NULL
              AND (:status IS NULL OR m.status = :status)
            """)
    Page<MaintenanceTicket> findAllActive(
            @Param("status") MaintenanceTicket.Status status,
            Pageable pageable);

    // SCR-59: pending maintenance for employee dashboard KPI
    long countByAssignedEmployeeIdAndStatusIn(
            UUID employeeId,
            java.util.Collection<MaintenanceTicket.Status> statuses);

    @Query("""
            SELECT COUNT(m) FROM MaintenanceTicket m
            WHERE m.assignedEmployee.id = :employeeId
              AND m.deletedAt IS NULL
              AND m.room.property.id IN :propertyIds
              AND m.status IN :statuses
            """)
    long countForDashboard(
            @Param("employeeId") UUID employeeId,
            @Param("propertyIds") Collection<UUID> propertyIds,
            @Param("statuses") Collection<MaintenanceTicket.Status> statuses);

    @EntityGraph(attributePaths = {"room", "room.property", "booking"})
    @Query("""
            SELECT m FROM MaintenanceTicket m
            WHERE m.assignedEmployee.id = :employeeId
              AND m.deletedAt IS NULL
              AND m.room.property.id IN :propertyIds
              AND m.status IN :statuses
            ORDER BY m.createdAt DESC
            """)
    List<MaintenanceTicket> findPreviewForDashboard(
            @Param("employeeId") UUID employeeId,
            @Param("propertyIds") Collection<UUID> propertyIds,
            @Param("statuses") Collection<MaintenanceTicket.Status> statuses,
            Pageable pageable);

    @Query("""
            SELECT COUNT(m) FROM MaintenanceTicket m
            WHERE m.assignedEmployee.id = :employeeId
              AND m.deletedAt IS NULL
              AND m.room.property.id IN :propertyIds
              AND m.status = 'RESOLVED'
              AND m.updatedAt >= :dayStart
              AND m.updatedAt < :dayEnd
            """)
    long countCompletedToday(
            @Param("employeeId") UUID employeeId,
            @Param("propertyIds") Collection<UUID> propertyIds,
            @Param("dayStart") java.time.LocalDateTime dayStart,
            @Param("dayEnd") java.time.LocalDateTime dayEnd);

    @EntityGraph(attributePaths = {"room", "room.property", "booking"})
    @Query("""
            SELECT m FROM MaintenanceTicket m
            WHERE m.assignedEmployee.id = :employeeId
              AND m.deletedAt IS NULL
              AND m.room.property.id IN :propertyIds
              AND m.status = 'RESOLVED'
              AND m.updatedAt >= :dayStart
              AND m.updatedAt < :dayEnd
            ORDER BY m.updatedAt DESC
            """)
    List<MaintenanceTicket> findCompletedToday(
            @Param("employeeId") UUID employeeId,
            @Param("propertyIds") Collection<UUID> propertyIds,
            @Param("dayStart") java.time.LocalDateTime dayStart,
            @Param("dayEnd") java.time.LocalDateTime dayEnd,
            Pageable pageable);

    @EntityGraph(attributePaths = {"room", "room.property", "booking"})
    @Query("""
            SELECT m FROM MaintenanceTicket m
            WHERE m.assignedEmployee.id = :employeeId
              AND m.deletedAt IS NULL
              AND m.room.property.id IN :propertyIds
              AND (
                (m.status IN ('ASSIGNED', 'IN_PROGRESS') AND m.createdAt >= :dayStart AND m.createdAt < :dayEnd)
                OR (m.status = 'RESOLVED' AND m.updatedAt >= :dayStart AND m.updatedAt < :dayEnd)
              )
            ORDER BY m.createdAt DESC
            """)
    List<MaintenanceTicket> findTodayForDashboard(
            @Param("employeeId") UUID employeeId,
            @Param("propertyIds") Collection<UUID> propertyIds,
            @Param("dayStart") java.time.LocalDateTime dayStart,
            @Param("dayEnd") java.time.LocalDateTime dayEnd,
            Pageable pageable);

    // SCR-61: employee maintenance workspace — EntityGraph thay JOIN FETCH để pagination đúng.
    @EntityGraph(attributePaths = {"room", "room.floor"})
    @Query(
            value = """
            SELECT m FROM MaintenanceTicket m
            WHERE m.assignedEmployee.id = :employeeId
              AND m.deletedAt IS NULL
              AND (:status IS NULL OR m.status = :status)
            ORDER BY m.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(m) FROM MaintenanceTicket m
            WHERE m.assignedEmployee.id = :employeeId
              AND m.deletedAt IS NULL
              AND (:status IS NULL OR m.status = :status)
            """)
    Page<MaintenanceTicket> findForEmployee(
            @Param("employeeId") UUID employeeId,
            @Param("status") MaintenanceTicket.Status status,
            Pageable pageable);

    /** SCR-61 — Load assigned ticket with room for status update. */
    @EntityGraph(attributePaths = {"room", "room.floor", "customer"})
    @Query("""
            SELECT m FROM MaintenanceTicket m
            WHERE m.id = :id
              AND m.assignedEmployee.id = :employeeId
              AND m.deletedAt IS NULL
            """)
    Optional<MaintenanceTicket> findByIdAndAssignedEmployeeId(
            @Param("id") UUID id,
            @Param("employeeId") UUID employeeId);
}
