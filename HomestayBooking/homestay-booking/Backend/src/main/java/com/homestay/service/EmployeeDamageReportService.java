package com.homestay.service;

import com.homestay.dto.request.CreateEmployeeDamageReportRequest;
import com.homestay.dto.response.EmployeeDamageReportResponse;
import com.homestay.dto.response.EmployeeEligibleDamageRoomResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Attachment;
import com.homestay.entity.DamageItem;
import com.homestay.entity.DamageReport;
import com.homestay.entity.ManagerPropertyAssignment;
import com.homestay.entity.Notification;
import com.homestay.entity.RoomInspection;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.repository.AttachmentRepository;
import com.homestay.repository.DamageReportRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.RoomInspectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SCR-63 — Employee Damage Report List.
 * SCR-64 — Create Damage Report (eligible rooms, photo upload, create).
 */
@Service
@RequiredArgsConstructor
public class EmployeeDamageReportService {

    private static final BigDecimal ESCALATION_THRESHOLD = new BigDecimal("5000000");
    private static final int MAX_PHOTOS = 5;
    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;

    private final DamageReportRepository damageReportRepository;
    private final RoomInspectionRepository roomInspectionRepository;
    private final AttachmentRepository attachmentRepository;
    private final NotificationService notificationService;
    private final ManagerPropertyAssignmentRepository managerPropertyAssignmentRepository;

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public PageResponse<EmployeeDamageReportResponse> list(
            User employee,
            DamageReport.Status status,
            String search,
            Pageable pageable) {
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;
        Page<DamageReport> page = damageReportRepository.findForEmployee(
                employee.getId(), status, normalizedSearch, pageable);
        // Touch items + booking while session is open (EntityGraph không fetch items trên Page).
        page.getContent().forEach(dr -> {
            if (dr.getItems() != null) {
                dr.getItems().size();
            }
            if (dr.getBooking() != null) {
                dr.getBooking().getId();
            }
        });
        List<EmployeeDamageReportResponse> content = page.getContent().stream()
                .map(EmployeeDamageReportResponse::fromEntity)
                .toList();
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<EmployeeEligibleDamageRoomResponse> listEligibleRooms(User employee) {
        return roomInspectionRepository
                .findEligibleForDamageReport(employee.getId(), RoomInspection.Status.FAILED_WITH_DAMAGE)
                .stream()
                .map(EmployeeEligibleDamageRoomResponse::fromEntity)
                .toList();
    }

    /** SCR-64 — Upload evidence photos; returns public /uploads/... URLs. */
    public List<String> uploadPhotos(User employee, List<MultipartFile> files) {
        if (employee == null) {
            throw new BusinessException("Không xác thực được nhân viên");
        }
        if (files == null || files.isEmpty()) {
            throw new BusinessException("Cần ít nhất một ảnh");
        }
        if (files.size() > MAX_PHOTOS) {
            throw new BusinessException("Tối đa " + MAX_PHOTOS + " ảnh mỗi lần tải lên");
        }

        Path dir = Paths.get(uploadDir, "damage").toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BusinessException("Không thể tạo thư mục upload");
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            validatePhoto(file);
            String savedName = UUID.randomUUID() + "_" + sanitizeFilename(file.getOriginalFilename());
            Path target = dir.resolve(savedName);
            try {
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new BusinessException("Không thể lưu ảnh hư hại");
            }
            urls.add("/uploads/damage/" + savedName);
        }

        if (urls.isEmpty()) {
            throw new BusinessException("Cần ít nhất một ảnh hợp lệ");
        }
        return urls;
    }

