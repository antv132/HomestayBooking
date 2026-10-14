package com.homestay.support;

import com.homestay.entity.Booking;
import com.homestay.entity.DamageReport;
import com.homestay.entity.Floor;
import com.homestay.entity.Payment;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.RoomInspection;
import com.homestay.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

public final class TestFixtures {

    private TestFixtures() {}

    public static User user(User.Role role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setFullName(role.name() + " User");
        u.setEmail(role.name().toLowerCase() + "@test.local");
        u.setRole(role);
        u.setStatus(User.Status.ACTIVE);
        return u;
    }

    public static Property property() {
        Property p = new Property();
        p.setId(UUID.randomUUID());
        p.setName("Test Homestay");
        return p;
    }

    public static Floor floor(Property property) {
        Floor f = new Floor();
        f.setId(UUID.randomUUID());
        f.setProperty(property);
        f.setFloorNumber(1);
        return f;
    }

    public static Room room(Property property) {
        Room r = new Room();
        r.setId(UUID.randomUUID());
        r.setRoomNumber("101");
        r.setProperty(property);
        r.setFloor(floor(property));
        r.setStatus(Room.Status.RESERVED);
        return r;
    }

    public static Booking booking(User customer, Room room, Booking.Status status) {
        Booking b = new Booking();
        b.setId(UUID.randomUUID());
        b.setCustomer(customer);
        b.setRoom(room);
        b.setStatus(status);
        b.setCheckInDate(LocalDate.now());
        b.setCheckOutDate(LocalDate.now().plusDays(2));
        b.setTotalAmount(new BigDecimal("10000000"));
        b.setDepositAmount(new BigDecimal("4000000"));
        b.setRemainingAmount(new BigDecimal("6000000"));
        return b;
    }

    public static Payment payment(Booking booking, Payment.Type type, Payment.Status status) {
        Payment p = new Payment();
        p.setId(UUID.randomUUID());
        p.setBooking(booking);
        p.setCustomer(booking.getCustomer());
        p.setType(type);
        p.setStatus(status);
        p.setMethod(Payment.Method.VNPAY);
        p.setAmount(type == Payment.Type.DEPOSIT
                ? booking.getDepositAmount()
                : type == Payment.Type.DAMAGE_FEE
                ? (booking.getDamageFeeAmount() != null ? booking.getDamageFeeAmount() : BigDecimal.ZERO)
                : booking.getRemainingAmount());
        return p;
    }

    public static RoomInspection inspection(Booking booking, Property property, Room room) {
        RoomInspection ri = new RoomInspection();
        ri.setId(UUID.randomUUID());
        ri.setBooking(booking);
        ri.setProperty(property);
        ri.setRoom(room);
        ri.setStatus(RoomInspection.Status.FAILED_WITH_DAMAGE);
        return ri;
    }

    public static DamageReport damageReport(
            Booking booking,
            RoomInspection inspection,
            BigDecimal total,
            boolean escalate) {
        DamageReport dr = new DamageReport();
        dr.setId(UUID.randomUUID());
        dr.setBooking(booking);
        dr.setInspection(inspection);
        dr.setTotalEstimatedCost(total);
        dr.setRequiresAdminEscalation(escalate);
        dr.setStatus(DamageReport.Status.PENDING_APPROVAL);
        dr.setItems(new ArrayList<>());
        return dr;
    }
}
