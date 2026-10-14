package com.homestay.controller;

import com.homestay.dto.request.AdminCreatePropertyRequest;
import com.homestay.dto.request.AdminUpdatePropertyRequest;
import com.homestay.dto.request.AssignManagerRequest;
import com.homestay.dto.response.AdminPropertyResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.AdminPropertyService;
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
class AdminPropertyControllerTest {

    @Mock AdminPropertyService adminPropertyService;

    private AdminPropertyController controller;
    private User admin;

    @BeforeEach
    void setUp() {
        controller = new AdminPropertyController(adminPropertyService);
        admin = TestFixtures.user(User.Role.ADMIN);
    }

    @Test
    void list_usesSearchAlias() {
        when(adminPropertyService.listProperties(eq("hue"), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0));
        assertEquals(HttpStatus.OK,
                controller.list(0, 10, null, "hue", null, null, "name,asc").getStatusCode());
    }

    @Test
    void createGetUpdateAssignHistory() {
        when(adminPropertyService.createProperty(any(), eq(admin))).thenReturn(new AdminPropertyResponse());
        assertEquals(HttpStatus.CREATED,
                controller.create(new AdminCreatePropertyRequest(), admin).getStatusCode());

        UUID id = UUID.randomUUID();
        when(adminPropertyService.getProperty(id)).thenReturn(new AdminPropertyResponse());
        controller.getById(id);

        when(adminPropertyService.updateProperty(eq(id), any(), eq(admin)))
                .thenReturn(new AdminPropertyResponse());
        controller.update(id, new AdminUpdatePropertyRequest(), admin);

        AssignManagerRequest assign = new AssignManagerRequest();
        assign.setManagerId(UUID.randomUUID());
        when(adminPropertyService.assignManager(eq(id), eq(assign.getManagerId()), eq(admin)))
                .thenReturn(new AdminPropertyResponse());
        controller.assignManager(id, assign, admin);

        when(adminPropertyService.listAssignmentHistory(eq(id), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));
        controller.assignmentHistory(id, 0, 20);
        verify(adminPropertyService).listAssignmentHistory(eq(id), any());
    }
}
