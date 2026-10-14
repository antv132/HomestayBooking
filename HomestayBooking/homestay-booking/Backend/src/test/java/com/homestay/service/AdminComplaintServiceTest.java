package com.homestay.service;

import com.homestay.dto.response.AdminComplaintResponse;
import com.homestay.dto.response.ComplaintDetailResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Complaint;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.ComplaintRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminComplaintServiceTest {

    @Mock ComplaintRepository complaintRepository;
    @Mock NotificationService notificationService;
    @Mock ComplaintStatusValidator statusValidator;
    @Mock ActivityLogService activityLogService;

    private AdminComplaintService adminComplaintService;
    private User admin;
    private User customer;
    private Complaint complaint;

    @BeforeEach
    void setUp() {
        adminComplaintService = new AdminComplaintService(
                complaintRepository,
                notificationService,
                statusValidator,
                activityLogService);

        admin = TestFixtures.user(User.Role.ADMIN);
        customer = TestFixtures.user(User.Role.CUSTOMER);

        complaint = new Complaint();
        complaint.setId(UUID.randomUUID());
        complaint.setUser(customer);
        complaint.setSubject("Test Complaint");
        complaint.setDescription("Test Description");
        complaint.setStatus(Complaint.Status.OPEN);
        complaint.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void listComplaints_filtersAndPaginates() {
        Page<Complaint> page = new PageImpl<>(List.of(complaint));
        when(complaintRepository.findForAdmin(any(), any(), any(Pageable.class))).thenReturn(page);

        PageResponse<AdminComplaintResponse> result = adminComplaintService.listComplaints(
                "OPEN", "test", PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(complaintRepository).findForAdmin(eq(Complaint.Status.OPEN), eq("test"), any(Pageable.class));
    }

    @Test
    void listComplaints_invalidStatus_throws() {
        assertThrows(BusinessException.class,
                () -> adminComplaintService.listComplaints("INVALID", null, PageRequest.of(0, 10)));
    }

    @Test
    void getDetail_returnsComplaintDetails() {
        when(complaintRepository.findByIdWithUser(complaint.getId())).thenReturn(Optional.of(complaint));

        ComplaintDetailResponse result = adminComplaintService.getDetail(complaint.getId());

        assertNotNull(result);
        assertEquals(complaint.getId(), result.getId());
        assertEquals(complaint.getSubject(), result.getSubject());
    }

    @Test
    void getDetail_notFound_throws() {
        when(complaintRepository.findByIdWithUser(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminComplaintService.getDetail(UUID.randomUUID()));
    }

    @Test
    void resolve_updatesStatusAndNotifiesCustomer() {
        complaint.setStatus(Complaint.Status.INVESTIGATING);
        when(complaintRepository.findByIdWithUser(complaint.getId())).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        AdminComplaintResponse result = adminComplaintService.resolve(
                complaint.getId(), "Issue resolved successfully", admin);

        assertNotNull(result);
        verify(statusValidator).validateTransition(Complaint.Status.INVESTIGATING, Complaint.Status.RESOLVED);
        verify(statusValidator).validateResolutionNotes(eq(Complaint.Status.RESOLVED), anyString(), any());
        verify(complaintRepository).save(complaint);
        verify(activityLogService).log(eq(admin), eq(ActivityLogService.COMPLAINT_STATUS_CHANGED),
                eq("Complaint"), eq(complaint.getId()), anyString());
        verify(notificationService).sendNotification(eq(customer.getId()), any(), anyString(), anyString(),
                eq(complaint.getId()), eq("Complaint"));
    }

    @Test
    void updateStatus_openToInvestigating_success() {
        complaint.setStatus(Complaint.Status.OPEN);
        when(complaintRepository.findByIdWithUser(complaint.getId())).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        AdminComplaintResponse result = adminComplaintService.updateStatus(
                complaint.getId(), Complaint.Status.INVESTIGATING, null, admin);

        assertNotNull(result);
        assertEquals(Complaint.Status.INVESTIGATING, complaint.getStatus());
        verify(statusValidator).validateTransition(Complaint.Status.OPEN, Complaint.Status.INVESTIGATING);
    }

    @Test
    void updateStatus_toResolved_setsResolvedAt() {
        complaint.setStatus(Complaint.Status.INVESTIGATING);
        complaint.setResolvedAt(null);
        when(complaintRepository.findByIdWithUser(complaint.getId())).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        adminComplaintService.updateStatus(
                complaint.getId(), Complaint.Status.RESOLVED, "Fixed the issue", admin);

        assertNotNull(complaint.getResolvedAt());
        assertEquals("Fixed the issue", complaint.getResolutionNotes());
    }

    @Test
    void updateStatus_validatorThrows_propagates() {
        complaint.setStatus(Complaint.Status.OPEN);
        when(complaintRepository.findByIdWithUser(complaint.getId())).thenReturn(Optional.of(complaint));
        doThrow(new BusinessException("Invalid transition"))
                .when(statusValidator).validateTransition(any(), any());

        assertThrows(BusinessException.class,
                () -> adminComplaintService.updateStatus(
                        complaint.getId(), Complaint.Status.RESOLVED, "notes", admin));
    }

    @Test
    void updateStatus_noCustomer_skipsNotification() {
        complaint.setUser(null);
        complaint.setStatus(Complaint.Status.OPEN);
        when(complaintRepository.findByIdWithUser(complaint.getId())).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        adminComplaintService.updateStatus(
                complaint.getId(), Complaint.Status.INVESTIGATING, null, admin);

        verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any(), any());
    }
}
