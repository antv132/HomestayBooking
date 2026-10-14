package com.homestay.controller;

import com.homestay.dto.request.ReorderImagesRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.RoomDetailResponse;
import com.homestay.entity.User;
import com.homestay.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** SCR-32 — Room gallery v1 (Manager, property-scoped). */
@RestController
@RequestMapping("/api/v1/manager/rooms/{roomId}/images")
@RequiredArgsConstructor
public class RoomImageV1Controller {

    private final RoomService roomService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<RoomDetailResponse.RoomImageInfo>>> upload(
            @PathVariable UUID roomId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(defaultValue = "false") boolean setPrimary,
            @AuthenticationPrincipal User currentUser) {
        List<RoomDetailResponse.RoomImageInfo> data =
                roomService.uploadImagesForManager(currentUser, roomId, files, setPrimary);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tải ảnh thành công", data));
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<RoomDetailResponse.RoomImageInfo>>> reorder(
            @PathVariable UUID roomId,
            @Valid @RequestBody ReorderImagesRequest request,
            @AuthenticationPrincipal User currentUser) {
        List<RoomDetailResponse.RoomImageInfo> data =
                roomService.reorderImagesForManager(currentUser, roomId, request);
        return ResponseEntity.ok(ApiResponse.ok("Sắp xếp ảnh thành công", data));
    }

    @PatchMapping("/{imageId}/primary")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> setPrimary(
            @PathVariable UUID roomId,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal User currentUser) {
        roomService.setPrimaryImageForManager(currentUser, roomId, imageId);
        return ResponseEntity.ok(ApiResponse.ok("Đã đặt ảnh đại diện"));
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID roomId,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal User currentUser) {
        roomService.deleteImageForManager(currentUser, roomId, imageId);
        return ResponseEntity.ok(ApiResponse.ok("Xóa ảnh thành công"));
    }
}
