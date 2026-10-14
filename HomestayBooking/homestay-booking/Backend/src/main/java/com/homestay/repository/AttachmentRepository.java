package com.homestay.repository;

import com.homestay.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * SCR-53: doc file dinh kem theo entity da hinh (entityType + entityId).
 * Anh bang chung damage: entityType = "DamageItem", entityId = damageItem.id.
 */
@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByEntityTypeAndEntityIdIn(String entityType, List<UUID> entityIds);
}