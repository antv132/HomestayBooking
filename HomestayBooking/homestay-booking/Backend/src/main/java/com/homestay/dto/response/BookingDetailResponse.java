package com.homestay.dto.response;

import com.homestay.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailResponse {
    private UUID id;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    private String roomNumber;
    private String roomType;
    private String propertyName;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guestCount;

    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;
    private BigDecimal damageFeeAmount;

    private String status;
    private String specialRequests;
    /** Deposit hold deadline (SCR-20 countdown); null when not Pending Deposit */
    private LocalDateTime holdExpiresAt;
    private String cancelReason;
    private LocalDateTime createdAt;
    private boolean isReviewed;
    /** FR-14 — present when customer already reviewed this booking */
    private UUID reviewId;

    private List<PaymentInfo> payments;
    private InvoiceBreakdownResponse invoiceBreakdown;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        private UUID id;
        private String type;
        private BigDecimal amount;
        private String method;
        private String status;
        private LocalDateTime paidAt;

        public static PaymentInfo fromEntity(com.homestay.entity.Payment payment) {
            return new PaymentInfo(
                    payment.getId(),
                    payment.getType().name(),
                    payment.getAmount(),
                    payment.getMethod().name(),
                    payment.getStatus().name(),
                    payment.getPaidAt()
            );
        }
    }

    public static BookingDetailResponse fromEntity(Booking booking) {
        return fromEntity(booking, false);
    }

    public static BookingDetailResponse fromEntity(Booking booking, boolean isReviewed) {
        return fromEntity(booking, isReviewed, null);
    }

    public static BookingDetailResponse fromEntity(Booking booking, boolean isReviewed, UUID reviewId) {
        BookingDetailResponse response = new BookingDetailResponse();
        response.setId(booking.getId());
        response.setCustomerId(booking.getCustomer().getId());
        response.setCustomerName(booking.getCustomer().getFullName());
        response.setCustomerEmail(booking.getCustomer().getEmail());
        response.setCustomerPhone(booking.getCustomer().getPhone());
        response.setRoomNumber(booking.getRoom().getRoomNumber());
        response.setRoomType(booking.getRoom().getRoomType());
        response.setPropertyName(booking.getRoom().getProperty().getName());
        response.setCheckInDate(booking.getCheckInDate());
        response.setCheckOutDate(booking.getCheckOutDate());
        response.setGuestCount(booking.getGuestCount());
        response.setTotalAmount(booking.getTotalAmount());
        response.setDepositAmount(booking.getDepositAmount());
        response.setRemainingAmount(booking.getRemainingAmount());
        response.setDamageFeeAmount(booking.getDamageFeeAmount());
        response.setStatus(booking.getStatus().name());
        response.setSpecialRequests(booking.getSpecialRequests());
        response.setHoldExpiresAt(booking.getHoldExpiresAt());
        response.setCancelReason(booking.getCancelReason());
        response.setCreatedAt(booking.getCreatedAt());
        response.setReviewed(isReviewed);
        response.setReviewId(reviewId);
        response.setPayments(null);
        return response;
    }
}
