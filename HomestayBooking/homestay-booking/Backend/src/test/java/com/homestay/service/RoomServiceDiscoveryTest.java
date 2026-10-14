package com.homestay.service;

import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.RoomDetailResponse;
import com.homestay.dto.response.RoomSummaryResponse;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceDiscoveryTest {

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
    private Property activeProperty;
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
        ReflectionTestUtils.setField(service, "uploadDir", "uploads/");
        activeProperty = TestFixtures.property();
        activeProperty.setStatus(Property.Status.ACTIVE);
        room = TestFixtures.room(activeProperty);
        room.setStatus(Room.Status.AVAILABLE);
        room.setRoomType("Deluxe");
        room.setPricePerNight(new java.math.BigDecimal("500000"));
        room.setCapacity(2);
        room.setArea(new java.math.BigDecimal("25"));
        room.setDescription("Nice room");
        // Floor required by RoomDetailResponse.fromEntity
        com.homestay.entity.Floor floor = new com.homestay.entity.Floor();
        floor.setId(java.util.UUID.randomUUID());
        floor.setFloorNumber(1);
        room.setFloor(floor);
    }

    @Test
    void getById_inactiveProperty_throwsNotFound() {
        activeProperty.setStatus(Property.Status.INACTIVE);
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

        assertThrows(ResourceNotFoundException.class, () -> service.getById(room.getId()));
    }

    @Test
    void getById_activeProperty_returnsDetail() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

        RoomDetailResponse detail = service.getById(room.getId());
        assertEquals(room.getId(), detail.getId());
        assertEquals(activeProperty.getName(), detail.getPropertyName());
    }

    @Test
    void getAll_invalidDateRange_throwsBusinessException() {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(3);

        assertThrows(BusinessException.class, () ->
                service.getAll(null, null, "AVAILABLE", null, null, null, null, 2,
                        checkIn, checkOut, PageRequest.of(0, 12)));
    }

    @Test
    void getAll_pastCheckIn_throwsBusinessException() {
        LocalDate checkIn = LocalDate.now().minusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(2);

        assertThrows(BusinessException.class, () ->
                service.getAll(null, null, "AVAILABLE", null, null, null, null, null,
                        checkIn, checkOut, PageRequest.of(0, 12)));
    }

    @Test
    void getFeatured_usesActivePropertyQuery() {
        Page<Room> page = new PageImpl<>(List.of(room));
        when(roomRepository.findByStatusAndActiveProperty(eq(Room.Status.AVAILABLE), any()))
                .thenReturn(page);

        List<RoomSummaryResponse> featured = service.getFeatured(8);
        assertEquals(1, featured.size());
        verify(roomRepository).findByStatusAndActiveProperty(eq(Room.Status.AVAILABLE), any());
    }

    @Test
    void getAll_validDates_delegatesToRepository() {
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(5);
        when(roomRepository.findPublicWithFilters(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(Room.Status.AVAILABLE), isNull(), isNull(),
                isNull(), isNull(), isNull(),
                eq(checkIn), eq(checkOut), any()))
                .thenReturn(Page.empty());

        PageResponse<RoomSummaryResponse> result = service.getAll(
                null, null, "AVAILABLE", null, null, null, null, null,
                checkIn, checkOut, PageRequest.of(0, 12));

        assertEquals(0, result.getTotalElements());
    }
}
