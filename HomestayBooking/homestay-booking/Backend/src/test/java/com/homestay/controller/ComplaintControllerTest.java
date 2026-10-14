package com.homestay.controller;

import com.homestay.dto.request.CreateComplaintRequest;
import com.homestay.dto.request.UpdateComplaintStatusRequest;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.ComplaintService;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplaintControllerTest {

    @Mock ComplaintService complaintService;

    private ComplaintController controller;
    private User customer;

    @BeforeEach
    void setUp() {
        controller = new ComplaintController(complaintService);
        customer = TestFixtures.user(User.Role.CUSTOMER);
    }

    @Test
    void managerEndpoints_gone() {
        assertEquals(HttpStatus.GONE, controller.getAllComplaintsDeprecated().getStatusCode());
        assertEquals(HttpStatus.GONE, controller.getComplaintDetailDeprecated(UUID.randomUUID()).getStatusCode());
        assertEquals(HttpStatus.GONE,
                controller.updateComplaintStatus(UUID.randomUUID(), new UpdateComplaintStatusRequest()).getStatusCode());
    }

    @Test
    void customerLegacyAndV1() {
        when(complaintService.submitComplaint(any(), eq(customer))).thenReturn(null);
        assertEquals(201, controller.submitComplaint(new CreateComplaintRequest(), customer).getStatusCode().value());
        assertEquals(201, controller.submitComplaintV1(new CreateComplaintRequest(), customer).getStatusCode().value());

        when(complaintService.getMyComplaints(customer.getId())).thenReturn(List.of());
        controller.getMyComplaints(customer);

        when(complaintService.listMyComplaints(eq(customer.getId()), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0));
        controller.listMyComplaintsV1(customer, -1, 100);

        UUID id = UUID.randomUUID();
        when(complaintService.getMyComplaintDetail(customer.getId(), id)).thenReturn(null);
        controller.getMyComplaintDetailV1(customer, id);
        verify(complaintService).getMyComplaintDetail(customer.getId(), id);
    }
}
