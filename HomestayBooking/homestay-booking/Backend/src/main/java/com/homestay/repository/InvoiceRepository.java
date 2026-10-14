package com.homestay.repository;

import com.homestay.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    boolean existsByBooking_IdAndType(UUID bookingId, Invoice.Type type);

    Optional<Invoice> findByBooking_IdAndType(UUID bookingId, Invoice.Type type);

    List<Invoice> findByBooking_IdOrderByTypeAsc(UUID bookingId);

    Page<Invoice> findByCustomer_Id(UUID customerId, Pageable pageable);

    Page<Invoice> findByCustomer_IdAndStatus(UUID customerId, Invoice.Status status, Pageable pageable);

    @Query("""
            SELECT i FROM Invoice i
            WHERE i.property.id = :propertyId
              AND (:status IS NULL OR i.status = :status)
            """)
    Page<Invoice> findByPropertyIdAndOptionalStatus(
            @Param("propertyId") UUID propertyId,
            @Param("status") Invoice.Status status,
            Pageable pageable);

    List<Invoice> findByBooking_IdAndStatusIn(UUID bookingId, List<Invoice.Status> statuses);
}
