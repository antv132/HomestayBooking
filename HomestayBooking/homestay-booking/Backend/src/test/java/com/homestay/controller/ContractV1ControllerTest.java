package com.homestay.controller;

import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.ContractService;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractV1ControllerTest {

    @Mock ContractService contractService;

    private ContractV1Controller controller;
    private User customer;
    private User manager;

    @BeforeEach
    void setUp() {
        controller = new ContractV1Controller(contractService);
        customer = TestFixtures.user(User.Role.CUSTOMER);
        manager = TestFixtures.user(User.Role.MANAGER);
    }

    @Test
    void listDetailBookingPdfResend() {
        when(contractService.getMyContracts(eq(customer), eq(0), eq(10), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0));
        controller.getMyContractsAlias(0, 10, null, null, null, customer);

        UUID id = UUID.randomUUID();
        when(contractService.getContractDetail(id, customer)).thenReturn(null);
        controller.getContractDetail(id, customer);

        UUID bookingId = UUID.randomUUID();
        when(contractService.getContractByBookingId(bookingId, customer)).thenReturn(null);
        controller.getContractByBooking(bookingId, customer);

        when(contractService.downloadContractPdf(id, customer)).thenReturn(new byte[]{1, 2, 3});
        assertEquals(HttpStatus.OK, controller.downloadContractPdf(id, customer).getStatusCode());

        controller.resendContractEmail(id, manager, Map.of("email", "alt@test.local"));
        verify(contractService).resendContractEmail(id, manager, "alt@test.local");
    }
}
