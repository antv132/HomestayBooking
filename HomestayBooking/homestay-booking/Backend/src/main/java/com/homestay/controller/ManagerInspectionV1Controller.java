package com.homestay.controller;

import com.homestay.dto.request.AssignInspectorRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.InspectionChecklistAnswerResponse;
import com.homestay.dto.response.InspectionSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.RoomInspection;
import com.homestay.entity.User;
import com.homestay.service.EmployeeInspectionService;
import com.homestay.service.RoomInspectionManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** SCR-42 — Inspection Management (Manager): list + assign inspector + checklist answers. */
@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
public class ManagerInspectionV1Controller {

    private final RoomInspectionManagerService roomInspectionManagerService;
    private final EmployeeInspectionService employeeInspectionService;

    @GetMapping("/inspections")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<InspectionSummaryResponse>>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam UUID propertyId,
            @RequestParam(required = false) RoomInspection.Status status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<InspectionSummaryResponse> data = roomInspectionManagerService.listForManager(
                currentUser, propertyId, status, search, page, size);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/inspections/{id}/checklist-answers")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<InspectionChecklistAnswerResponse>>> checklistAnswers(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(employeeInspectionService.listAnswers(id)));
    }

    @PatchMapping("/inspections/{id}/assign")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<InspectionSummaryResponse>> assignInspector(
            @PathVariable UUID id,
            @Valid @RequestBody AssignInspectorRequest body,
            @AuthenticationPrincipal User currentUser) {

        InspectionSummaryResponse data =
                roomInspectionManagerService.assignInspector(currentUser, id, body);

        return ResponseEntity.ok(ApiResponse.ok("Gán nhân viên kiểm tra thành công", data));
    }
}
