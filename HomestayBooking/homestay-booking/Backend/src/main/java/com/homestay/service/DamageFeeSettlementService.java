package com.homestay.service;

import com.homestay.entity.Booking;
import com.homestay.entity.DamageReport;
import com.homestay.entity.Payment;
import com.homestay.exception.BusinessException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.DamageReportRepository;
import com.homestay.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DamageFeeSettlementService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final DamageReportRepository damageReportRepository;

    @Transactional
    public void applyApprovedFee(DamageReport report, BigDecimal approvedAmount) {
        if (approvedAmount == null || approvedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("So tien boi thuong phai lon hon 0");
        }
        if (report.getBooking() == null) {
            throw new BusinessException("Bao cao hu hai thieu booking");
        }

        Booking booking = report.getBooking();
        booking.setDamageFeeAmount(approvedAmount);
        booking.setStatus(Booking.Status.PENDING_DAMAGE_PAYMENT);
        bookingRepository.save(booking);

        List<Payment> existing = paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId());
        boolean hasOpenDamageFee = existing.stream().anyMatch(p ->
                p.getType() == Payment.Type.DAMAGE_FEE
                        && (p.getStatus() == Payment.Status.PENDING || p.getStatus() == Payment.Status.PAID));
        if (hasOpenDamageFee) {
            return;
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setCustomer(booking.getCustomer());
        payment.setType(Payment.Type.DAMAGE_FEE);
        payment.setAmount(approvedAmount);
        payment.setMethod(Payment.Method.VNPAY);
        payment.setStatus(Payment.Status.PENDING);
        paymentRepository.save(payment);
    }

    /**
     * After DAMAGE_FEE VNPay success: mark report Paid and move booking back to
     * PENDING_INSPECTION so Manager can finish check-out (004 FR-013 / 023 FR-012).
     */
    @Transactional
    public void markDamageReportPaidForBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException("Booking không tồn tại"));

        if (booking.getStatus() == Booking.Status.PENDING_DAMAGE_PAYMENT) {
            booking.setStatus(Booking.Status.PENDING_INSPECTION);
            bookingRepository.save(booking);
        }

        damageReportRepository
                .findFirstByBooking_IdAndStatus(bookingId, DamageReport.Status.APPROVED)
                .ifPresent(dr -> {
                    dr.setStatus(DamageReport.Status.PAID);
                    damageReportRepository.save(dr);
                });
    }
}
