package com.homestay.repository;

import com.homestay.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    /** Public: Active only — sortOrder ASC, createdAt DESC tie-break (FR-18 US1/US4). */
    List<Promotion> findByIsActiveTrueOrderBySortOrderAscCreatedAtDesc();

    /** Admin: all banners — sortOrder ASC, createdAt DESC. */
    List<Promotion> findAllByOrderBySortOrderAscCreatedAtDesc();

    Page<Promotion> findAllByOrderBySortOrderAscCreatedAtDesc(Pageable pageable);
}
