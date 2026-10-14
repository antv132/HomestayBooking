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
@Table(name = "maintenance_tickets",
    indexes = {
        @Index(name = "idx_maint_customer", columnList = "customer_id"),
        @Index(name = "idx_maint_room",     columnList = "room_id"),
        @Index(name = "idx_maint_status",   columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class MaintenanceTicket {

    // OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED
    public enum Status { OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Danh sách URL ảnh dưới dạng JSON string (chứa mảng URL)
    @Column(name = "photo_urls", columnDefinition = "TEXT")
    private String photoUrls;

    // Employee được gán xử lý yêu cầu bảo trì
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee_id")
    private User assignedEmployee;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.OPEN;

    // Manager ghi chú khi xác nhận đóng (CLOSED)
    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    /** Employee internal note (ASSIGNED→RESOLVED); không hiển thị Customer đến khi CLOSED. */
    @Column(name = "work_note", columnDefinition = "TEXT")
    private String workNote;

    // Manager xác nhận hoàn thành bảo trì
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /** Soft-delete (FR-013); null = active. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
