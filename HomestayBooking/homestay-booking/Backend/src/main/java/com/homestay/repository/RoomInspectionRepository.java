package com.homestay.repository;

import com.homestay.entity.RoomInspection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomInspectionRepository extends JpaRepository<RoomInspection, UUID> {

    Optional<RoomInspection> findByBookingId(UUID bookingId);

    // SCR-42: Danh sach kiem tra phong cho Manager, scope theo property, filter status + search room number.
    // JOIN FETCH cac quan he single-valued de tranh N+1; enum truyen qua parameter.
    @Query(value = """
            SELECT ri FROM RoomInspection ri
            JOIN FETCH ri.room r
            JOIN FETCH ri.property p
            JOIN FETCH ri.booking b
            LEFT JOIN FETCH ri.inspectedBy ib
            WHERE p.id = :propertyId
              AND (:status IS NULL OR ri.status = :status)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY ri.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(ri) FROM RoomInspection ri
            WHERE ri.property.id = :propertyId
              AND (:status IS NULL OR ri.status = :status)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(ri.room.roomNumber) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<RoomInspection> findForManagerBoard(
            @Param("propertyId") UUID propertyId,
            @Param("status") RoomInspection.Status status,
            @Param("search") String search,
            Pageable pageable);

    // SCR-59: pending inspections in employee-assigned properties (workspace pool)
    @Query("""
            SELECT COUNT(ri) FROM RoomInspection ri
            WHERE ri.status IN :statuses
              AND ri.property.id IN :propertyIds
            """)
    long countPendingForProperties(
            @Param("statuses") Collection<RoomInspection.Status> statuses,
            @Param("propertyIds") Collection<UUID> propertyIds);

    /** FR-22 — KPI/preview: unassigned OR assigned to employee (matches SCR-62 workspace). */
    @Query("""
            SELECT COUNT(ri) FROM RoomInspection ri
            WHERE ri.property.id IN :propertyIds
              AND ri.status IN :statuses
              AND (ri.inspectedBy IS NULL OR ri.inspectedBy.id = :employeeId)
            """)
    long countForEmployeeDashboard(
            @Param("propertyIds") Collection<UUID> propertyIds,
            @Param("statuses") Collection<RoomInspection.Status> statuses,
            @Param("employeeId") UUID employeeId);

    @Query("""
            SELECT COUNT(ri) FROM RoomInspection ri
            WHERE ri.inspectedBy.id = :employeeId
              AND ri.property.id IN :propertyIds
              AND ri.status = 'PASSED'
              AND ri.inspectedAt >= :dayStart
              AND ri.inspectedAt < :dayEnd
            """)
    long countCompletedToday(
            @Param("employeeId") UUID employeeId,
            @Param("propertyIds") Collection<UUID> propertyIds,
            @Param("dayStart") java.time.LocalDateTime dayStart,
            @Param("dayEnd") java.time.LocalDateTime dayEnd);

    @Query("""
            SELECT ri FROM RoomInspection ri
            JOIN FETCH ri.room r
            JOIN FETCH ri.property p
            JOIN FETCH ri.booking b
            WHERE ri.inspectedBy.id = :employeeId
              AND ri.property.id IN :propertyIds
              AND ri.status = 'PASSED'
              AND ri.inspectedAt >= :dayStart
              AND ri.inspectedAt < :dayEnd
            ORDER BY ri.inspectedAt DESC
            """)
    List<RoomInspection> findCompletedToday(
            @Param("employeeId") UUID employeeId,
            @Param("propertyIds") Collection<UUID> propertyIds,
            @Param("dayStart") java.time.LocalDateTime dayStart,
            @Param("dayEnd") java.time.LocalDateTime dayEnd,
            Pageable pageable);

    @Query("""
            SELECT ri FROM RoomInspection ri
            JOIN FETCH ri.room r
            JOIN FETCH ri.property p
            JOIN FETCH ri.booking b
            WHERE ri.property.id IN :propertyIds
              AND (ri.inspectedBy IS NULL OR ri.inspectedBy.id = :employeeId)
              AND (
                (ri.status IN ('PENDING', 'IN_PROGRESS') AND (
                    (ri.createdAt >= :dayStart AND ri.createdAt < :dayEnd)
                    OR b.checkOutDate = :today
                ))
                OR (ri.status = 'PASSED' AND ri.inspectedBy.id = :employeeId
                    AND ri.inspectedAt >= :dayStart AND ri.inspectedAt < :dayEnd)
              )
            ORDER BY ri.createdAt DESC
            """)
    List<RoomInspection> findTodayForDashboard(
            @Param("propertyIds") Collection<UUID> propertyIds,
            @Param("employeeId") UUID employeeId,
            @Param("today") java.time.LocalDate today,
            @Param("dayStart") java.time.LocalDateTime dayStart,
            @Param("dayEnd") java.time.LocalDateTime dayEnd,
            Pageable pageable);

    // SCR-62: employee inspection workspace — unassigned OR assigned to current employee
    @Query(
            value = """
            SELECT ri FROM RoomInspection ri
            JOIN FETCH ri.room r
            JOIN FETCH ri.booking b
            LEFT JOIN FETCH ri.inspectedBy ib
            WHERE ri.property.id IN :propertyIds
              AND ri.status IN :statuses
              AND (ri.inspectedBy IS NULL OR ri.inspectedBy.id = :employeeId)
            ORDER BY ri.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(ri) FROM RoomInspection ri
            WHERE ri.property.id IN :propertyIds
              AND ri.status IN :statuses
              AND (ri.inspectedBy IS NULL OR ri.inspectedBy.id = :employeeId)
            """)
    Page<RoomInspection> findForEmployee(
            @Param("propertyIds") Collection<UUID> propertyIds,
            @Param("statuses") Collection<RoomInspection.Status> statuses,
            @Param("employeeId") UUID employeeId,
            Pageable pageable);

    @Query("""
            SELECT ri FROM RoomInspection ri
            JOIN FETCH ri.room r
            JOIN FETCH ri.booking b
            LEFT JOIN FETCH ri.inspectedBy ib
            WHERE ri.id = :id
              AND ri.property.id IN :propertyIds
            """)
    Optional<RoomInspection> findByIdAndPropertyIdIn(
            @Param("id") UUID id,
            @Param("propertyIds") Collection<UUID> propertyIds);

    // SCR-64: resolve inspection FAILED for a room + employee (most recent first).
    @Query("""
            SELECT ri FROM RoomInspection ri
            JOIN FETCH ri.booking b
            JOIN FETCH ri.property p
            JOIN FETCH ri.room r
            WHERE ri.room.id = :roomId
              AND ri.inspectedBy.id = :employeeId
              AND ri.status = :status
            ORDER BY ri.inspectedAt DESC
            """)
    List<RoomInspection> findFailedForEmployeeAndRoom(
            @Param("roomId") UUID roomId,
            @Param("employeeId") UUID employeeId,
            @Param("status") RoomInspection.Status status,
            Pageable pageable);

    /** SCR-64 — FAILED inspections by employee that do not yet have a damage report. */
    @Query("""
            SELECT ri FROM RoomInspection ri
            JOIN FETCH ri.room r
            JOIN FETCH ri.booking b
            WHERE ri.inspectedBy.id = :employeeId
              AND ri.status = :status
              AND NOT EXISTS (
                  SELECT 1 FROM DamageReport dr WHERE dr.inspection.id = ri.id
              )
            ORDER BY ri.inspectedAt DESC
            """)
    List<RoomInspection> findEligibleForDamageReport(
            @Param("employeeId") UUID employeeId,
            @Param("status") RoomInspection.Status status);

    /** SCR-64 — Load specific FAILED inspection owned by employee (for create by inspectionId). */
    @Query("""
            SELECT ri FROM RoomInspection ri
            JOIN FETCH ri.booking b
            JOIN FETCH ri.property p
            JOIN FETCH ri.room r
            WHERE ri.id = :id
              AND ri.inspectedBy.id = :employeeId
              AND ri.status = :status
            """)
    Optional<RoomInspection> findFailedByIdForEmployee(
            @Param("id") UUID id,
            @Param("employeeId") UUID employeeId,
            @Param("status") RoomInspection.Status status);
}