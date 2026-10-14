package com.homestay.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "room_inspections",
    indexes = {
        @Index(name = "idx_ri_booking",  columnList = "booking_id", unique = true),
        @Index(name = "idx_ri_room",     columnList = "room_id"),
        @Index(name = "idx_ri_property", columnList = "property_id"),
        @Index(name = "idx_ri_status",   columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class RoomInspection {

    // Pending -> In Progress -> Passed / Failed With Damage
    public enum Status { PENDING, IN_PROGRESS, PASSED, FAILED_WITH_DAMAGE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Mỗi Booking chỉ có một RoomInspection duy nhất
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    // Employee thực hiện kiểm tra
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspected_by")
    private User inspectedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status = Status.PENDING;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "inspected_at")
    private LocalDateTime inspectedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
