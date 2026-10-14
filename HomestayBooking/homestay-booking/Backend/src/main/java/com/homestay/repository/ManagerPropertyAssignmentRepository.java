package com.homestay.repository;

import com.homestay.entity.ManagerPropertyAssignment;
import com.homestay.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ManagerPropertyAssignmentRepository extends JpaRepository<ManagerPropertyAssignment, UUID> {

    boolean existsByManagerIdAndPropertyIdAndStatus(
            UUID managerId, UUID propertyId, ManagerPropertyAssignment.Status status);

    boolean existsByPropertyIdAndStatus(UUID propertyId, ManagerPropertyAssignment.Status status);

    /** FR-06 Manager list — ACTIVE assignment; property may be ACTIVE or INACTIVE (audit). */
    @Query("""
        SELECT mpa.property FROM ManagerPropertyAssignment mpa
        WHERE mpa.manager.id = :managerId
          AND mpa.status = 'ACTIVE'
        ORDER BY mpa.property.name ASC
        """)
    List<Property> findAssignedPropertiesByManagerId(@Param("managerId") UUID managerId);

    @Query("""
        SELECT mpa.property FROM ManagerPropertyAssignment mpa
        WHERE mpa.manager.id = :managerId
          AND mpa.status = 'ACTIVE'
          AND mpa.property.status = 'ACTIVE'
        ORDER BY mpa.property.name ASC
        """)
    List<Property> findActivePropertiesByManagerId(@Param("managerId") UUID managerId);

    @Query("""
        SELECT mpa FROM ManagerPropertyAssignment mpa
        JOIN FETCH mpa.manager
        LEFT JOIN FETCH mpa.assignedBy
        WHERE mpa.property.id = :propertyId
        ORDER BY mpa.assignedAt DESC
        """)
    List<ManagerPropertyAssignment> findHistoryByPropertyId(@Param("propertyId") UUID propertyId);

    /** SCR-46 — Manager (kèm user) theo danh sách property, gộp 1 query để tránh N+1. */
    @Query("""
        SELECT mpa FROM ManagerPropertyAssignment mpa
        JOIN FETCH mpa.manager
        WHERE mpa.property.id IN :propertyIds
          AND mpa.status = :status
        """)
    List<ManagerPropertyAssignment> findActiveByPropertyIds(
            @Param("propertyIds") List<UUID> propertyIds,
            @Param("status") ManagerPropertyAssignment.Status status);

    long countByManager_IdAndStatus(UUID managerId, ManagerPropertyAssignment.Status status);

    /** SCR-50 — Đếm property ACTIVE của nhiều manager (tránh N+1). */
    @Query("""
        SELECT mpa.manager.id, COUNT(mpa)
        FROM ManagerPropertyAssignment mpa
        WHERE mpa.manager.id IN :managerIds
          AND mpa.status = :status
        GROUP BY mpa.manager.id
        """)
    List<Object[]> countActiveAssignmentsByManagerIds(
            @Param("managerIds") List<UUID> managerIds,
            @Param("status") ManagerPropertyAssignment.Status status);
}
