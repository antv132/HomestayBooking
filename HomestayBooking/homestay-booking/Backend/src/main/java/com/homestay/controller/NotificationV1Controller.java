package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.entity.User;
import com.homestay.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/** SCR-13/SCR-14 — authenticated user notifications (api-spec v1). */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationV1Controller {

    private final NotificationService notificationService;

    public NotificationV1Controller(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotifications(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        Map<String, Object> data = notificationService.getNotifications(
                currentUser.getId(), page, size, unreadOnly);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount(
            @AuthenticationPrincipal User currentUser) {
        long count = notificationService.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    /** Contract: PATCH /mark-all-read — keep POST /read-all for FE compat. */
    @PatchMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllReadPatch(
            @AuthenticationPrincipal User currentUser) {
        return markAllRead(currentUser);
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllRead(
            @AuthenticationPrincipal User currentUser) {
        int updated = notificationService.markAllRead(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã đánh dấu tất cả thông báo là đã đọc",
                Map.of("updated", updated)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotificationDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> data = notificationService.getNotificationDetail(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAsReadPatch(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return markAsRead(id, currentUser);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> data = notificationService.markAsRead(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã đánh dấu thông báo là đã đọc", data));
    }
}
