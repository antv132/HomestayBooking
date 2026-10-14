package com.homestay.service;

import com.homestay.entity.Booking;
import com.homestay.entity.DamageReport;
import com.homestay.entity.Payment;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.RoomInspection;
import com.homestay.entity.User;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.DamageReportRepository;
import com.homestay.repository.PaymentRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DamageFeeSettlementServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock DamageReportRepository damageReportRepository;

    private DamageFeeSettlementService service;
    private Booking booking;
    private DamageReport report;

    @BeforeEach
    void setUp() {
        service = new DamageFeeSettlementService(bookingRepository, paymentRepository, damageReportRepository);
        User customer = TestFixtures.user(User.Role.CUSTOMER);
        Property property = TestFixtures.property();
        Room room = TestFixtures.room(property);
        booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_INSPECTION);
        RoomInspection inspection = TestFixtures.inspection(booking, property, room);
        report = TestFixtures.damageReport(booking, inspection, new BigDecimal("750000"), false);
        report.setStatus(DamageReport.Status.APPROVED);
        report.setBooking(booking);
    }

    @Test
    void applyApprovedFee_createsVnpayPendingDamagePayment() {
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId())).thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.applyApprovedFee(report, new BigDecimal("750000"));

        assertEquals(Booking.Status.PENDING_DAMAGE_PAYMENT, booking.getStatus());
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();
        assertEquals(Payment.Type.DAMAGE_FEE, saved.getType());
        assertEquals(Payment.Method.VNPAY, saved.getMethod());
        assertEquals(Payment.Status.PENDING, saved.getStatus());
        assertEquals(0, new BigDecimal("750000").compareTo(saved.getAmount()));
    }

    @Test
    void markDamageReportPaidForBooking_unlocksCheckoutPath() {
        booking.setStatus(Booking.Status.PENDING_DAMAGE_PAYMENT);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(damageReportRepository.findFirstByBooking_IdAndStatus(booking.getId(), DamageReport.Status.APPROVED))
                .thenReturn(Optional.of(report));
        when(damageReportRepository.save(any(DamageReport.class))).thenAnswer(inv -> inv.getArgument(0));

        service.markDamageReportPaidForBooking(booking.getId());

        assertEquals(Booking.Status.PENDING_INSPECTION, booking.getStatus());
        assertEquals(DamageReport.Status.PAID, report.getStatus());
        verify(bookingRepository).save(booking);
        verify(damageReportRepository).save(report);
    }

    @Test
    void applyApprovedFee_zeroAmount_throws() {
        assertThrows(com.homestay.exception.BusinessException.class,
                () -> service.applyApprovedFee(report, BigDecimal.ZERO));
    }

    @Test
    void applyApprovedFee_missingBooking_throws() {
        report.setBooking(null);
        assertThrows(com.homestay.exception.BusinessException.class,
                () -> service.applyApprovedFee(report, new BigDecimal("100000")));
    }

    @Test
    void applyApprovedFee_existingPendingDamage_skipsNewPayment() {
        Payment existing = TestFixtures.payment(booking, Payment.Type.DAMAGE_FEE, Payment.Status.PENDING);
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(existing));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        service.applyApprovedFee(report, new BigDecimal("750000"));

        verify(paymentRepository, never()).save(any(Payment.class));
        assertEquals(Booking.Status.PENDING_DAMAGE_PAYMENT, booking.getStatus());
    }

    @Test
    void markDamageReportPaidForBooking_bookingMissing_throws() {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.empty());
        assertThrows(com.homestay.exception.BusinessException.class,
                () -> service.markDamageReportPaidForBooking(booking.getId()));
    }

    @Test
    void markDamageReportPaidForBooking_notPendingDamage_skipsStatusChange() {
        booking.setStatus(Booking.Status.CHECKED_IN);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(damageReportRepository.findFirstByBooking_IdAndStatus(booking.getId(), DamageReport.Status.APPROVED))
                .thenReturn(Optional.empty());

        service.markDamageReportPaidForBooking(booking.getId());

        assertEquals(Booking.Status.CHECKED_IN, booking.getStatus());
        verify(bookingRepository, never()).save(any());
    }
}
