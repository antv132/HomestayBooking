package com.homestay.service;

import com.homestay.dto.response.RoomDetailResponse;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.RoomImage;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
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
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceImageTest {

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

    @TempDir Path tempDir;

    private RoomService service;
    private User manager;
    private Property property;
    private Room room;

    @BeforeEach
    void setUp() {
        service = new RoomService(
                roomRepository, propertyRepository, floorRepository, roomImageRepository,
                bookingRepository, reviewRepository, scopeValidator, assignmentRepository,
                roomStatusBlockRepository, housekeepingTaskRepository, activityLogService);
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString().replace('\\', '/') + "/");
        manager = TestFixtures.user(User.Role.MANAGER);
        property = TestFixtures.property();
        property.setStatus(Property.Status.ACTIVE);
        room = TestFixtures.room(property);
        room.setDeletedAt(null);
    }

    @Test
    void uploadImages_emptyRejected() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        assertThrows(BusinessException.class, () -> service.uploadImages(room.getId(), List.of(), false));
    }

    @Test
    void uploadImages_successAsPrimary() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomImageRepository.countByRoomId(room.getId())).thenReturn(0L);
        when(roomImageRepository.findByRoomIdOrderBySortOrderAsc(room.getId())).thenReturn(List.of());
        when(roomImageRepository.save(any(RoomImage.class))).thenAnswer(inv -> {
            RoomImage img = inv.getArgument(0);
            img.setId(UUID.randomUUID());
            return img;
        });

        MockMultipartFile file = new MockMultipartFile(
                "file", "room.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});
        List<RoomDetailResponse.RoomImageInfo> infos =
                service.uploadImagesForManager(manager, room.getId(), List.of(file), true);
        assertEquals(1, infos.size());
        assertTrue(infos.get(0).getIsPrimary());
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void reorderImages_ok() {
        RoomImage a = new RoomImage();
        a.setId(UUID.randomUUID());
        a.setSortOrder(0);
        RoomImage b = new RoomImage();
        b.setId(UUID.randomUUID());
        b.setSortOrder(1);
        when(roomImageRepository.findByRoomIdOrderBySortOrderAsc(room.getId()))
                .thenReturn(List.of(a, b), List.of(b, a));
        when(roomImageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<RoomDetailResponse.RoomImageInfo> result =
                service.reorderImages(room.getId(), List.of(b.getId(), a.getId()));
        assertEquals(2, result.size());
        assertEquals(0, b.getSortOrder());
        assertEquals(1, a.getSortOrder());
    }

    @Test
    void reorderImages_invalidList() {
        when(roomImageRepository.findByRoomIdOrderBySortOrderAsc(room.getId())).thenReturn(List.of());
        assertThrows(BusinessException.class, () ->
                service.reorderImages(room.getId(), List.of(UUID.randomUUID())));
    }

    @Test
    void deleteImage_promotesNextPrimary() {
        RoomImage primary = new RoomImage();
        primary.setId(UUID.randomUUID());
        primary.setIsPrimary(true);
        primary.setRoom(room);
        primary.setImageUrl("/uploads/rooms/" + room.getId() + "/gone.jpg");

        RoomImage next = new RoomImage();
        next.setId(UUID.randomUUID());
        next.setIsPrimary(false);
        next.setRoom(room);

        when(roomImageRepository.findById(primary.getId())).thenReturn(Optional.of(primary));
        when(roomImageRepository.findByRoomIdOrderBySortOrderAsc(room.getId())).thenReturn(List.of(next));
        when(roomImageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deleteImage(primary.getId());
        assertTrue(next.getIsPrimary());
        verify(roomImageRepository).delete(primary);
    }
}
