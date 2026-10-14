package com.homestay.service;

import com.homestay.dto.request.DisputeDamageReportRequest;
import com.homestay.dto.response.CustomerDamageReportResponse;
import com.homestay.entity.DamageReport;
import com.homestay.entity.Notification;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ForbiddenException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.DamageReportRepository;
import com.homestay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 023 FR-013 / US6 — Customer dispute damage fee within 24h of approval.
 */
@Service
@RequiredArgsConstructor
public class CustomerDamageReportService {

    private final DamageReportRepository damageReportRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<CustomerDamageReportResponse> listForCustomer(User customer) {
        return damageReportRepository.findForCustomer(customer.getId()).stream()
                .map(CustomerDamageReportResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerDamageReportResponse getForCustomer(User customer, UUID id) {
        DamageReport dr = loadOwned(customer, id);
        return CustomerDamageReportResponse.fromEntity(dr);
    }

    @Transactional
    public CustomerDamageReportResponse dispute(User customer, UUID id, DisputeDamageReportRequest request) {
        DamageReport dr = loadOwned(customer, id);

        if (dr.getStatus() != DamageReport.Status.APPROVED) {
            throw new BusinessException("Chỉ có thể tranh chấp báo cáo đã được duyệt bồi thường");
        }
        if (dr.getApprovedAt() == null) {
            throw new BusinessException("Báo cáo thiếu thời điểm duyệt — không thể tranh chấp");
        }

        LocalDateTime deadline = CustomerDamageReportResponse.disputeDeadlineOf(dr.getApprovedAt());
        ZonedDateTime now = ZonedDateTime.now(CustomerDamageReportResponse.VN_ZONE);
        if (deadline == null || now.isAfter(deadline.atZone(CustomerDamageReportResponse.VN_ZONE))) {
            throw new BusinessException("DISPUTE_WINDOW_EXPIRED: Đã hết thời hạn phản đối (24 giờ sau khi duyệt)");
        }

        dr.setStatus(DamageReport.Status.DISPUTED);
        dr.setDisputeNote(request.getNote().trim());
        dr.setDisputedAt(LocalDateTime.now());
        DamageReport saved = damageReportRepository.save(dr);

        notifyAdmins(saved);
        notifyCustomerConfirm(saved);
        notifyInspector(saved);

        return CustomerDamageReportResponse.fromEntity(saved);
    }

    private DamageReport loadOwned(User customer, UUID id) {
        DamageReport dr = damageReportRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo hư hại"));
        UUID ownerId = dr.getBooking().getCustomer().getId();
        if (!ownerId.equals(customer.getId())) {
            throw new ForbiddenException("Bạn không có quyền truy cập báo cáo hư hại này");
        }
        return dr;
    }

    private void notifyAdmins(DamageReport dr) {
        String shortId = dr.getId().toString().substring(0, 8).toUpperCase();
        for (User admin : userRepository.findByRole(User.Role.ADMIN)) {
            notificationService.sendNotification(
                    admin.getId(),
                    Notification.Type.SYSTEM,
                    "Khách tranh chấp phí hư hại",
                    "Báo cáo #" + shortId + " đã bị khách tranh chấp. Vui lòng xử lý (v1: ngoài băng).",
                    dr.getId(),
                    "DamageReport");
        }
    }

    private void notifyCustomerConfirm(DamageReport dr) {
        notificationService.sendNotification(
                dr.getBooking().getCustomer().getId(),
                Notification.Type.SYSTEM,
                "Đã gửi tranh chấp phí hư hại",
                "Yêu cầu tranh chấp của bạn đã được ghi nhận. Thanh toán phí hư hại tạm khóa đến khi Admin xử lý.",
                dr.getId(),
                "DamageReport");
    }

    private void notifyInspector(DamageReport dr) {
        User inspector = dr.getInspection().getInspectedBy();
        if (inspector == null) {
            return;
        }
        notificationService.sendNotification(
                inspector.getId(),
                Notification.Type.SYSTEM,
                "Khách tranh chấp báo cáo hư hại",
                "Khách đã tranh chấp báo cáo hư hại liên quan phòng "
                        + dr.getInspection().getRoom().getRoomNumber() + ".",
                dr.getId(),
                "DamageReport");
    }
}
