package com.homestay.dto.response;

import com.homestay.entity.Booking;
import com.homestay.entity.Payment;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentReconciliationResponseTest {

    @Test
    void fromEntity_gatewaySuccessButNotPaid() {
        Payment p = basePayment(Payment.Status.PENDING, "00");
        PaymentReconciliationResponse res = PaymentReconciliationResponse.fromEntity(p);
        assertEquals("SUCCESS", res.getVnpayStatus());
        assertEquals("Gateway success but system not PAID", res.getDiscrepancyReason());
    }

    @Test
    void fromEntity_paidWithoutGateway() {
        Payment p = basePayment(Payment.Status.PAID, null);
        PaymentReconciliationResponse res = PaymentReconciliationResponse.fromEntity(p);
        assertEquals("NO_RESPONSE", res.getVnpayStatus());
        assertEquals("System PAID but gateway not confirmed", res.getDiscrepancyReason());
    }

    @Test
    void fromEntity_failedCode() {
        Payment p = basePayment(Payment.Status.PENDING, "24");
        PaymentReconciliationResponse res = PaymentReconciliationResponse.fromEntity(p);
        assertEquals("FAILED_24", res.getVnpayStatus());
        assertEquals("Pending without IPN (timeout)", res.getDiscrepancyReason());
    }

    private static Payment basePayment(Payment.Status status, String code) {
        User customer = TestFixtures.user(User.Role.CUSTOMER);
        Property property = TestFixtures.property();
        Room room = TestFixtures.room(property);
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_DEPOSIT);
        Payment p = TestFixtures.payment(booking, Payment.Type.DEPOSIT, status);
        p.setGatewayResponseCode(code);
        return p;
    }
}
