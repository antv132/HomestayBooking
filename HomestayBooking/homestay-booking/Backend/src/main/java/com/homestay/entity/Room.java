package com.homestay.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "rooms",
    indexes = {
        @Index(name = "idx_rooms_property", columnList = "property_id"),
        @Index(name = "idx_rooms_floor",    columnList = "floor_id"),
        @Index(name = "idx_rooms_status",   columnList = "status"),
        @Index(name = "idx_rooms_deleted_at", columnList = "deleted_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Room {

    // Trạng thái phòng - được quản lý tự động theo booking flow
    public enum Status {
        AVAILABLE,            // Có thể đặt
        PENDING_DEPOSIT,      // Đã tạo booking, chờ đặt cọc
        RESERVED,             // Đã cọc xong, booking confirmed
        OCCUPIED,             // Khách đang ở (checked in)
        PENDING_CLEANING,     // Chờ dọn phòng (sau check-out)
        CLEANING_IN_PROGRESS, // Đang dọn phòng
        MAINTENANCE,          // Đang bảo trì, không thể đặt
        OUT_OF_SERVICE        // Ngưng hoạt động
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @Column(name = "room_number", nullable = false, length = 50)
    private String roomNumber;

    // Loại phòng: Studio / Standard / Deluxe / Suite / Villa
    @Column(name = "room_type", length = 100)
    private String roomType;

    // Giá theo đêm (VNĐ)
    @Column(name = "price_per_night", nullable = false, precision = 15, scale = 2)
    private BigDecimal pricePerNight;

    // Số người tối đa
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    // Diện tích phòng (m2)
    @Column(name = "area", precision = 8, scale = 2)
    private BigDecimal area;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "amenities")
    private List<String> amenities = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.AVAILABLE;

    /** Soft-delete — null = active; set when Manager deletes (FR-08). */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** FR-14 — denormalized Published review average (null if none). */
    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    /** FR-14 — denormalized Published review count. */
    @Column(name = "total_reviews", nullable = false)
    private Integer totalReviews = 0;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<RoomImage> roomImages;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Booking> bookings;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MaintenanceTicket> maintenanceTickets;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
