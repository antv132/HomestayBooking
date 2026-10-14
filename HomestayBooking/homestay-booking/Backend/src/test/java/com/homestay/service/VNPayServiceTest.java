package com.homestay.service;

import com.homestay.config.VNPayConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VNPayServiceTest {

    @Mock VNPayConfig vnPayConfig;

    private VNPayService service;

    @BeforeEach
    void setUp() {
        service = new VNPayService(vnPayConfig);
        lenient().when(vnPayConfig.getVnp_TmnCode()).thenReturn("TESTTMN");
        lenient().when(vnPayConfig.getSecretKey()).thenReturn("TESTHASHSECRET0123456789");
        lenient().when(vnPayConfig.getVnp_PayUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        lenient().when(vnPayConfig.hmacSHA512(anyString(), anyString())).thenReturn("fixed-signature");
    }

    @Test
    void createOrder_includesSignedHashAndPaymentId() {
        String paymentId = UUID.randomUUID().toString();
        String url = service.createOrder(
                4000000L,
                "Deposit",
                "http://localhost:8080/api/payments/vnpay/return",
                paymentId);

        assertTrue(url.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"));
        assertTrue(url.contains("vnp_SecureHash=fixed-signature"));
        assertTrue(url.contains(paymentId));
    }

    @Test
    void verifySignature_acceptsMatchingHash() {
        Map<String, String> fields = new HashMap<>();
        fields.put("vnp_Amount", "400000000");
        fields.put("vnp_ResponseCode", "00");
        fields.put("vnp_TxnRef", "pay-1");
        fields.put("vnp_SecureHash", "fixed-signature");

        assertTrue(service.verifySignature(fields));
    }

    @Test
    void verifySignature_rejectsTamperedHash() {
        Map<String, String> fields = new HashMap<>();
        fields.put("vnp_Amount", "100");
        fields.put("vnp_TxnRef", "pay-2");
        fields.put("vnp_SecureHash", "tampered");

        assertFalse(service.verifySignature(fields));
    }
}
