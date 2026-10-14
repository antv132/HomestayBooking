package com.homestay.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attachments",
    indexes = {
        @Index(name = "idx_attach_entity", columnList = "entity_type, entity_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Loại entity sở hữu file: "Maintenance" / "DamageItem"
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    // ID của entity sở hữu
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}
