package com.homestay.dto.response;

import com.homestay.entity.Booking;
import com.homestay.entity.Payment;
import com.homestay.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentDetailResponseTest {

    @Test
    void fromEntity_mapsAllFields() {
        Payment payment = createPayment();

        PaymentDetailResponse response = PaymentDetailResponse.fromEntity(payment);

        assertThat(response.getId()).isEqualTo(payment.getId());
        assertThat(response.getBookingId()).isEqualTo(payment.getBooking().getId());
        assertThat(response.getCustomerId()).isEqualTo(payment.getCustomer().getId());
        assertThat(response.getCustomerName()).isEqualTo("John Customer");
        assertThat(response.getType()).isEqualTo(Payment.Type.DEPOSIT);
        assertThat(response.getMethod()).isEqualTo(Payment.Method.VNPAY);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("4000000"));
        assertThat(response.getStatus()).isEqualTo(Payment.Status.PAID);
        assertThat(response.getOrderRef()).isEqualTo("ORD123456");
        assertThat(response.getGatewayTransactionId()).isEqualTo("TXN789");
        assertThat(response.getGatewayResponseCode()).isEqualTo("00");
        assertThat(response.getVerifiedByName()).isEqualTo("Verifier User");
        assertThat(response.getVerifiedAt()).isNotNull();
        assertThat(response.getPaidAt()).isNotNull();
        assertThat(response.getVerificationNote()).isEqualTo("Verified manually");
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void fromEntity_withNullVerifier_handlesGracefully() {
        Payment payment = createPayment();
        payment.setVerifiedBy(null);

        PaymentDetailResponse response = PaymentDetailResponse.fromEntity(payment);

        assertThat(response.getVerifiedByName()).isNull();
    }

    @Test
    void fromEntity_withRemainingBalancePayment_mapsType() {
        Payment payment = createPayment();
        payment.setType(Payment.Type.REMAINING_BALANCE);
        payment.setAmount(new BigDecimal("6000000"));

        PaymentDetailResponse response = PaymentDetailResponse.fromEntity(payment);

        assertThat(response.getType()).isEqualTo(Payment.Type.REMAINING_BALANCE);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("6000000"));
    }

    @Test
    void fromEntity_withDamageFeePayment_mapsType() {
        Payment payment = createPayment();
        payment.setType(Payment.Type.DAMAGE_FEE);
        payment.setAmount(new BigDecimal("2000000"));

        PaymentDetailResponse response = PaymentDetailResponse.fromEntity(payment);

        assertThat(response.getType()).isEqualTo(Payment.Type.DAMAGE_FEE);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("2000000"));
    }

    @Test
    void fromEntity_withCashMethod_mapsProperly() {
        Payment payment = createPayment();
        payment.setMethod(Payment.Method.CASH);
        payment.setOrderRef(null);
        payment.setGatewayTransactionId(null);

        PaymentDetailResponse response = PaymentDetailResponse.fromEntity(payment);

        assertThat(response.getMethod()).isEqualTo(Payment.Method.CASH);
        assertThat(response.getOrderRef()).isNull();
        assertThat(response.getGatewayTransactionId()).isNull();
    }

    @Test
    void fromEntity_withPendingStatus_mapsProperly() {
        Payment payment = createPayment();
        payment.setStatus(Payment.Status.PENDING);
        payment.setPaidAt(null);

        PaymentDetailResponse response = PaymentDetailResponse.fromEntity(payment);

        assertThat(response.getStatus()).isEqualTo(Payment.Status.PENDING);
        assertThat(response.getPaidAt()).isNull();
    }

    @Test
    void fromEntity_withFailedStatus_mapsProperly() {
        Payment payment = createPayment();
        payment.setStatus(Payment.Status.FAILED);
        payment.setGatewayResponseCode("99");

        PaymentDetailResponse response = PaymentDetailResponse.fromEntity(payment);

        assertThat(response.getStatus()).isEqualTo(Payment.Status.FAILED);
        assertThat(response.getGatewayResponseCode()).isEqualTo("99");
    }

    private Payment createPayment() {
        User customer = new User();
        customer.setId(UUID.randomUUID());
        customer.setFullName("John Customer");

        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());

        User verifier = new User();
        verifier.setFullName("Verifier User");

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setBooking(booking);
        payment.setCustomer(customer);
        payment.setType(Payment.Type.DEPOSIT);
        payment.setMethod(Payment.Method.VNPAY);
        payment.setAmount(new BigDecimal("4000000"));
        payment.setStatus(Payment.Status.PAID);
        payment.setOrderRef("ORD123456");
        payment.setGatewayTransactionId("TXN789");
        payment.setGatewayResponseCode("00");
        payment.setVerifiedBy(verifier);
        payment.setVerifiedAt(LocalDateTime.now());
        payment.setPaidAt(LocalDateTime.now());
        payment.setVerificationNote("Verified manually");
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        return payment;
    }
}
