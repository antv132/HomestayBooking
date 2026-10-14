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
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Dòng nhỏ phía trên tiêu đề, VD: "Ưu đãi cuối tuần" */
    @Column(nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String subtitle;

    /** Tiêu đề lớn, VD: "Giảm 20%\nthứ 6 – chủ nhật" */
    @Column(nullable = false, length = 200, columnDefinition = "NVARCHAR(200)")
    private String title;

    /** Mô tả ngắn bên dưới tiêu đề */
    @Column(length = 400, columnDefinition = "NVARCHAR(400)")
    private String description;

    /** Text nút CTA, VD: "Đặt ngay →" */
    @Column(nullable = false, length = 80, columnDefinition = "NVARCHAR(80)")
    private String ctaText;

    /** URL nút CTA, VD: "/search?sort=price-asc" */
    @Column(nullable = false, length = 300, columnDefinition = "NVARCHAR(300)")
    private String ctaUrl;

    /** Ảnh hiển thị trên carousel banner trang chủ */
    @Column(length = 500, columnDefinition = "NVARCHAR(500)")
    private String imageUrl;

    /**
     * Chủ đề màu sắc banner.
     * Giá trị: "red" | "blue" | "green" | "purple" | "orange"
     */
    @Column(nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String colorTheme;

    /** Hiển thị trên landing page hay không */
    @Column(nullable = false)
    private boolean isActive = true;

    /** Thứ tự hiển thị (nhỏ hơn = hiển thị trước) */
    @Column(nullable = false)
    private int sortOrder = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
