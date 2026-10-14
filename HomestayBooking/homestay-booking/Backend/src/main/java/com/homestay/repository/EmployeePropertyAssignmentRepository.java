package com.homestay.repository;

import com.homestay.entity.EmployeePropertyAssignment;
import com.homestay.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeePropertyAssignmentRepository extends JpaRepository<EmployeePropertyAssignment, UUID> {

    @Query("""
            SELECT epa.employee FROM EmployeePropertyAssignment epa
            WHERE epa.property.id = :propertyId
              AND epa.status = :assignmentStatus
              AND epa.employee.role = :employeeRole
            ORDER BY epa.employee.fullName ASC
            """)
    List<User> findActiveEmployeesByPropertyId(
            @Param("propertyId") UUID propertyId,
            @Param("assignmentStatus") EmployeePropertyAssignment.Status assignmentStatus,
            @Param("employeeRole") User.Role employeeRole);

    boolean existsByEmployeeIdAndPropertyIdAndStatus(
            UUID employeeId,
            UUID propertyId,
            EmployeePropertyAssignment.Status status);

    /** SCR-39 — Eager-load employee + property for list mapping (avoid LazyInitializationException). */
    @EntityGraph(attributePaths = {"employee", "property"})
    @Query("""
            SELECT epa FROM EmployeePropertyAssignment epa
            JOIN epa.employee e
            WHERE epa.property.id = :propertyId
              AND epa.status = :assignmentStatus
              AND (:search IS NULL OR :search = '' OR
                   LOWER(e.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(e.phone) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY e.fullName ASC
            """)
    Page<EmployeePropertyAssignment> findByPropertyIdAndStatusWithSearch(
            @Param("propertyId") UUID propertyId,
            @Param("assignmentStatus") EmployeePropertyAssignment.Status assignmentStatus,
            @Param("search") String search,
            Pageable pageable);

    @EntityGraph(attributePaths = {"employee", "property"})
    Optional<EmployeePropertyAssignment> findFirstByEmployee_IdAndStatus(
            UUID employeeId,
            EmployeePropertyAssignment.Status status);

    /** SCR-39 — Scope update/status to employee at a specific property. */
    @EntityGraph(attributePaths = {"employee", "property"})
    Optional<EmployeePropertyAssignment> findByEmployee_IdAndProperty_IdAndStatus(
            UUID employeeId,
            UUID propertyId,
            EmployeePropertyAssignment.Status status);

    // SCR-59: all ACTIVE property assignments for employee KPI scope
    @Query("""
            SELECT epa.property.id FROM EmployeePropertyAssignment epa
            WHERE epa.employee.id = :employeeId
              AND epa.status = :status
            """)
    List<UUID> findPropertyIdsByEmployeeIdAndStatus(
            @Param("employeeId") UUID employeeId,
            @Param("status") EmployeePropertyAssignment.Status status);

    @Query("""
            SELECT u FROM User u
            WHERE u.role = :employeeRole
              AND u.status = :userStatus
              AND NOT EXISTS (
                SELECT 1 FROM EmployeePropertyAssignment epa
                WHERE epa.employee = u
                  AND epa.status = :assignmentStatus
              )
              AND (:search IS NULL OR :search = '' OR
                   LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY u.fullName ASC
            """)
    Page<User> findUnassignedEmployees(
            @Param("search") String search,
            @Param("employeeRole") User.Role employeeRole,
            @Param("userStatus") User.Status userStatus,
            @Param("assignmentStatus") EmployeePropertyAssignment.Status assignmentStatus,
            Pageable pageable);

    /** FR-20 Admin list — optional property filter. */
    @EntityGraph(attributePaths = {"employee", "property"})
    @Query("""
            SELECT epa FROM EmployeePropertyAssignment epa
            JOIN epa.employee e
            WHERE epa.status = :assignmentStatus
              AND (:propertyId IS NULL OR epa.property.id = :propertyId)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(e.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(e.phone) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY e.fullName ASC
            """)
    Page<EmployeePropertyAssignment> findActiveWithOptionalPropertyAndSearch(
            @Param("propertyId") UUID propertyId,
            @Param("assignmentStatus") EmployeePropertyAssignment.Status assignmentStatus,
            @Param("search") String search,
            Pageable pageable);
}
