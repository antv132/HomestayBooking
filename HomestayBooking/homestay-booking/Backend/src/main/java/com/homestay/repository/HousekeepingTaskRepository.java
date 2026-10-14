package com.homestay.repository;
import com.homestay.entity.HousekeepingTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface HousekeepingTaskRepository extends JpaRepository<HousekeepingTask, UUID> {
    @Query("""
            SELECT t FROM HousekeepingTask t
            JOIN FETCH t.room
            LEFT JOIN FETCH t.assignedEmployee
            WHERE t.property.id = :propertyId
              AND t.status IN :statuses
              AND t.createdAt >= :start
              AND t.createdAt < :end
            ORDER BY t.createdAt ASC
            """)
    List<HousekeepingTask> findScheduleTasks(
            @Param("propertyId") UUID propertyId,
            @Param("statuses") List<HousekeepingTask.Status> statuses,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
    @Query(
            value = """
            SELECT t FROM HousekeepingTask t
            WHERE t.property.id = :propertyId
              AND (:status IS NULL OR t.status = :status)
              AND t.createdAt >= :fromDate
              AND t.createdAt < :toDate
            ORDER BY t.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(t) FROM HousekeepingTask t
            WHERE t.property.id = :propertyId
              AND (:status IS NULL OR t.status = :status)
              AND t.createdAt >= :fromDate
              AND t.createdAt < :toDate
            """)
    Page<HousekeepingTask> findForManagerBoard(
            @Param("propertyId") UUID propertyId,
            @Param("status") HousekeepingTask.Status status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
    boolean existsByRoom_IdAndStatusIn(UUID roomId, Collection<HousekeepingTask.Status> statuses);
    boolean existsByBooking_IdAndStatusIn(UUID bookingId, Collection<HousekeepingTask.Status> statuses);

    /** FR-21 idempotency — one housekeeping task per checkout booking. */
    boolean existsByBooking_Id(UUID bookingId);

    @Query(
            value = """
            SELECT t FROM HousekeepingTask t
            WHERE (:propertyId IS NULL OR t.property.id = :propertyId)
              AND (:status IS NULL OR t.status = :status)
            ORDER BY t.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(t) FROM HousekeepingTask t
            WHERE (:propertyId IS NULL OR t.property.id = :propertyId)
              AND (:status IS NULL OR t.status = :status)
            """)
    Page<HousekeepingTask> findForAdmin(
            @Param("propertyId") UUID propertyId,
            @Param("status") HousekeepingTask.Status status,
            Pageable pageable);
    @Query("""
            SELECT t FROM HousekeepingTask t
            WHERE t.room.id = :roomId
              AND t.status IN :statuses
            """)
    Optional<HousekeepingTask> findOpenByRoomId(
            @Param("roomId") UUID roomId,
            @Param("statuses") Collection<HousekeepingTask.Status> statuses);
    // SCR-59: pending HK tasks for employee dashboard KPI
    long countByAssignedEmployeeIdAndStatusIn(
            UUID employeeId,
            Collection<HousekeepingTask.Status> statuses);

    @Query("""
            SELECT COUNT(t) FROM HousekeepingTask t
            WHERE t.assignedEmployee.id = :employeeId
              AND t.property.id IN :propertyIds
              AND t.status IN :statuses
            """)
    long countForDashboard(
            @Param("employeeId") UUID employeeId,
            @Param("propertyIds") Collection<UUID> propertyIds,
            @Param("statuses") Collection<HousekeepingTask.Status> statuses);

    @EntityGraph(attributePaths = {"room", "property", "booking"})
    @Query("""
            SELECT t FROM HousekeepingTask t
            WHERE t.assignedEmployee.id = :employeeId
              AND t.property.id IN :propertyIds
              AND t.status IN :statuses
            ORDER BY t.createdAt DESC
            """)
    List<HousekeepingTask> findPreviewForDashboard(
            @Param("employeeId") UUID employeeId,
            @Param("propertyIds") Collection<UUID> propertyIds,
            @Param("statuses") Collection<HousekeepingTask.Status> statuses,
            Pageable pageable);

    @Query("""
            SELECT COUNT(t) FROM HousekeepingTask t
            WHERE t.assignedEmployee.id = :employeeId
              AND t.property.id IN :propertyIds
              AND t.status = 'COMPLETED'
              AND t.completedAt >= :dayStart
              AND t.completedAt < :dayEnd
            """)
    long countCompletedToday(
            @Param("employeeId") UUID employeeId,
            @Param("propertyIds") Collection<UUID> propertyIds,
            @Param("dayStart") java.time.LocalDateTime dayStart,
            @Param("dayEnd") java.time.LocalDateTime dayEnd);

    @EntityGraph(attributePaths = {"room", "property", "booking"})
    @Query("""
            SELECT t FROM HousekeepingTask t
            WHERE t.assignedEmployee.id = :employeeId
              AND t.property.id IN :propertyIds
              AND t.status = 'COMPLETED'
              AND t.completedAt >= :dayStart
              AND t.completedAt < :dayEnd
            ORDER BY t.completedAt DESC
            """)
    List<HousekeepingTask> findCompletedToday(
            @Param("employeeId") UUID employeeId,
            @Param("propertyIds") Collection<UUID> propertyIds,
            @Param("dayStart") java.time.LocalDateTime dayStart,
            @Param("dayEnd") java.time.LocalDateTime dayEnd,
            Pageable pageable);

    @EntityGraph(attributePaths = {"room", "property", "booking"})
    @Query("""
            SELECT t FROM HousekeepingTask t
            WHERE t.assignedEmployee.id = :employeeId
              AND t.property.id IN :propertyIds
              AND (
                (t.status IN ('PENDING', 'IN_PROGRESS') AND (
                    (t.createdAt >= :dayStart AND t.createdAt < :dayEnd)
                    OR (t.booking IS NOT NULL AND t.booking.checkOutDate = :today)
                ))
                OR (t.status = 'COMPLETED' AND t.completedAt >= :dayStart AND t.completedAt < :dayEnd)
              )
            ORDER BY t.createdAt DESC
            """)
    List<HousekeepingTask> findTodayForDashboard(
            @Param("employeeId") UUID employeeId,
            @Param("propertyIds") Collection<UUID> propertyIds,
            @Param("today") java.time.LocalDate today,
            @Param("dayStart") java.time.LocalDateTime dayStart,
            @Param("dayEnd") java.time.LocalDateTime dayEnd,
            Pageable pageable);

    // SCR-60: employee housekeeping workspace
    @Query(
            value = """
            SELECT t FROM HousekeepingTask t
            JOIN FETCH t.room r
            LEFT JOIN FETCH r.floor
            WHERE t.assignedEmployee.id = :employeeId
              AND (:status IS NULL OR t.status = :status)
            ORDER BY t.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(t) FROM HousekeepingTask t
            WHERE t.assignedEmployee.id = :employeeId
              AND (:status IS NULL OR t.status = :status)
            """)
    Page<HousekeepingTask> findForEmployee(
            @Param("employeeId") UUID employeeId,
            @Param("status") HousekeepingTask.Status status,
            Pageable pageable);
    @Query("""
            SELECT t FROM HousekeepingTask t
            JOIN FETCH t.room r
            LEFT JOIN FETCH r.floor
            LEFT JOIN FETCH t.property
            WHERE t.id = :id
              AND t.assignedEmployee.id = :employeeId
            """)
    Optional<HousekeepingTask> findByIdAndAssignedEmployeeId(
            @Param("id") UUID id,
            @Param("employeeId") UUID employeeId);
}
