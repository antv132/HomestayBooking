package com.homestay.service;

import com.homestay.dto.request.UpdateMaintenanceStatusRequest;
import com.homestay.entity.Booking;
import com.homestay.entity.MaintenanceTicket;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.MaintenanceTicketRepository;
import com.homestay.repository.RoomRepository;
import com.homestay.repository.UserRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceTicketServiceTest {

    @Mock MaintenanceTicketRepository ticketRepository;
    @Mock BookingRepository bookingRepository;
    @Mock UserRepository userRepository;
    @Mock RoomRepository roomRepository;
    @Mock ReportPropertyScopeValidator scopeValidator;
    @Mock ActivityLogService activityLogService;
    @Mock MaintenanceTaskManagerService maintenanceTaskManagerService;

    private MaintenanceTicketService service;

    private User customer;
    private User manager;
    private Property property;
    private Room room;
    private Booking booking;
    private MaintenanceTicket ticket;

    @BeforeEach
    void setUp() {
        service = new MaintenanceTicketService(
                ticketRepository,
                bookingRepository,
                userRepository,
                roomRepository,
                scopeValidator,
                activityLogService,
                maintenanceTaskManagerService);

        customer = TestFixtures.user(User.Role.CUSTOMER);
        manager = TestFixtures.user(User.Role.MANAGER);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        booking = TestFixtures.booking(customer, room, Booking.Status.CHECKED_IN);

        ticket = new MaintenanceTicket();
        ticket.setId(UUID.randomUUID());
        ticket.setCustomer(customer);
        ticket.setRoom(room);
        ticket.setBooking(booking);
        ticket.setTitle("Broken AC unit");
        ticket.setDescription("Air conditioner not cooling the room properly.");
        ticket.setStatus(MaintenanceTicket.Status.OPEN);
        ticket.setCreatedAt(LocalDateTime.now().minusHours(1));
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket.setPhotoUrls("[\"/uploads/tickets/a.jpg\"]");
    }

    @Test
    void createFromBooking_requiresConfirmedOrCheckedIn() {
        booking.setStatus(Booking.Status.PENDING_DEPOSIT);
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(bookingRepository.findByIdWithRoomAndCustomer(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(BusinessException.class, () ->
                service.createTicketFromBooking(
                        customer.getId(),
                        booking.getId(),
                        "Broken AC unit",
                        "Air conditioner not cooling the room properly.",
                        null));
    }

    @Test
    void createFromBooking_rejectsOtherCustomer() {
        User other = TestFixtures.user(User.Role.CUSTOMER);
        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(bookingRepository.findByIdWithRoomAndCustomer(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(BusinessException.class, () ->
                service.createTicketFromBooking(
                        other.getId(),
                        booking.getId(),
                        "Broken AC unit",
                        "Air conditioner not cooling the room properly.",
                        null));
    }

    @Test
    void createFromBooking_rejectsShortDescription() {
        assertThrows(BusinessException.class, () ->
                service.createTicketFromBooking(
                        customer.getId(),
                        booking.getId(),
                        "Broken AC",
                        "too short",
                        null));
    }

    @Test
    void createFromBooking_opensTicket() {
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(bookingRepository.findByIdWithRoomAndCustomer(booking.getId())).thenReturn(Optional.of(booking));
        when(ticketRepository.saveAndFlush(any(MaintenanceTicket.class))).thenAnswer(inv -> {
            MaintenanceTicket t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            t.setCreatedAt(LocalDateTime.now());
            t.setUpdatedAt(LocalDateTime.now());
            return t;
        });
        when(ticketRepository.findActiveById(any())).thenAnswer(inv -> Optional.of(ticket));

        Map<String, Object> result = service.createTicketFromBooking(
                customer.getId(),
                booking.getId(),
                "Broken AC unit",
                "Air conditioner not cooling the room properly.",
                null);

        assertEquals(MaintenanceTicket.Status.OPEN.name(), result.get("status"));
        assertNull(result.get("workNote"));
        verify(activityLogService).log(eq(customer), eq(ActivityLogService.TICKET_CREATED),
                eq("MaintenanceTicket"), any(), any());
    }

    @Test
    void createFromForm_requiresActiveBookingForRoom() {
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(bookingRepository.findByCustomerOrderByCreatedAtDesc(customer)).thenReturn(List.of());

        assertThrows(BusinessException.class, () ->
                service.createTicketFromForm(
                        customer.getId(),
                        room.getId(),
                        "Broken AC unit",
                        "Air conditioner not cooling the room properly.",
                        null));
    }

    @Test
    void deleteTicket_onlyWhenOpen() {
        ticket.setStatus(MaintenanceTicket.Status.ASSIGNED);
        when(ticketRepository.findActiveById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(BusinessException.class, () ->
                service.deleteTicket(customer.getId(), ticket.getId()));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void deleteTicket_softDeletesOpen() {
        when(ticketRepository.findActiveById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(MaintenanceTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deleteTicket(customer.getId(), ticket.getId());

        assertNotNull(ticket.getDeletedAt());
        verify(activityLogService).log(eq(customer), eq(ActivityLogService.TICKET_DELETED),
                eq("MaintenanceTicket"), eq(ticket.getId()), any());
    }

    @Test
    void updateTicketContent_blockedWhenNotOpen() {
        ticket.setStatus(MaintenanceTicket.Status.IN_PROGRESS);
        when(ticketRepository.findActiveById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(BusinessException.class, () ->
                service.updateTicketContent(
                        customer.getId(),
                        ticket.getId(),
                        "New title",
                        "Updated description with enough characters.",
                        List.of(),
                        null));
    }

    @Test
    void getTicketDetail_hidesResolutionUntilClosed_forCustomer() {
        ticket.setResolutionNote("Fixed compressor");
        ticket.setWorkNote("internal");
        when(ticketRepository.findActiveById(ticket.getId())).thenReturn(Optional.of(ticket));

        Map<String, Object> map = service.getTicketDetail(customer, ticket.getId());

        assertNull(map.get("resolutionNote"));
        assertNull(map.get("workNote"));
    }

    @Test
    void getTicketDetail_managerSeesWorkNote() {
        ticket.setResolutionNote("Fixed compressor");
        ticket.setWorkNote("internal");
        when(ticketRepository.findActiveById(ticket.getId())).thenReturn(Optional.of(ticket));

        Map<String, Object> map = service.getTicketDetail(manager, ticket.getId());

        assertEquals("Fixed compressor", map.get("resolutionNote"));
        assertEquals("internal", map.get("workNote"));
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void getAllTicketsPaged_legacyBlocked() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getAllTicketsPaged("OPEN", 0, 10));
        assertTrue(ex.getMessage().contains("propertyId"));
    }

    @Test
    void updateTicketStatusAndReturn_onlyClosedAllowed() {
        UpdateMaintenanceStatusRequest req = new UpdateMaintenanceStatusRequest();
        req.setStatus(MaintenanceTicket.Status.RESOLVED);

        assertThrows(BusinessException.class, () ->
                service.updateTicketStatusAndReturn(manager, ticket.getId(), req));
        verify(maintenanceTaskManagerService, never()).updateStatusForManager(any(), any(), any());
    }

    @Test
    void getActiveBookingsForCustomer_mapsConfirmedAndCheckedIn() {
        when(userRepository.existsById(customer.getId())).thenReturn(true);
        when(bookingRepository.findActiveWithRoomPropertyByCustomerId(
                eq(customer.getId()), any())).thenReturn(List.of(booking));

        List<Map<String, Object>> rows = service.getActiveBookingsForCustomer(customer.getId());

        assertEquals(1, rows.size());
        assertEquals(booking.getId().toString(), rows.get(0).get("bookingId"));
        assertEquals(Booking.Status.CHECKED_IN.name(), rows.get(0).get("status"));
    }

    @Test
    void listForAdmin_returnsPagedMaps() {
        when(ticketRepository.findAllActive(eq(MaintenanceTicket.Status.OPEN), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket)));

        Map<String, Object> page = service.listForAdmin("OPEN", 0, 10);

        assertEquals(1L, page.get("totalElements"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertEquals(1, content.size());
        assertTrue(content.get(0).containsKey("workNote"));
    }

    @Test
    void getCustomerTicketsPaged_filtersAll() {
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(ticketRepository.findByCustomerPaged(eq(customer), eq(null), any(Pageable.class)))
                .thenReturn(Page.empty());

        Map<String, Object> page = service.getCustomerTicketsPaged(customer.getId(), "ALL", 0, 10);

        assertEquals(0L, page.get("totalElements"));
    }

    @Test
    void getCustomerTicketsPaged_missingCustomer() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                service.getCustomerTicketsPaged(UUID.randomUUID(), null, 0, 10));
    }

    @Test
    void updateTicketStatusAndReturn_closesViaManagerService() {
        UpdateMaintenanceStatusRequest req = new UpdateMaintenanceStatusRequest();
        req.setStatus(MaintenanceTicket.Status.CLOSED);
        req.setResolutionNote("Verified fix and customer confirmed.");
        when(ticketRepository.findActiveById(ticket.getId())).thenReturn(Optional.of(ticket));

        Map<String, Object> map = service.updateTicketStatusAndReturn(manager, ticket.getId(), req);
        assertNotNull(map.get("status"));
        verify(maintenanceTaskManagerService).updateStatusForManager(manager, ticket.getId(), req);
    }

    @Test
    void updateTicketContent_openSuccess() {
        when(ticketRepository.findActiveById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ticketRepository.findActiveById(ticket.getId())).thenReturn(Optional.of(ticket));

        Map<String, Object> map = service.updateTicketContent(
                customer.getId(),
                ticket.getId(),
                "Updated AC title",
                "Updated description with enough characters for validation.",
                List.of(),
                null);
        assertEquals(MaintenanceTicket.Status.OPEN.name(), map.get("status"));
    }

    @Test
    void createFromForm_success() {
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(bookingRepository.findByCustomerOrderByCreatedAtDesc(customer)).thenReturn(List.of(booking));
        when(ticketRepository.saveAndFlush(any(MaintenanceTicket.class))).thenAnswer(inv -> {
            MaintenanceTicket t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            t.setCreatedAt(LocalDateTime.now());
            t.setUpdatedAt(LocalDateTime.now());
            return t;
        });
        when(ticketRepository.findActiveById(any())).thenReturn(Optional.of(ticket));

        Map<String, Object> result = service.createTicketFromForm(
                customer.getId(),
                room.getId(),
                "Broken AC unit",
                "Air conditioner not cooling the room properly.",
                null);
        assertEquals(MaintenanceTicket.Status.OPEN.name(), result.get("status"));
    }
}
