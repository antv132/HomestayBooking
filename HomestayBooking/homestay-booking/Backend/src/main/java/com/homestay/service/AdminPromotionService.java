package com.homestay.service;

import com.homestay.dto.request.PromotionRequest;
import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.PromotionResponse;
import com.homestay.entity.Promotion;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.PromotionRepository;
import com.homestay.util.PromotionValidators;
import com.homestay.util.UploadLimits;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * FR-18 SCR-57/58 — Admin Promotion CRUD + Active toggle + banner image upload.
 */
@Service
@RequiredArgsConstructor
public class AdminPromotionService {

    private final PromotionRepository promotionRepository;

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public PageResponse<PromotionResponse> listForAdmin(Pageable pageable) {
        Page<Promotion> page = promotionRepository.findAllByOrderBySortOrderAscCreatedAtDesc(pageable);
        return new PageResponse<>(
                page.getContent().stream().map(PromotionResponse::fromEntity).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional
    public PromotionResponse create(PromotionRequest req) {
        Promotion p = new Promotion();
        applyRequest(p, req);
        if (req.getIsActive() == null) {
            p.setActive(true);
        }
        return PromotionResponse.fromEntity(promotionRepository.save(p));
    }

    @Transactional
    public PromotionResponse update(UUID id, PromotionRequest req) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy promotion"));
        applyRequest(p, req);
        return PromotionResponse.fromEntity(promotionRepository.save(p));
    }

    @Transactional
    public PromotionResponse patchActive(UUID id, boolean isActive) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy promotion"));
        p.setActive(isActive);
        return PromotionResponse.fromEntity(promotionRepository.save(p));
    }

    @Transactional
    public void deleteForAdmin(UUID id) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy promotion"));
        promotionRepository.delete(p);
    }

    /** Upload one banner image → public {@code /uploads/promotions/...} URL. */
    public String uploadImage(MultipartFile file) {
        UploadLimits.validateNotEmpty(file, "Cần chọn một ảnh banner");
        UploadLimits.validateRoomImageMime(file);
        UploadLimits.validateSize(file, "Ảnh banner");

        Path dir = Paths.get(uploadDir, "promotions").toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BusinessException("Không thể tạo thư mục upload banner");
        }

        String ext = extensionOf(file.getOriginalFilename(), file.getContentType());
        String savedName = UUID.randomUUID() + ext;
        Path target = dir.resolve(savedName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("Không thể lưu ảnh banner");
        }
        return "/uploads/promotions/" + savedName;
    }

    private void applyRequest(Promotion p, PromotionRequest req) {
        p.setSubtitle(req.getSubtitle().trim());
        p.setTitle(req.getTitle().trim());
        p.setDescription(req.getDescription() != null ? req.getDescription().trim() : null);
        p.setCtaText(req.getCtaText().trim());
        p.setCtaUrl(PromotionValidators.normalizeCtaUrl(req.getCtaUrl()));
        p.setColorTheme(PromotionValidators.normalizeColorTheme(req.getColorTheme()));
        p.setActive(req.getIsActive() != null ? req.getIsActive() : true);
        p.setSortOrder(Math.max(0, req.getSortOrder()));
        // blank = clear image; null = leave unchanged (create → null)
        if (req.getImageUrl() != null) {
            String img = req.getImageUrl().trim();
            p.setImageUrl(img.isEmpty() ? null : img);
        }
    }

    private static String extensionOf(String originalName, String contentType) {
        if (originalName != null && originalName.contains(".")) {
            String ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (ext.matches("\\.(jpe?g|png|webp)")) {
                return ext.equals(".jpeg") ? ".jpg" : ext;
            }
        }
        if (contentType != null) {
            String ct = contentType.toLowerCase(Locale.ROOT);
            if (ct.contains("png")) return ".png";
            if (ct.contains("webp")) return ".webp";
        }
        return ".jpg";
    }
}
