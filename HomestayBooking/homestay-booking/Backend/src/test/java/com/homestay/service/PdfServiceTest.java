package com.homestay.service;

import com.homestay.entity.Booking;
import com.homestay.entity.Contract;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfServiceTest {

    @Test
    void generateContractPdf_returnsPdfBytes() {
        User customer = TestFixtures.user(User.Role.CUSTOMER);
        customer.setFullName(null);
        Property property = TestFixtures.property();
        Room room = TestFixtures.room(property);
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.CONFIRMED);

        Contract contract = new Contract();
        contract.setId(UUID.randomUUID());
        contract.setBooking(booking);
        contract.setCustomer(customer);
        contract.setRoom(room);
        contract.setCheckInDate(booking.getCheckInDate());
        contract.setCheckOutDate(booking.getCheckOutDate());
        contract.setDepositAmount(new BigDecimal("4000000"));
        contract.setTotalAmount(new BigDecimal("10000000"));
        contract.setCreatedAt(LocalDateTime.of(2026, 7, 1, 10, 0));

        byte[] pdf = new PdfService().generateContractPdf(contract);
        assertTrue(pdf.length > 100);
        assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F');
    }
}
