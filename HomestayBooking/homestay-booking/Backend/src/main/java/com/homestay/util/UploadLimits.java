package com.homestay.util;

import com.homestay.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

/**
 * Constitutional upload cap — {@code sdd/global/security.md} / CONSTITUTION Article 3.
 */
public final class UploadLimits {

    public static final long MAX_BYTES = 10L * 1024 * 1024;

    /** FR-13 maintenance ticket photos — spec max 5 MB per image. */
    public static final long MAINTENANCE_IMAGE_MAX_BYTES = 5L * 1024 * 1024;

    public static final int MAX_ROOM_IMAGES = 10;

    private static final Set<String> ROOM_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private UploadLimits() {}

    public static void validateNotEmpty(MultipartFile file, String emptyMessage) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(emptyMessage);
        }
    }

    public static void validateSize(MultipartFile file, String label) {
        if (file != null && file.getSize() > MAX_BYTES) {
            throw new BusinessException(label + " tối đa 10MB");
        }
    }

    /** FR-08 / FR-012 — jpeg, png, webp only; validate before disk write (LESSON-007). */
    public static void validateRoomImageMime(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File ảnh không được để trống");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ROOM_IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("Chỉ chấp nhận ảnh JPEG, PNG hoặc WebP");
        }
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (!Set.of("jpg", "jpeg", "png", "webp").contains(ext)) {
                throw new BusinessException("Chỉ chấp nhận ảnh JPEG, PNG hoặc WebP");
            }
        }
    }

    /** FR-13 — JPEG/PNG/WebP, max 5MB, before disk write. */
    public static void validateMaintenanceImage(MultipartFile file) {
        validateRoomImageMime(file);
        if (file.getSize() > MAINTENANCE_IMAGE_MAX_BYTES) {
            throw new BusinessException("Ảnh ticket tối đa 5MB");
        }
    }
}
