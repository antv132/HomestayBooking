package com.homestay.repository;

import com.homestay.entity.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {

    // Tìm kiếm property theo tên (manager)
    Page<Property> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Đếm theo status
    long countByStatus(Property.Status status);

    // Lọc theo status
    Page<Property> findByStatus(Property.Status status, Pageable pageable);

    // Tìm theo tên và status
    Page<Property> findByNameContainingIgnoreCaseAndStatus(
            String name, Property.Status status, Pageable pageable);

    // Tìm theo tên hoặc địa chỉ (SCR-33 search)
    @Query("SELECT p FROM Property p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.address) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Property> searchByNameOrAddress(@Param("keyword") String keyword, Pageable pageable);

    // Tìm theo tên/địa chỉ và status
    @Query("SELECT p FROM Property p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.address) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND p.status = :status")
    Page<Property> searchByNameOrAddressAndStatus(
            @Param("keyword") String keyword,
            @Param("status") Property.Status status,
            Pageable pageable);

    /**
     * SCR-46 — Admin Property list: keyword (name/address) + status + assigned manager.
     * Null/blank filters are ignored.
     */
    @Query("""
            SELECT DISTINCT p FROM Property p
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.address) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR p.status = :status)
              AND (:managerId IS NULL OR EXISTS (
                    SELECT 1 FROM ManagerPropertyAssignment mpa
                    WHERE mpa.property = p
                      AND mpa.manager.id = :managerId
                      AND mpa.status = 'ACTIVE'
                  ))
            """)
    Page<Property> searchForAdmin(
            @Param("keyword") String keyword,
            @Param("status") Property.Status status,
            @Param("managerId") UUID managerId,
            Pageable pageable);
}