    @Transactional
    public EmployeeDamageReportResponse create(User employee, CreateEmployeeDamageReportRequest req) {
        validateAttachments(req.getAttachments());

        RoomInspection inspection = resolveInspection(employee, req);

        BigDecimal total = req.getItems().stream()
                .map(CreateEmployeeDamageReportRequest.Item::getEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DamageReport report = new DamageReport();
        report.setInspection(inspection);
        report.setBooking(inspection.getBooking());
        report.setStatus(DamageReport.Status.PENDING_APPROVAL);
        report.setTotalEstimatedCost(total);
        report.setRequiresAdminEscalation(total.compareTo(ESCALATION_THRESHOLD) > 0);
        report.setNote(StringUtils.hasText(req.getNotes()) ? req.getNotes().trim() : null);
        report.setItems(new ArrayList<>());

        for (CreateEmployeeDamageReportRequest.Item i : req.getItems()) {
            DamageItem di = new DamageItem();
            di.setDamageReport(report);
            di.setItemName(i.getName().trim());
            di.setEstimatedCost(i.getEstimatedCost());
            report.getItems().add(di);
        }

        DamageReport saved = damageReportRepository.save(report);

        DamageItem firstItem = saved.getItems().get(0);
        List<Attachment> attachments = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (CreateEmployeeDamageReportRequest.AttachmentRef ref : req.getAttachments()) {
            Attachment a = new Attachment();
            a.setEntityType("DamageItem");
            a.setEntityId(firstItem.getId());
            a.setFileUrl(ref.getUrl().trim());
            a.setUploadedAt(now);
            attachments.add(a);
        }
        attachmentRepository.saveAll(attachments);

        if (saved.getItems() != null) {
            saved.getItems().size();
        }
        notifyDamageReportCreated(employee, saved);
        return EmployeeDamageReportResponse.fromEntity(saved);
    }

    /** 023 FR-017 — notify customer + employee + property managers on create. */
    private void notifyDamageReportCreated(User employee, DamageReport report) {
        String roomNumber = report.getInspection() != null && report.getInspection().getRoom() != null
                ? report.getInspection().getRoom().getRoomNumber()
                : "?";
        String body = "Báo cáo hư hại phòng " + roomNumber
                + " đã được tạo (tổng ước tính "
                + report.getTotalEstimatedCost() + " VND).";

        if (report.getBooking() != null && report.getBooking().getCustomer() != null) {
            notificationService.sendNotification(
                    report.getBooking().getCustomer().getId(),
                    Notification.Type.SYSTEM,
                    "Báo cáo hư hại mới",
                    body,
                    report.getId(),
                    "DamageReport");
        }

        notificationService.sendNotification(
                employee.getId(),
                Notification.Type.SYSTEM,
                "Báo cáo hư hại đã gửi",
                "Bạn đã gửi báo cáo hư hại phòng " + roomNumber + ". Chờ Manager duyệt.",
                report.getId(),
                "DamageReport");

        UUID propertyId = report.getInspection() != null && report.getInspection().getProperty() != null
                ? report.getInspection().getProperty().getId()
                : null;
        if (propertyId == null) {
            return;
        }
        List<ManagerPropertyAssignment> managers = managerPropertyAssignmentRepository
                .findActiveByPropertyIds(List.of(propertyId), ManagerPropertyAssignment.Status.ACTIVE);
        for (ManagerPropertyAssignment mpa : managers) {
            if (mpa.getManager() == null) {
                continue;
            }
            notificationService.sendNotification(
                    mpa.getManager().getId(),
                    Notification.Type.SYSTEM,
                    "Báo cáo hư hại chờ duyệt",
                    body,
                    report.getId(),
                    "DamageReport");
        }
    }

    private RoomInspection resolveInspection(User employee, CreateEmployeeDamageReportRequest req) {
        if (req.getInspectionId() != null) {
            RoomInspection ri = roomInspectionRepository
                    .findFailedByIdForEmployee(
                            req.getInspectionId(),
                            employee.getId(),
                            RoomInspection.Status.FAILED_WITH_DAMAGE)
                    .orElseThrow(() -> new BusinessException(
                            "Không tìm thấy kiểm tra FAILED thuộc về bạn"));
            if (!ri.getRoom().getId().equals(req.getRoomId())) {
                throw new BusinessException("Phòng không khớp với kiểm tra đã chọn");
            }
            if (damageReportRepository.existsByInspection_Id(ri.getId())) {
                throw new BusinessException("Kiểm tra này đã có báo cáo hư hại");
            }
            return ri;
        }

        List<RoomInspection> candidates = roomInspectionRepository.findFailedForEmployeeAndRoom(
                req.getRoomId(),
                employee.getId(),
                RoomInspection.Status.FAILED_WITH_DAMAGE,
                PageRequest.of(0, 5));
        for (RoomInspection ri : candidates) {
            if (!damageReportRepository.existsByInspection_Id(ri.getId())) {
                return ri;
            }
        }
        throw new BusinessException(
                "Không tìm thấy kiểm tra phòng FAILED chưa có báo cáo cho phòng này");
    }

    private void validateAttachments(List<CreateEmployeeDamageReportRequest.AttachmentRef> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            throw new BusinessException("Cần ít nhất một ảnh minh chứng");
        }
        if (attachments.size() > MAX_PHOTOS) {
            throw new BusinessException("Tối đa " + MAX_PHOTOS + " ảnh minh chứng");
        }
        for (CreateEmployeeDamageReportRequest.AttachmentRef ref : attachments) {
            String url = ref.getUrl() != null ? ref.getUrl().trim() : "";
            if (!url.startsWith("/uploads/")) {
                throw new BusinessException("URL ảnh không hợp lệ — vui lòng tải ảnh lên hệ thống");
            }
            if (url.startsWith("blob:") || url.startsWith("data:")) {
                throw new BusinessException("URL ảnh không hợp lệ");
            }
            ref.setUrl(url);
        }
    }

    private void validatePhoto(MultipartFile file) {
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw new BusinessException("Ảnh không được vượt quá 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null
                || (!contentType.equalsIgnoreCase("image/jpeg")
                && !contentType.equalsIgnoreCase("image/png")
                && !contentType.equalsIgnoreCase("image/webp"))) {
            throw new BusinessException("Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP");
        }
    }

    private static String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) {
            return "photo.jpg";
        }
        return original.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
