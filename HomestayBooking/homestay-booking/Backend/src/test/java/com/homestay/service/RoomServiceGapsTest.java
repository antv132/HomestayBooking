package com.homestay.service;

import com.homestay.dto.request.CreateRoomRequest;
import com.homestay.dto.request.UpdateRoomRequest;
import com.homestay.dto.request.UpdateRoomStatusRequest;
import com.homestay.dto.response.AvailabilityResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.RoomDetailResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.Floor;
import com.homestay.entity.Property;
import com.homestay.entity.Review;
import com.homestay.entity.Room;
import com.homestay.entity.RoomStatusBlock;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceGapsTest {

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
    private User manager;
    private Property property;
    private Floor floor;
    private Room room;

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
        ReflectionTestUtils.setField(service, "uploadDir", "uploads");

        manager = TestFixtures.user(User.Role.MANAGER);
        property = TestFixtures.property();
        property.setStatus(Property.Status.ACTIVE);
        floor = TestFixtures.floor(property);
        room = TestFixtures.room(property);
        room.setFloor(floor);
        room.setStatus(Room.Status.AVAILABLE);
        room.setDeletedAt(null);
    }

    @Test
    void getPriceStats() {
        when(roomRepository.findMinPrice(Room.Status.AVAILABLE)).thenReturn(new BigDecimal("100"));
        when(roomRepository.findMaxPrice(Room.Status.AVAILABLE)).thenReturn(new BigDecimal("900"));
        Map<String, BigDecimal> stats = service.getPriceStats();
        assertEquals(0, stats.get("minPrice").compareTo(new BigDecimal("100")));
        assertEquals(0, stats.get("maxPrice").compareTo(new BigDecimal("900")));
    }

    @Test
    void update_duplicateRoomNumber() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.existsByPropertyIdAndRoomNumberAndDeletedAtIsNullAndIdNot(
                property.getId(), "202", room.getId())).thenReturn(true);

        UpdateRoomRequest req = new UpdateRoomRequest();
        req.setRoomNumber("202");
        assertThrows(ConflictException.class, () -> service.update(room.getId(), req));
    }

    @Test
    void updateForManager_success() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateRoomRequest req = new UpdateRoomRequest();
        req.setDescription("Updated description");
        req.setCapacity(3);

        RoomDetailResponse res = service.updateForManager(manager, room.getId(), req);
        assertEquals(3, res.getCapacity());
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
        verify(activityLogService).log(eq(manager), any(), eq("Room"), eq(room.getId()), any());
    }

    @Test
    void updateStatus_toAvailable_clearsBlocks() {
        room.setStatus(Room.Status.MAINTENANCE);
        RoomStatusBlock block = new RoomStatusBlock();
        block.setId(UUID.randomUUID());
        block.setRoom(room);
        block.setActive(true);
        block.setStatus(RoomStatusBlock.Status.MAINTENANCE);

        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(housekeepingTaskRepository.existsByRoom_IdAndStatusIn(eq(room.getId()), any()))
                .thenReturn(false);
        when(roomStatusBlockRepository.findByRoom_IdAndActiveTrue(room.getId()))
                .thenReturn(List.of(block));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roomStatusBlockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateRoomStatusRequest req = new UpdateRoomStatusRequest();
        req.setStatus("AVAILABLE");
        RoomDetailResponse res = service.updateStatus(room.getId(), req);

        assertEquals(Room.Status.AVAILABLE.name(), res.getStatus());
        assertFalse(block.isActive());
    }

    @Test
    void checkAvailability_available() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.findBookedDateRanges(room.getId())).thenReturn(List.of());
        when(roomStatusBlockRepository.existsActiveOverlappingStay(any(), any(), any())).thenReturn(false);
        when(roomRepository.existsOverlapBooking(any(), any(), any())).thenReturn(false);

        LocalDate in = LocalDate.now().plusDays(10);
        LocalDate out = in.plusDays(2);
        AvailabilityResponse res = service.checkAvailability(room.getId(), in, out);
        assertTrue(res.isAvailable());
    }

    @Test
    void checkAvailability_blockedByRoomStatus() {
        room.setStatus(Room.Status.MAINTENANCE);
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.findBookedDateRanges(room.getId())).thenReturn(List.of());

        LocalDate in = LocalDate.now().plusDays(10);
        AvailabilityResponse res = service.checkAvailability(room.getId(), in, in.plusDays(1));
        assertFalse(res.isAvailable());
    }

    @Test
    void getPublishedReviews() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        Review review = new Review();
        review.setId(UUID.randomUUID());
        review.setRoom(room);
        review.setCustomer(TestFixtures.user(User.Role.CUSTOMER));
        review.setRating(5);
        review.setComment("Great stay with friendly staff.");
        review.setStatus(Review.Status.PUBLISHED);
        when(reviewRepository.findByRoom_IdAndStatusOrderByCreatedAtDesc(
                eq(room.getId()), eq(Review.Status.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));

        PageResponse<RoomDetailResponse.ReviewInfo> page =
                service.getPublishedReviews(room.getId(), PageRequest.of(0, 10));
        assertEquals(1, page.getContent().size());
    }

    @Test
    void getAllForManagerScoped_emptyAssignments() {
        when(assignmentRepository.findActivePropertiesByManagerId(manager.getId()))
                .thenReturn(List.of());

        PageResponse<?> page = service.getAllForManagerScoped(
                manager, null, null, null, null, null, PageRequest.of(0, 10));
        assertEquals(0, page.getTotalElements());
    }

    @Test
    void getAllForManagerScoped_withPropertyFilter() {
        when(roomRepository.findWithFilters(
                isNull(), isNull(), eq(property.getId()), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(room)));

        PageResponse<?> page = service.getAllForManagerScoped(
                manager, null, null, property.getId().toString(), null, null, PageRequest.of(0, 10));
        assertEquals(1, page.getContent().size());
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void getByIdForManager() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        RoomDetailResponse res = service.getByIdForManager(manager, room.getId());
        assertEquals(room.getId(), res.getId());
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void listStatusBlocksForManager() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomStatusBlockRepository.findByRoom_IdAndActiveTrue(room.getId()))
                .thenReturn(List.of());
        assertTrue(service.listStatusBlocksForManager(manager, room.getId()).isEmpty());
    }

    @Test
    void getRoomBookings() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(bookingRepository.findByRoomIdOrderByCheckInDateDesc(eq(room.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        assertEquals(0, service.getRoomBookings(room.getId(), PageRequest.of(0, 10)).getContent().size());
    }

    @Test
    void getFeatured() {
        when(roomRepository.findByStatusAndActiveProperty(eq(Room.Status.AVAILABLE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(room)));
        assertEquals(1, service.getFeatured(5).size());
    }

    @Test
    void createForManager_success() {
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(floorRepository.findById(floor.getId())).thenReturn(Optional.of(floor));
        when(roomRepository.existsByPropertyIdAndRoomNumberAndDeletedAtIsNull(property.getId(), "301"))
                .thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
            Room r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        CreateRoomRequest req = new CreateRoomRequest();
        req.setPropertyId(property.getId().toString());
        req.setFloorId(floor.getId().toString());
        req.setRoomNumber("301");
        req.setRoomType("Deluxe");
        req.setPricePerNight(new BigDecimal("500000"));
        req.setCapacity(2);
        req.setAmenities(List.of("WiFi"));

        RoomDetailResponse res = service.createForManager(manager, req);
        assertEquals("301", res.getRoomNumber());
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
        verify(activityLogService).log(eq(manager), eq("ROOM_CREATED"), eq("Room"), any(), any());
    }

    @Test
    void deleteRoomForManager_blockedByBookings() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.hasBlockingBookings(room.getId())).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.deleteRoomForManager(manager, room.getId()));
    }

    @Test
    void deleteRoomForManager_success() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.hasBlockingBookings(room.getId())).thenReturn(false);
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.deleteRoomForManager(manager, room.getId());
        verify(activityLogService).log(eq(manager), eq("ROOM_DELETED"), eq("Room"), eq(room.getId()), any());
    }

    @Test
    void updateStatusForManager_maintenanceCreatesBlock() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.existsActiveBookingOverlapHalfOpen(any(), any(), any())).thenReturn(false);
        when(roomStatusBlockRepository.save(any())).thenAnswer(inv -> {
            RoomStatusBlock b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateRoomStatusRequest req = new UpdateRoomStatusRequest();
        req.setStatus("MAINTENANCE");
        req.setReason("AC repair scheduled");
        LocalDate start = LocalDate.now();
        req.setStartDate(start);
        req.setEndDate(start.plusDays(2));

        RoomDetailResponse res = service.updateStatusForManager(manager, room.getId(), req);
        assertEquals(Room.Status.MAINTENANCE.name(), res.getStatus());
        verify(activityLogService).log(eq(manager), eq("ROOM_STATUS_CHANGED"), eq("RoomStatusBlock"), any(), any());
    }

    @Test
    void getMonthAvailability_invalidRange() {
        assertThrows(BusinessException.class, () ->
                service.getMonthAvailability(room.getId(), LocalDate.now().plusDays(5), LocalDate.now()));
    }

    @Test
    void getMonthAvailability_buildsDays() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = start.plusDays(2);
        Object[] bookingRow = new Object[]{
                start, start.plusDays(2), Booking.Status.CONFIRMED.name()};
        when(roomRepository.findBlockingBookingsInRange(room.getId(), start, end))
                .thenReturn(List.<Object[]>of(bookingRow));

        RoomStatusBlock block = new RoomStatusBlock();
        block.setStatus(RoomStatusBlock.Status.MAINTENANCE);
        block.setStartDate(end);
        block.setEndDate(end);
        when(roomStatusBlockRepository.findActiveOverlappingRange(room.getId(), start, end))
                .thenReturn(List.of(block));

        var res = service.getMonthAvailability(room.getId(), start, end);
        assertEquals(3, res.getDays().size());
        assertFalse(res.getBookedDates().isEmpty());
        assertFalse(res.getMaintenanceDates().isEmpty());
    }

    @Test
    void getMonthAvailabilityForManager_delegates() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = start.plusDays(1);
        when(roomRepository.findBlockingBookingsInRange(room.getId(), start, end)).thenReturn(List.of());
        when(roomStatusBlockRepository.findActiveOverlappingRange(room.getId(), start, end))
                .thenReturn(List.of());
        when(roomRepository.findBlockingBookingsWithIdInRange(room.getId(), start, end))
                .thenReturn(List.of());

        var res = service.getMonthAvailabilityForManager(manager, room.getId(), start, end);
        assertEquals(room.getId(), res.getRoomId());
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void getAll_publicSearch() {
        when(roomRepository.findPublicWithFilters(
                any(), any(), any(), any(), any(),
                eq(Room.Status.AVAILABLE), eq(property.getId()), eq("Deluxe"),
                any(), any(), eq(2), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(room)));

        PageResponse<?> page = service.getAll(
                "da nang", null, "AVAILABLE", property.getId().toString(),
                "Deluxe", new BigDecimal("100"), new BigDecimal("900"),
                2, LocalDate.now().plusDays(5), LocalDate.now().plusDays(7),
                PageRequest.of(0, 12));
        assertEquals(1, page.getContent().size());
    }

    @Test
    void getAll_invalidStatusIgnored() {
        when(roomRepository.findPublicWithFilters(
                any(), any(), any(), any(), any(),
                eq(null), eq(null), eq(null),
                eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        assertEquals(0, service.getAll(null, null, "NOPE", null, null, null, null, null, null, null,
                PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void getPriceStats_defaultsWhenNull() {
        when(roomRepository.findMinPrice(Room.Status.AVAILABLE)).thenReturn(null);
        when(roomRepository.findMaxPrice(Room.Status.AVAILABLE)).thenReturn(null);
        Map<String, BigDecimal> stats = service.getPriceStats();
        assertEquals(0, stats.get("minPrice").compareTo(BigDecimal.ZERO));
        assertEquals(0, stats.get("maxPrice").compareTo(new BigDecimal("5000000")));
    }
}
