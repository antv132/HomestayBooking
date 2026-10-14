package com.homestay.dto.response;

import com.homestay.entity.Booking;
import com.homestay.entity.RoomInspection;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ManagerBookingDetailResponse {

    private UUID id;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    private UUID roomId;
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
    private LocalDateTime createdAt;
    private boolean isReviewed;

    private boolean canCheckIn;
    private boolean canCheckOut;
    private boolean canCancel;
    private boolean canModify;
    private String checkInBlockedReason;
    private String checkOutBlockedReason;

    private List<BookingDetailResponse.PaymentInfo> payments;
    private InvoiceBreakdownResponse invoiceBreakdown;

    public static ManagerBookingDetailResponse fromBooking(
            Booking booking,
            boolean isReviewed,
            Optional<RoomInspection> inspection,
            List<BookingDetailResponse.PaymentInfo> payments) {

        ManagerBookingDetailResponse resp = new ManagerBookingDetailResponse();
        resp.setId(booking.getId());
        resp.setCustomerId(booking.getCustomer().getId());
        resp.setCustomerName(booking.getCustomer().getFullName());
        resp.setCustomerEmail(booking.getCustomer().getEmail());
        resp.setCustomerPhone(booking.getCustomer().getPhone());
        resp.setRoomId(booking.getRoom().getId());
        resp.setRoomNumber(booking.getRoom().getRoomNumber());
        resp.setRoomType(booking.getRoom().getRoomType());
        resp.setPropertyName(booking.getRoom().getProperty().getName());
        resp.setCheckInDate(booking.getCheckInDate());
        resp.setCheckOutDate(booking.getCheckOutDate());
        resp.setGuestCount(booking.getGuestCount());
        resp.setTotalAmount(booking.getTotalAmount());
        resp.setDepositAmount(booking.getDepositAmount());
        resp.setRemainingAmount(booking.getRemainingAmount());
        resp.setDamageFeeAmount(booking.getDamageFeeAmount());
        resp.setStatus(booking.getStatus().name());
        resp.setSpecialRequests(booking.getSpecialRequests());
        resp.setCreatedAt(booking.getCreatedAt());
        resp.setReviewed(isReviewed);
        resp.setPayments(payments);
        applyActionFlags(resp, booking, inspection);
        return resp;
    }

    private static void applyActionFlags(
            ManagerBookingDetailResponse resp,
            Booking booking,
            Optional<RoomInspection> inspection) {

        LocalDate today = LocalDate.now();
        Booking.Status status = booking.getStatus();

        resp.setCanCancel(status == Booking.Status.PENDING_DEPOSIT
                || status == Booking.Status.CONFIRMED);
        resp.setCanModify(status == Booking.Status.CONFIRMED);

        if (status == Booking.Status.CONFIRMED) {
            if (booking.getCheckInDate().isAfter(today)) {
                resp.setCanCheckIn(false);
                resp.setCheckInBlockedReason(
                        "Chưa đến ngày nhận phòng (" + booking.getCheckInDate() + "). Nút sẽ mở từ ngày này.");
            } else {
                resp.setCanCheckIn(true);
                resp.setCheckInBlockedReason(null);
            }
        } else if (status == Booking.Status.PENDING_DEPOSIT) {
            resp.setCanCheckIn(false);
            resp.setCheckInBlockedReason("Khách chưa thanh toán / xác nhận tiền cọc — booking vẫn Chờ cọc.");
        } else {
            resp.setCanCheckIn(false);
            resp.setCheckInBlockedReason(null);
        }

        switch (status) {
            case CHECKED_IN -> {
                resp.setCanCheckOut(true);
                resp.setCheckOutBlockedReason(null);
            }
            case PENDING_INSPECTION -> {
                boolean passed = inspection
                        .map(i -> i.getStatus() == RoomInspection.Status.PASSED)
                        .orElse(false);
                resp.setCanCheckOut(passed);
                resp.setCheckOutBlockedReason(passed ? null : "Đang chờ kiểm tra phòng");
            }
            case PENDING_DAMAGE_PAYMENT -> {
                resp.setCanCheckOut(false);
                resp.setCheckOutBlockedReason("Khách chưa thanh toán phí thiệt hại");
            }
            default -> {
                resp.setCanCheckOut(false);
                resp.setCheckOutBlockedReason(null);
            }
        }
    }
}
