package com.homestay.controller;

import com.homestay.config.VNPayConfig;
import com.homestay.entity.Booking;
import com.homestay.entity.Payment;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.service.PaymentService;
import com.homestay.service.VNPayService;
import com.homestay.support.TestFixtures;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerPaymentControllerTest {

    @Mock VNPayService vnPayService;
    @Mock PaymentService paymentService;
    @Mock VNPayConfig vnPayConfig;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;

    private CustomerPaymentController controller;
    private User customer;
    private Payment payment;

    @BeforeEach
    void setUp() {
        controller = new CustomerPaymentController(vnPayService, paymentService, vnPayConfig);
        customer = TestFixtures.user(User.Role.CUSTOMER);
        Property property = TestFixtures.property();
        Room room = TestFixtures.room(property);
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_DEPOSIT);
        payment = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PENDING);
    }

    @Test
    void createPaymentUrl() {
        UUID bookingId = UUID.randomUUID();
        when(paymentService.createVnpayPaymentUrl(bookingId, "DEPOSIT", customer))
                .thenReturn(Map.of("paymentUrl", "http://pay"));
        controller.createPaymentUrl(bookingId, "DEPOSIT", customer);
        verify(paymentService).createVnpayPaymentUrl(bookingId, "DEPOSIT", customer);
    }

    @Test
    void vnpayReturn_success() throws Exception {
        when(vnPayConfig.getFrontendResultUrl()).thenReturn("http://fe/result");
        when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of("vnp_ResponseCode", "vnp_TxnRef")));
        when(request.getParameter("vnp_ResponseCode")).thenReturn("00");
        when(request.getParameter("vnp_TxnRef")).thenReturn("TXN");
        when(request.getParameter("vnp_TransactionNo")).thenReturn("1");
        when(request.getParameter("paymentId")).thenReturn(payment.getId().toString());
        when(paymentService.resolvePaymentIdForReturn(payment.getId().toString(), "TXN"))
                .thenReturn(payment.getId());
        when(vnPayService.verifySignature(anyMap())).thenReturn(true);
        when(paymentService.completeVnpayReturn(eq(payment.getId()), eq(true), eq("00"), eq("1")))
                .thenReturn(payment);

        controller.vnpayReturn(request, response);
        verify(response).sendRedirect("http://fe/result?status=success&bookingId=" + payment.getBooking().getId());
    }

    @Test
    void vnpayReturn_invalidSignature() throws Exception {
        when(vnPayConfig.getFrontendResultUrl()).thenReturn("http://fe/result");
        when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of("vnp_ResponseCode")));
        when(request.getParameter("vnp_ResponseCode")).thenReturn("00");
        when(request.getParameter("paymentId")).thenReturn(payment.getId().toString());
        when(paymentService.resolvePaymentIdForReturn(any(), any())).thenReturn(payment.getId());
        when(vnPayService.verifySignature(anyMap())).thenReturn(false);

        controller.vnpayReturn(request, response);
        verify(response).sendRedirect("http://fe/result?status=failed&message=Invalid_Signature");
    }

    @Test
    void vnpayReturn_missingPayment() throws Exception {
        when(vnPayConfig.getFrontendResultUrl()).thenReturn("http://fe/result");
        when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of()));
        when(paymentService.resolvePaymentIdForReturn(any(), any()))
                .thenThrow(new ResourceNotFoundException("missing"));
        controller.vnpayReturn(request, response);
        verify(response).sendRedirect("http://fe/result?status=failed&message=Missing_Payment_ID");
    }

    @Test
    void vnpayReturn_failedCode() throws Exception {
        when(vnPayConfig.getFrontendResultUrl()).thenReturn("http://fe/result");
        when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of("vnp_ResponseCode")));
        when(request.getParameter("vnp_ResponseCode")).thenReturn("24");
        when(request.getParameter("paymentId")).thenReturn(payment.getId().toString());
        when(paymentService.resolvePaymentIdForReturn(any(), any())).thenReturn(payment.getId());
        when(vnPayService.verifySignature(anyMap())).thenReturn(true);
        when(paymentService.completeVnpayReturn(any(), anyBoolean(), any(), any())).thenReturn(payment);

        controller.vnpayReturn(request, response);
        verify(response).sendRedirect("http://fe/result?status=failed&message=Payment_Failed");
    }
}
