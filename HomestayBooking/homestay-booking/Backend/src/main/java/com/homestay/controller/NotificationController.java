package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.entity.User;
import com.homestay.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ── GET /api/notifications?page=0&size=10&unreadOnly=false ──
    // Lấy danh sách thông báo phân trang của user đang đăng nhập
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotifications(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        Map<String, Object> data = notificationService.getNotifications(
                currentUser.getId(), page, size, unreadOnly);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    // ── GET /api/notifications/unread-count ──
    // Đếm số thông báo chưa đọc (phục vụ badge chuông)
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount(
            @AuthenticationPrincipal User currentUser
    ) {
        long count = notificationService.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    // ── PATCH /api/notifications/mark-all-read ──
    // Đánh dấu tất cả thông báo là đã đọc
    @PatchMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllRead(
            @AuthenticationPrincipal User currentUser
    ) {
        int updated = notificationService.markAllRead(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Marked all as read",
                Map.of("updated", updated)));
    }

    // ── GET /api/notifications/{id} ──
    // Xem chi tiết 1 thông báo + auto mark as read (SCR-15)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotificationDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        Map<String, Object> data = notificationService.getNotificationDetail(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    // ── DELETE /api/notifications/{id} ──
    // Manager xóa 1 thông báo
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable UUID id
    ) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.ok("Notification deleted"));
    }

    // ── GET /api/notifications/test-seed ──
    // DEV ONLY: Seed demo notifications for testing the UI
    @GetMapping("/test-seed")
    public ResponseEntity<ApiResponse<Void>> testSeed(
            @AuthenticationPrincipal User currentUser
    ) {
        // BOOKING_CONFIRMED
        notificationService.sendNotification(
                currentUser.getId(),
                com.homestay.entity.Notification.Type.BOOKING_CONFIRMED,
                "Booking Confirmed",
                "Your booking request for Villa 01 has been confirmed by the resort manager.",
                UUID.randomUUID(),
                "Booking"
        );

        // CONTRACT_GENERATED
        notificationService.sendNotification(
                currentUser.getId(),
                com.homestay.entity.Notification.Type.CONTRACT_GENERATED,
                "Contract Generated",
                "Your accommodation contract is ready. Please view and sign it to finalize your booking.",
                UUID.randomUUID(),
                "Contract"
        );

        // PAYMENT_CONFIRMED
        notificationService.sendNotification(
                currentUser.getId(),
                com.homestay.entity.Notification.Type.PAYMENT_CONFIRMED,
                "Payment Verified",
                "Your deposit payment of ₫2,000,000 has been verified successfully.",
                UUID.randomUUID(),
                "Payment"
        );

        // MAINTENANCE_UPDATED
        notificationService.sendNotification(
                currentUser.getId(),
                com.homestay.entity.Notification.Type.MAINTENANCE_UPDATED,
                "Maintenance Update",
                "Your ticket 'Air conditioner leaking' status has been updated to IN PROGRESS.",
                UUID.randomUUID(),
                "MaintenanceTicket"
        );

        return ResponseEntity.ok(ApiResponse.ok("Test notifications seeded successfully"));
    }
}
