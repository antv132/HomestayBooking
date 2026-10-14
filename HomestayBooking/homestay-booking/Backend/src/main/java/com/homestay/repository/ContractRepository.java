package com.homestay.repository;

import com.homestay.entity.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

    // Legacy/admin-style unscoped list (kept for compatibility)
    @Query("SELECT c FROM Contract c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:search IS NULL OR (" +
           "LOWER(c.customer.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(CAST(c.booking.id AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.room.roomNumber) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Contract> findAllWithFilters(@Param("status") Contract.Status status, @Param("search") String search, Pageable pageable);

    /** SCR-38 — Manager contracts scoped to assigned properties. */
    @Query("SELECT c FROM Contract c WHERE c.room.property.id IN :propertyIds " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:search IS NULL OR (" +
           "LOWER(c.customer.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(CAST(c.booking.id AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.room.roomNumber) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Contract> findByPropertyIdsWithFilters(
            @Param("propertyIds") java.util.List<UUID> propertyIds,
            @Param("status") Contract.Status status,
            @Param("search") String search,
            Pageable pageable);

    // Customer: own contracts; search bookingId / room / property
    @Query("SELECT c FROM Contract c WHERE c.customer.id = :customerId " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:search IS NULL OR (" +
           "LOWER(CAST(c.booking.id AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.room.roomNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.room.property.name) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Contract> findByCustomerWithFilters(
            @Param("customerId") UUID customerId,
            @Param("status") Contract.Status status,
            @Param("search") String search,
            Pageable pageable);


    java.util.Optional<Contract> findByBookingId(UUID bookingId);
}

