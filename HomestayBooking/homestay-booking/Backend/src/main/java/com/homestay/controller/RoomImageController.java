package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * SCR-43: Room Gallery Management
 * Handles per-image operations (set-primary, delete).
 * Upload is handled by RoomController POST /api/rooms/{id}/images
 */
@RestController
@RequestMapping("/api/room-images")
public class RoomImageController {

    private final RoomService roomService;

    public RoomImageController(RoomService roomService) {
        this.roomService = roomService;
    }

    // Đặt ảnh làm primary (ảnh bìa hiển thị trên listing)
    @PatchMapping("/{id}/set-primary")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> setPrimary(@PathVariable UUID id) {
        roomService.setPrimaryImage(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã đặt ảnh làm primary"));
    }

    // Xóa ảnh khỏi gallery
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable UUID id) {
        roomService.deleteImage(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa ảnh thành công"));
    }
}
