package com.homestay.dto.response;

import com.homestay.entity.Booking;
import com.homestay.entity.Payment;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BookingDetailResponseTest {

    @Test
    void fromEntity_mapsAllBookingFields() {
        Booking booking = createBooking();

        BookingDetailResponse response = BookingDetailResponse.fromEntity(booking);

        assertThat(response.getId()).isEqualTo(booking.getId());
        assertThat(response.getCustomerId()).isEqualTo(booking.getCustomer().getId());
        assertThat(response.getCustomerName()).isEqualTo("John Doe");
        assertThat(response.getCustomerEmail()).isEqualTo("john@test.com");
        assertThat(response.getCustomerPhone()).isEqualTo("123456789");
        assertThat(response.getRoomNumber()).isEqualTo("101");
        assertThat(response.getRoomType()).isEqualTo("DELUXE");
        assertThat(response.getPropertyName()).isEqualTo("Test Homestay");
        assertThat(response.getCheckInDate()).isEqualTo(booking.getCheckInDate());
        assertThat(response.getCheckOutDate()).isEqualTo(booking.getCheckOutDate());
        assertThat(response.getGuestCount()).isEqualTo(2);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("10000000"));
        assertThat(response.getDepositAmount()).isEqualByComparingTo(new BigDecimal("4000000"));
        assertThat(response.getRemainingAmount()).isEqualByComparingTo(new BigDecimal("6000000"));
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getSpecialRequests()).isEqualTo("Late check-in");
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.isReviewed()).isFalse();
        assertThat(response.getReviewId()).isNull();
    }

    @Test
    void fromEntity_withReviewedFlag_setsReviewedTrue() {
        Booking booking = createBooking();

        BookingDetailResponse response = BookingDetailResponse.fromEntity(booking, true);

        assertThat(response.isReviewed()).isTrue();
        assertThat(response.getReviewId()).isNull();
    }

    @Test
    void fromEntity_withReviewId_setsReviewedAndId() {
        Booking booking = createBooking();
        UUID reviewId = UUID.randomUUID();

        BookingDetailResponse response = BookingDetailResponse.fromEntity(booking, true, reviewId);

        assertThat(response.isReviewed()).isTrue();
        assertThat(response.getReviewId()).isEqualTo(reviewId);
    }

    @Test
    void fromEntity_withNullDamageFee_handlesProperly() {
        Booking booking = createBooking();
        booking.setDamageFeeAmount(null);

        BookingDetailResponse response = BookingDetailResponse.fromEntity(booking);

        assertThat(response.getDamageFeeAmount()).isNull();
    }

    @Test
    void fromEntity_withHoldExpiresAt_mapsField() {
        Booking booking = createBooking();
        LocalDateTime holdExpiry = LocalDateTime.now().plusMinutes(10);
        booking.setHoldExpiresAt(holdExpiry);

        BookingDetailResponse response = BookingDetailResponse.fromEntity(booking);

        assertThat(response.getHoldExpiresAt()).isEqualTo(holdExpiry);
    }

    @Test
    void fromEntity_withCancelReason_mapsField() {
        Booking booking = createBooking();
        booking.setCancelReason("Customer requested");

        BookingDetailResponse response = BookingDetailResponse.fromEntity(booking);

        assertThat(response.getCancelReason()).isEqualTo("Customer requested");
    }

    @Test
    void paymentInfoFromEntity_mapsPaymentFields() {
        Payment payment = createPayment();

        BookingDetailResponse.PaymentInfo info = BookingDetailResponse.PaymentInfo.fromEntity(payment);

        assertThat(info.getId()).isEqualTo(payment.getId());
        assertThat(info.getType()).isEqualTo("DEPOSIT");
        assertThat(info.getAmount()).isEqualByComparingTo(new BigDecimal("4000000"));
        assertThat(info.getMethod()).isEqualTo("VNPAY");
        assertThat(info.getStatus()).isEqualTo("PAID");
        assertThat(info.getPaidAt()).isNotNull();
    }

    private Booking createBooking() {
        User customer = new User();
        customer.setId(UUID.randomUUID());
        customer.setFullName("John Doe");
        customer.setEmail("john@test.com");
        customer.setPhone("123456789");

        Property property = new Property();
        property.setId(UUID.randomUUID());
        property.setName("Test Homestay");

        Room room = new Room();
        room.setId(UUID.randomUUID());
        room.setRoomNumber("101");
        room.setRoomType("DELUXE");
        room.setProperty(property);

        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setCustomer(customer);
        booking.setRoom(room);
        booking.setCheckInDate(LocalDate.now().plusDays(1));
        booking.setCheckOutDate(LocalDate.now().plusDays(3));
        booking.setGuestCount(2);
        booking.setTotalAmount(new BigDecimal("10000000"));
        booking.setDepositAmount(new BigDecimal("4000000"));
        booking.setRemainingAmount(new BigDecimal("6000000"));
        booking.setStatus(Booking.Status.CONFIRMED);
        booking.setSpecialRequests("Late check-in");
        booking.setCreatedAt(LocalDateTime.now());

        return booking;
    }

    private Payment createPayment() {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setType(Payment.Type.DEPOSIT);
        payment.setAmount(new BigDecimal("4000000"));
        payment.setMethod(Payment.Method.VNPAY);
        payment.setStatus(Payment.Status.PAID);
        payment.setPaidAt(LocalDateTime.now());
        return payment;
    }
}
