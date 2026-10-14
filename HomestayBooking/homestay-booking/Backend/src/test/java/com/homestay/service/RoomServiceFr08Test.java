package com.homestay.service;

import com.homestay.dto.request.CreateRoomRequest;
import com.homestay.dto.response.RoomDetailResponse;
import com.homestay.entity.Floor;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.ConflictException;
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
import com.homestay.util.UploadLimits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceFr08Test {

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
    private Property property;
    private Floor floor;
    private User manager;

    @BeforeEach
    void setUp() throws Exception {
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
        ReflectionTestUtils.setField(service, "uploadDir",
                Files.createTempDirectory("room-fr08-upload").toString() + "/");

        property = TestFixtures.property();
        property.setStatus(Property.Status.ACTIVE);
        floor = TestFixtures.floor(property);
        room = TestFixtures.room(property);
        room.setFloor(floor);
        room.setStatus(Room.Status.AVAILABLE);
        room.setDeletedAt(null);
        manager = TestFixtures.user(User.Role.MANAGER);
    }

    @Test
    void softDelete_setsDeletedAt_whenNoBlockingBookings() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.hasBlockingBookings(room.getId())).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deleteRoomForManager(manager, room.getId());

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        assertNotNull(captor.getValue().getDeletedAt());
        verify(roomRepository, never()).delete(any());
        verify(activityLogService).log(any(), org.mockito.ArgumentMatchers.eq("ROOM_DELETED"),
                org.mockito.ArgumentMatchers.eq("Room"), org.mockito.ArgumentMatchers.eq(room.getId()), any());
    }

    @Test
    void softDelete_conflict_whenBlockingBookings() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.hasBlockingBookings(room.getId())).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.deleteRoomForManager(manager, room.getId()));
        verify(roomRepository, never()).save(any());
    }

    @Test
    void softDelete_notFound_whenAlreadyDeleted() {
        room.setDeletedAt(java.time.LocalDateTime.now());
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

        assertThrows(ResourceNotFoundException.class,
                () -> service.deleteRoomForManager(manager, room.getId()));
    }

    @Test
    void create_rejectsDuplicateRoomNumber() {
        CreateRoomRequest req = new CreateRoomRequest();
        req.setPropertyId(property.getId().toString());
        req.setFloorId(floor.getId().toString());
        req.setRoomNumber("101");
        req.setRoomType("Deluxe");
        req.setPricePerNight(new BigDecimal("500000"));
        req.setCapacity(2);

        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(floorRepository.findById(floor.getId())).thenReturn(Optional.of(floor));
        when(roomRepository.existsByPropertyIdAndRoomNumberAndDeletedAtIsNull(property.getId(), "101"))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(req));
    }

    @Test
    void createForManager_defaultsAvailable_andLogs() {
        CreateRoomRequest req = new CreateRoomRequest();
        req.setPropertyId(property.getId().toString());
        req.setFloorId(floor.getId().toString());
        req.setRoomNumber("202");
        req.setRoomType("Standard");
        req.setPricePerNight(new BigDecimal("350000"));
        req.setCapacity(2);

        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(floorRepository.findById(floor.getId())).thenReturn(Optional.of(floor));
        when(roomRepository.existsByPropertyIdAndRoomNumberAndDeletedAtIsNull(property.getId(), "202"))
                .thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
            Room r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(UUID.randomUUID());
            }
            return r;
        });

        RoomDetailResponse created = service.createForManager(manager, req);

        assertEquals("AVAILABLE", created.getStatus());
        assertEquals("202", created.getRoomNumber());
        verify(activityLogService).log(any(), org.mockito.ArgumentMatchers.eq("ROOM_CREATED"),
                org.mockito.ArgumentMatchers.eq("Room"), any(), any());
    }

    @Test
    void uploadImages_rejectsWhenAtMaxCap() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomImageRepository.countByRoomId(room.getId())).thenReturn((long) UploadLimits.MAX_ROOM_IMAGES);

        MultipartFile file = new MockMultipartFile(
                "file", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});

        assertThrows(com.homestay.exception.BusinessException.class,
                () -> service.uploadImages(room.getId(), List.of(file), false));
    }

    @Test
    void uploadImages_rejectsInvalidMime() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomImageRepository.countByRoomId(room.getId())).thenReturn(0L);

        MultipartFile file = new MockMultipartFile(
                "file", "a.gif", "image/gif", new byte[]{1, 2, 3});

        assertThrows(com.homestay.exception.BusinessException.class,
                () -> service.uploadImages(room.getId(), List.of(file), false));
    }
}
