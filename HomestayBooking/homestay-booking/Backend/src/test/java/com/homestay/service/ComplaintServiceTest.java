package com.homestay.service;

import com.homestay.dto.request.CreateComplaintRequest;
import com.homestay.dto.request.UpdateComplaintStatusRequest;
import com.homestay.dto.response.ComplaintDetailResponse;
import com.homestay.dto.response.ComplaintSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Complaint;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.ComplaintRepository;
import com.homestay.repository.UserRepository;
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
class ComplaintServiceTest {

    @Mock ComplaintRepository complaintRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock ActivityLogService activityLogService;

    private ComplaintService complaintService;
    private User customer;
    private User admin;
    private Complaint complaint;

    @BeforeEach
    void setUp() {
        complaintService = new ComplaintService(
                complaintRepository,
                userRepository,
                notificationService,
                activityLogService);

        customer = TestFixtures.user(User.Role.CUSTOMER);
        admin = TestFixtures.user(User.Role.ADMIN);
        
        complaint = new Complaint();
        complaint.setId(UUID.randomUUID());
        complaint.setUser(customer);
        complaint.setSubject("Test Complaint");
        complaint.setDescription("Test Description");
        complaint.setStatus(Complaint.Status.OPEN);
        complaint.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void submitComplaint_createsWithOpenStatus() {
        CreateComplaintRequest request = new CreateComplaintRequest();
        request.setSubject("AC not working");
        request.setDescription("The air conditioner in my room is broken");

        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> {
            Complaint c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(userRepository.findByRole(User.Role.ADMIN)).thenReturn(List.of(admin));

        ComplaintDetailResponse result = complaintService.submitComplaint(request, customer);

        assertNotNull(result);
        assertEquals("AC not working", result.getSubject());
        assertEquals("OPEN", result.getStatus());
        verify(complaintRepository).save(argThat(c -> c.getStatus() == Complaint.Status.OPEN));
        verify(activityLogService).log(eq(customer), eq(ActivityLogService.COMPLAINT_CREATED),
                eq("Complaint"), any(UUID.class), anyString());
        verify(notificationService).sendNotification(eq(admin.getId()), any(), anyString(), anyString(),
                any(UUID.class), eq("Complaint"));
    }

    @Test
    void getAllComplaints_capsSizeAt100() {
        Page<Complaint> page = new PageImpl<>(List.of(complaint));
        when(complaintRepository.findByFilters(any(), any(), any(Pageable.class))).thenReturn(page);

        complaintService.getAllComplaints(0, 200, null, null);

        verify(complaintRepository).findByFilters(any(), any(), argThat(p -> p.getPageSize() == 100));
    }

    @Test
    void getAllComplaints_invalidStatus_throws() {
        assertThrows(BusinessException.class,
                () -> complaintService.getAllComplaints(0, 10, "INVALID_STATUS", null));
    }

    @Test
    void getAllComplaints_filtersCorrectly() {
        Page<Complaint> page = new PageImpl<>(List.of(complaint));
        when(complaintRepository.findByFilters(eq(Complaint.Status.OPEN), eq("test"), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<ComplaintSummaryResponse> result = 
                complaintService.getAllComplaints(0, 10, "OPEN", "test");

        assertNotNull(result);
        verify(complaintRepository).findByFilters(eq(Complaint.Status.OPEN), eq("test"), any(Pageable.class));
    }

    @Test
    void getComplaintDetail_returnsDetail() {
        when(complaintRepository.findById(complaint.getId())).thenReturn(Optional.of(complaint));

        ComplaintDetailResponse result = complaintService.getComplaintDetail(complaint.getId());

        assertNotNull(result);
        assertEquals(complaint.getId(), result.getId());
        assertEquals(complaint.getSubject(), result.getSubject());
    }

    @Test
    void getComplaintDetail_notFound_throws() {
        when(complaintRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> complaintService.getComplaintDetail(UUID.randomUUID()));
    }

    @Test
    void getMyComplaintDetail_wrongCustomer_throws() {
        when(complaintRepository.findByIdAndUserId(complaint.getId(), customer.getId()))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> complaintService.getMyComplaintDetail(customer.getId(), complaint.getId()));
    }

    @Test
    void listMyComplaints_returnsCustomerComplaints() {
        Page<Complaint> page = new PageImpl<>(List.of(complaint));
        when(complaintRepository.findByUserIdOrderByCreatedAtDesc(eq(customer.getId()), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<ComplaintDetailResponse> result = 
                complaintService.listMyComplaints(customer.getId(), PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(complaintRepository).findByUserIdOrderByCreatedAtDesc(eq(customer.getId()), any(Pageable.class));
    }

    @Test
    void updateComplaintStatus_openToInvestigating_success() {
        complaint.setStatus(Complaint.Status.OPEN);
        when(complaintRepository.findById(complaint.getId())).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        UpdateComplaintStatusRequest request = new UpdateComplaintStatusRequest();
        request.setStatus(Complaint.Status.INVESTIGATING);

        ComplaintDetailResponse result = complaintService.updateComplaintStatus(complaint.getId(), request);

        assertNotNull(result);
        assertEquals(Complaint.Status.INVESTIGATING, complaint.getStatus());
        verify(complaintRepository).save(complaint);
    }

    @Test
    void updateComplaintStatus_toResolved_requiresResolutionNotes() {
        complaint.setStatus(Complaint.Status.INVESTIGATING);
        when(complaintRepository.findById(complaint.getId())).thenReturn(Optional.of(complaint));

        UpdateComplaintStatusRequest request = new UpdateComplaintStatusRequest();
        request.setStatus(Complaint.Status.RESOLVED);
        request.setResolutionNotes(null);

        assertThrows(BusinessException.class,
                () -> complaintService.updateComplaintStatus(complaint.getId(), request));
    }

    @Test
    void updateComplaintStatus_toResolved_setsResolvedAt() {
        complaint.setStatus(Complaint.Status.INVESTIGATING);
        when(complaintRepository.findById(complaint.getId())).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        UpdateComplaintStatusRequest request = new UpdateComplaintStatusRequest();
        request.setStatus(Complaint.Status.RESOLVED);
        request.setResolutionNotes("Issue has been resolved successfully");

        complaintService.updateComplaintStatus(complaint.getId(), request);

        assertNotNull(complaint.getResolvedAt());
        assertEquals(Complaint.Status.RESOLVED, complaint.getStatus());
    }

    @Test
    void updateComplaintStatus_sameStatus_throws() {
        complaint.setStatus(Complaint.Status.OPEN);
        when(complaintRepository.findById(complaint.getId())).thenReturn(Optional.of(complaint));

        UpdateComplaintStatusRequest request = new UpdateComplaintStatusRequest();
        request.setStatus(Complaint.Status.OPEN);

        assertThrows(BusinessException.class,
                () -> complaintService.updateComplaintStatus(complaint.getId(), request));
    }

    @Test
    void updateComplaintStatus_toClosed_requiresResolutionNotes() {
        complaint.setStatus(Complaint.Status.RESOLVED);
        when(complaintRepository.findById(complaint.getId())).thenReturn(Optional.of(complaint));

        UpdateComplaintStatusRequest request = new UpdateComplaintStatusRequest();
        request.setStatus(Complaint.Status.CLOSED);
        request.setResolutionNotes("");

        assertThrows(BusinessException.class,
                () -> complaintService.updateComplaintStatus(complaint.getId(), request));
    }
}
