package com.homestay.repository;

import com.homestay.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("""
        SELECT e FROM OutboxEvent e
        WHERE e.status = :status
        ORDER BY e.createdAt ASC
        """)
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(
            @Param("status") OutboxEvent.Status status,
            Pageable pageable);

    @Query("""
        SELECT e FROM OutboxEvent e
        WHERE e.status = :status
        AND e.eventType IN :eventTypes
        ORDER BY e.createdAt DESC
        """)
    org.springframework.data.domain.Page<OutboxEvent> findByStatusAndEventTypeIn(
            @Param("status") OutboxEvent.Status status,
            @Param("eventTypes") List<String> eventTypes,
            Pageable pageable);
}
