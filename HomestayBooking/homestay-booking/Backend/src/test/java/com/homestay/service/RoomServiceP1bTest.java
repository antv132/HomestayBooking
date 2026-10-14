package com.homestay.service;

import com.homestay.dto.request.UpdateRoomStatusRequest;
import com.homestay.dto.response.MonthAvailabilityResponse;
import com.homestay.dto.response.StaffRoomMonthCalendarResponse;
import com.homestay.entity.Floor;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.RoomStatusBlock;
import com.homestay.entity.User;
import com.homestay.exception.ConflictException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.FloorRepository;
import com.homestay.repository.HousekeepingTaskRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PropertyRepository;
import com.homestay.repository.ReviewRepository;
import com.homestay.repository.RoomImageRepository;
import com.homestay.repository.RoomRepository;
import com.homestay.repository.RoomStatusBlockRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceP1bTest {

    @Mock RoomRepository roomRepository;
    @Mock PropertyRepository propertyRepository;
    @Mock FloorRepository floorRepository;
    @Mock RoomImageRepository roomImageRepository;
    @Mock BookingRepository bookingRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock ReportPropertyScopeValidator scopeValidator;
    @Mock ManagerPropertyAssignmentRepository assignmentRepository;
    @Mock RoomStatusBlockRepository roomStatusBlockRepository;
    @Mock HousekeepingTaskRepository housekeepingTaskRepository;
    @Mock ActivityLogService activityLogService;

    private RoomService service;
    private Room room;
    private User manager;

    @BeforeEach
    void setUp() {
        service = new RoomService(
                roomRepository,
                propertyRepository,
                floorRepository,
                roomImageRepository,
                bookingRepository,
                reviewRepository,
                scopeValidator,
                assignmentRepository,
                roomStatusBlockRepository,
                housekeepingTaskRepository,
                activityLogService);
        ReflectionTestUtils.setField(service, "uploadDir", "uploads/");
        Property property = TestFixtures.property();
        property.setStatus(Property.Status.ACTIVE);
        room = TestFixtures.room(property);
        room.setStatus(Room.Status.AVAILABLE);
        manager = TestFixtures.user(User.Role.MANAGER);
    }

    @Test
    void getMonthAvailability_halfOpen_excludesCheckoutDay() {
        LocalDate checkIn = LocalDate.of(2026, 8, 10);
        LocalDate checkOut = LocalDate.of(2026, 8, 12);
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.findBlockingBookingsInRange(eq(room.getId()), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{checkIn, checkOut, "CONFIRMED"}));
        when(roomStatusBlockRepository.findActiveOverlappingRange(eq(room.getId()), any(), any()))
                .thenReturn(List.of());

        MonthAvailabilityResponse resp = service.getMonthAvailability(
                room.getId(), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertTrue(resp.getBookedDates().contains("2026-08-10"));
        assertTrue(resp.getBookedDates().contains("2026-08-11"));
        assertFalse(resp.getBookedDates().contains("2026-08-12"));

        String status12 = resp.getDays().stream()
                .filter(d -> d.getDate().equals("2026-08-12"))
                .findFirst()
                .orElseThrow()
                .getStatus();
        assertEquals("AVAILABLE", status12);
    }

    @Test
    void getMonthAvailabilityForManager_reusesDaysAndAddsBookingRefs() {
        LocalDate checkIn = LocalDate.of(2026, 8, 10);
        LocalDate checkOut = LocalDate.of(2026, 8, 12);
        java.util.UUID bookingId = java.util.UUID.randomUUID();
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.findBlockingBookingsInRange(eq(room.getId()), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{checkIn, checkOut, "CONFIRMED"}));
        when(roomRepository.findBlockingBookingsWithIdInRange(eq(room.getId()), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{bookingId, checkIn, checkOut, "CONFIRMED"}));
        when(roomStatusBlockRepository.findActiveOverlappingRange(eq(room.getId()), any(), any()))
                .thenReturn(List.of());

        StaffRoomMonthCalendarResponse resp = service.getMonthAvailabilityForManager(
                manager, room.getId(), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        verify(scopeValidator).validateManagerAccess(eq(manager), eq(room.getProperty().getId()));
        assertEquals(room.getId(), resp.getRoomId());
        assertFalse(resp.getDays().isEmpty());
        assertEquals(1, resp.getBookedRanges().size());
        assertEquals(bookingId, resp.getBookedRanges().get(0).getBookingId());
        assertEquals("RESERVED", resp.getDays().stream()
                .filter(d -> d.getDate().equals("2026-08-10")).findFirst().orElseThrow().getStatus());
    }

    @Test
    void getMonthAvailability_blockOnlyPaintsRangeNotWholeMonth() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.findBlockingBookingsInRange(eq(room.getId()), any(), any()))
                .thenReturn(List.of());
        RoomStatusBlock block = new RoomStatusBlock();
        block.setRoom(room);
        block.setProperty(room.getProperty());
        block.setStatus(RoomStatusBlock.Status.MAINTENANCE);
        block.setStartDate(LocalDate.of(2026, 8, 5));
        block.setEndDate(LocalDate.of(2026, 8, 6));
        block.setActive(true);
        when(roomStatusBlockRepository.findActiveOverlappingRange(eq(room.getId()), any(), any()))
                .thenReturn(List.of(block));

        MonthAvailabilityResponse resp = service.getMonthAvailability(
                room.getId(), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertTrue(resp.getMaintenanceDates().contains("2026-08-05"));
        assertTrue(resp.getMaintenanceDates().contains("2026-08-06"));
        assertFalse(resp.getMaintenanceDates().contains("2026-08-07"));
        assertEquals("AVAILABLE", resp.getDays().stream()
                .filter(d -> d.getDate().equals("2026-08-01")).findFirst().orElseThrow().getStatus());
        assertEquals("MAINTENANCE", resp.getDays().stream()
                .filter(d -> d.getDate().equals("2026-08-05")).findFirst().orElseThrow().getStatus());
    }

    @Test
    void updateStatus_availableRejectedWhenHkOpen() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(housekeepingTaskRepository.existsByRoom_IdAndStatusIn(
                eq(room.getId()), any())).thenReturn(true);

        UpdateRoomStatusRequest req = new UpdateRoomStatusRequest();
        req.setStatus("AVAILABLE");

        assertThrows(ConflictException.class,
                () -> service.updateStatusForManager(manager, room.getId(), req));
        verify(roomStatusBlockRepository, never()).save(any());
    }

    @Test
    void updateStatus_maintenanceRejectedOnBookingOverlap() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.existsActiveBookingOverlapHalfOpen(
                eq(room.getId()), any(), any())).thenReturn(true);

        UpdateRoomStatusRequest req = new UpdateRoomStatusRequest();
        req.setStatus("MAINTENANCE");
        req.setReason("Plumbing");
        req.setStartDate(LocalDate.of(2026, 8, 1));
        req.setEndDate(LocalDate.of(2026, 8, 5));

        assertThrows(ConflictException.class,
                () -> service.updateStatusForManager(manager, room.getId(), req));
        verify(roomStatusBlockRepository, never()).save(any());
    }

    @Test
    void updateStatus_maintenancePersistsBlock() {
        Floor floor = new Floor();
        floor.setId(java.util.UUID.randomUUID());
        floor.setFloorNumber(1);
        floor.setProperty(room.getProperty());
        room.setFloor(floor);
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.existsActiveBookingOverlapHalfOpen(
                eq(room.getId()), any(), any())).thenReturn(false);
        when(roomStatusBlockRepository.save(any(RoomStatusBlock.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateRoomStatusRequest req = new UpdateRoomStatusRequest();
        req.setStatus("MAINTENANCE");
        req.setReason("Plumbing");
        req.setStartDate(LocalDate.of(2026, 8, 1));
        req.setEndDate(LocalDate.of(2026, 8, 5));

        service.updateStatusForManager(manager, room.getId(), req);

        ArgumentCaptor<RoomStatusBlock> captor = ArgumentCaptor.forClass(RoomStatusBlock.class);
        verify(roomStatusBlockRepository).save(captor.capture());
        assertEquals(RoomStatusBlock.Status.MAINTENANCE, captor.getValue().getStatus());
        assertEquals(LocalDate.of(2026, 8, 1), captor.getValue().getStartDate());
        assertTrue(captor.getValue().isActive());
    }

    @Test
    void checkAvailability_statusBlockOverlap_notAvailable() {
        room.getProperty().setStatus(Property.Status.ACTIVE);
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.findBookedDateRanges(room.getId())).thenReturn(List.of());
        when(roomStatusBlockRepository.existsActiveOverlappingStay(
                eq(room.getId()), any(), any())).thenReturn(true);

        var resp = service.checkAvailability(
                room.getId(), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3));

        assertFalse(resp.isAvailable());
        verify(roomRepository, never()).existsOverlapBooking(any(), any(), any());
    }
}
