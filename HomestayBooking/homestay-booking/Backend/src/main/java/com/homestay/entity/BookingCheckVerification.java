package com.homestay.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "booking_check_verifications",
    indexes = {
        @Index(name = "idx_bcv_booking", columnList = "booking_id"),
        @Index(name = "idx_bcv_type", columnList = "type")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class BookingCheckVerification {

    public enum Type { CHECK_IN, CHECK_OUT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private Type type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "id_document_urls", columnDefinition = "nvarchar(max)")
    private List<String> idDocumentUrls;

    @Column(name = "key_handed_over")
    private Boolean keyHandedOver;

    @Column(name = "key_returned")
    private Boolean keyReturned;

    @Column(name = "remaining_collected")
    private Boolean remainingCollected;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
