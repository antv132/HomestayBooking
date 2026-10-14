package com.homestay.service;

import com.homestay.dto.response.ContractDetailResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.Contract;
import com.homestay.entity.Notification;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ForbiddenException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.ContractRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock ContractRepository contractRepository;
    @Mock BookingRepository bookingRepository;
    @Mock PdfService pdfService;
    @Mock EmailService emailService;
    @Mock NotificationService notificationService;
    @Mock ManagerPropertyAssignmentRepository assignmentRepository;
    @Mock ReportPropertyScopeValidator scopeValidator;
    @Mock OutboxPublisher outboxPublisher;
    @Mock ActivityLogService activityLogService;

    @TempDir Path tempDir;

    private ContractService service;
    private User customer;
    private User manager;
    private User admin;
    private Property property;
    private Room room;
    private Booking booking;
    private Contract contract;

    @BeforeEach
    void setUp() {
        service = new ContractService(
                contractRepository,
                bookingRepository,
                pdfService,
                emailService,
                notificationService,
                assignmentRepository,
                scopeValidator,
                outboxPublisher,
                activityLogService,
                tempDir.toString());

        customer = TestFixtures.user(User.Role.CUSTOMER);
        manager = TestFixtures.user(User.Role.MANAGER);
        admin = TestFixtures.user(User.Role.ADMIN);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        booking = TestFixtures.booking(customer, room, Booking.Status.CONFIRMED);

        contract = new Contract();
        contract.setId(UUID.randomUUID());
        contract.setBooking(booking);
        contract.setCustomer(customer);
        contract.setRoom(room);
        contract.setStatus(Contract.Status.ACTIVE);
        contract.setDepositAmount(booking.getDepositAmount());
        contract.setTotalAmount(booking.getTotalAmount());
        contract.setCheckInDate(booking.getCheckInDate());
        contract.setCheckOutDate(booking.getCheckOutDate());
        contract.setGeneratedAt(LocalDateTime.now());
        contract.setPdfUrl("/uploads/contracts/" + contract.getId() + ".pdf");
    }

    @Test
    void autoGenerate_enqueuesOutbox() {
        service.autoGenerateAndSendContract(booking.getId(), manager);
        verify(outboxPublisher).enqueueContractGenerate(booking.getId(), manager.getId());
    }

    @Test
    void processGenerate_rejectsPendingDeposit() {
        booking.setStatus(Booking.Status.PENDING_DEPOSIT);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        assertThrows(IllegalArgumentException.class, () ->
                service.processContractGenerateFromOutbox(booking.getId(), manager));
    }

    @Test
    void processGenerate_createsContractAndPdf() throws Exception {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(contractRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> {
            Contract c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(UUID.randomUUID());
            }
            return c;
        });
        when(pdfService.generateContractPdf(any(Contract.class))).thenReturn(new byte[]{1, 2, 3, 4});

        service.processContractGenerateFromOutbox(booking.getId(), manager);

        verify(notificationService).sendNotification(
                eq(customer.getId()),
                eq(Notification.Type.CONTRACT_GENERATED),
                any(),
                any(),
                any(),
                eq("Contract"));
        verify(outboxPublisher).enqueueContractEmailSend(any(UUID.class), eq(manager.getId()));
        verify(activityLogService).log(eq(manager), eq(ActivityLogService.CONTRACT_GENERATED),
                eq("Contract"), any(), any());
        assertTrue(Files.isDirectory(tempDir.resolve("contracts")));
    }

    @Test
    void processGenerate_idempotentWhenPdfExists() {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(contractRepository.findByBookingId(booking.getId())).thenReturn(Optional.of(contract));

        service.processContractGenerateFromOutbox(booking.getId(), manager);

        verify(pdfService, never()).generateContractPdf(any());
        verify(outboxPublisher).enqueueContractEmailSend(contract.getId(), manager.getId());
    }

    @Test
    void getAllContracts_filters() {
        when(contractRepository.findAllWithFilters(eq(Contract.Status.ACTIVE), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(contract)));

        PageResponse<?> page = service.getAllContracts(0, 10, "ACTIVE", null, null);
        assertEquals(1, page.getContent().size());
    }

    @Test
    void getManagerContracts_emptyAssignments() {
        when(assignmentRepository.findActivePropertiesByManagerId(manager.getId())).thenReturn(List.of());

        PageResponse<?> page = service.getManagerContracts(manager, null, 0, 10, null, null, null);
        assertEquals(0, page.getTotalElements());
    }

    @Test
    void getManagerContracts_withPropertyId() {
        when(contractRepository.findByPropertyIdsWithFilters(
                eq(List.of(property.getId())), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(contract)));

        PageResponse<?> page = service.getManagerContracts(
                manager, property.getId().toString(), 0, 10, "ALL", " ", "createdAt,asc");
        assertEquals(1, page.getContent().size());
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void getMyContracts() {
        when(contractRepository.findByCustomerWithFilters(
                eq(customer.getId()), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(contract)));

        PageResponse<?> page = service.getMyContracts(customer, 0, 10, null, null, null);
        assertEquals(1, page.getContent().size());
    }

    @Test
    void getContractDetail_forbiddenForOtherCustomer() {
        User other = TestFixtures.user(User.Role.CUSTOMER);
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        assertThrows(ForbiddenException.class, () ->
                service.getContractDetail(contract.getId(), other));
    }

    @Test
    void getContractDetail_adminOk() {
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        ContractDetailResponse detail = service.getContractDetail(contract.getId(), admin);
        assertEquals(contract.getId(), detail.getId());
    }

    @Test
    void getContractByBookingId_missingContract() {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(contractRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                service.getContractByBookingId(booking.getId(), customer));
    }

    @Test
    void resendContractEmail_managerOnly() {
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        assertThrows(ForbiddenException.class, () ->
                service.resendContractEmail(contract.getId(), customer, null));
    }

    @Test
    void resendContractEmail_enqueuesResend() {
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        service.resendContractEmail(contract.getId(), manager, "alt@test.local");
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
        verify(outboxPublisher).enqueueContractResend(contract.getId(), "alt@test.local", manager.getId());
    }

    @Test
    void processEmailSend_updatesSentAt() throws Exception {
        Path pdf = tempDir.resolve("contracts").resolve(contract.getId() + ".pdf");
        Files.createDirectories(pdf.getParent());
        Files.write(pdf, new byte[]{9, 9, 9});
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processContractEmailSendFromOutbox(contract.getId(), null, manager);

        verify(emailService).sendEmailWithAttachment(
                eq(customer.getEmail()), any(), any(), any(), any());
        verify(activityLogService).log(eq(manager), eq(ActivityLogService.CONTRACT_EMAIL_SENT),
                eq("Contract"), eq(contract.getId()), any());
    }

    @Test
    void downloadContractPdf_missingFile_generatesOnce() {
        contract.setPdfUrl(null);
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pdfService.generateContractPdf(contract)).thenReturn(new byte[]{1, 2, 3, 4});

        byte[] bytes = service.downloadContractPdf(contract.getId(), customer);

        assertEquals(4, bytes.length);
        assertTrue(contract.getPdfUrl() != null && contract.getPdfUrl().startsWith("/uploads/contracts/"));
        verify(pdfService).generateContractPdf(contract);
        verify(activityLogService).log(eq(customer), eq(ActivityLogService.CONTRACT_GENERATED),
                eq("Contract"), eq(contract.getId()), any());
    }

    @Test
    void downloadContractPdf_missingDiskFile_regenerates() throws Exception {
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pdfService.generateContractPdf(contract)).thenReturn(new byte[]{5, 6});

        byte[] bytes = service.downloadContractPdf(contract.getId(), customer);

        assertEquals(2, bytes.length);
        verify(pdfService).generateContractPdf(contract);
    }

    @Test
    void processResend_logsResent() throws Exception {
        Path pdf = tempDir.resolve("contracts").resolve(contract.getId() + ".pdf");
        Files.createDirectories(pdf.getParent());
        Files.write(pdf, new byte[]{1});
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processContractResendFromOutbox(contract.getId(), "x@y.z", manager);

        verify(activityLogService).log(eq(manager), eq(ActivityLogService.CONTRACT_RESENT),
                eq("Contract"), eq(contract.getId()), any());
    }
}
