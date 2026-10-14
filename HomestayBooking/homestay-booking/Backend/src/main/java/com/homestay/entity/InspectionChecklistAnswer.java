package com.homestay.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/** Kết quả từng mục checklist gắn với một RoomInspection (SCR-62). */
@Entity
@Table(
    name = "inspection_checklist_answers",
    indexes = {
        @Index(name = "idx_ica_inspection", columnList = "inspection_id"),
        @Index(name = "idx_ica_item", columnList = "checklist_item_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ica_inspection_item", columnNames = { "inspection_id", "checklist_item_id" })
    }
)
@Getter
@Setter
@NoArgsConstructor
public class InspectionChecklistAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    private RoomInspection inspection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checklist_item_id", nullable = false)
    private ChecklistItemDefinition checklistItem;

    @Column(name = "passed", nullable = false)
    private Boolean passed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
