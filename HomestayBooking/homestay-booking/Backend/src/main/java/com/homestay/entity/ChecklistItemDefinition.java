package com.homestay.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Catalog mục checklist kiểm tra phòng (SCR-62).
 * property_id NULL = danh mục global mặc định.
 */
@Entity
@Table(
    name = "checklist_item_definitions",
    indexes = {
        @Index(name = "idx_cid_active_sort", columnList = "is_active, sort_order"),
        @Index(name = "idx_cid_property", columnList = "property_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_cid_code", columnNames = { "code" })
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ChecklistItemDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Column(name = "icon", length = 20)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** NULL = global seed; non-null = property-specific override (phase 2). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
