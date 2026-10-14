package com.homestay.service;

import com.homestay.dto.request.EmployeeInspectionResultRequest;
import com.homestay.dto.response.ChecklistItemDefinitionResponse;
import com.homestay.dto.response.EmployeeInspectionResponse;
import com.homestay.dto.response.InspectionChecklistAnswerResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.ChecklistItemDefinition;
import com.homestay.entity.EmployeePropertyAssignment;
import com.homestay.entity.InspectionChecklistAnswer;
import com.homestay.entity.RoomInspection;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.ChecklistItemDefinitionRepository;
import com.homestay.repository.EmployeePropertyAssignmentRepository;
import com.homestay.repository.InspectionChecklistAnswerRepository;
import com.homestay.repository.RoomInspectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SCR-62 - Employee Room Inspection Hub.
 * Checklist definitions from DB; answers persisted on Pass/Fail.
 */
@Service
@RequiredArgsConstructor
public class EmployeeInspectionService {

    private static final List<RoomInspection.Status> OPEN_STATUSES =
            List.of(RoomInspection.Status.PENDING, RoomInspection.Status.IN_PROGRESS);

    private final RoomInspectionRepository roomInspectionRepository;
    private final EmployeePropertyAssignmentRepository employeePropertyAssignmentRepository;
    private final ChecklistItemDefinitionRepository checklistItemDefinitionRepository;
    private final InspectionChecklistAnswerRepository checklistAnswerRepository;

    @Transactional(readOnly = true)
    public List<ChecklistItemDefinitionResponse> listActiveChecklistItems() {
        return checklistItemDefinitionRepository.findByIsActiveTrueAndPropertyIsNullOrderBySortOrderAsc()
                .stream()
                .map(ChecklistItemDefinitionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InspectionChecklistAnswerResponse> listAnswers(UUID inspectionId) {
        return checklistAnswerRepository.findByInspectionIdOrderByChecklistItem_SortOrderAsc(inspectionId)
                .stream()
                .map(InspectionChecklistAnswerResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeInspectionResponse> list(User employee, String status, Pageable pageable) {
        List<UUID> propertyIds = activePropertyIds(employee.getId());
        if (propertyIds.isEmpty()) {
            return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
        }

        List<RoomInspection.Status> statuses = resolveStatuses(status);
        Page<RoomInspection> page =
                roomInspectionRepository.findForEmployee(propertyIds, statuses, employee.getId(), pageable);
        List<EmployeeInspectionResponse> content = page.getContent().stream()
                .map(EmployeeInspectionResponse::fromEntity)
                .toList();
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional
    public void pass(User employee, UUID id, EmployeeInspectionResultRequest request) {
        RoomInspection inspection = loadInScope(employee.getId(), id);
        guardCanPerform(inspection, employee);
        claimIfUnassigned(inspection, employee);
        List<InspectionChecklistAnswer> answers = persistAnswers(inspection, request);
        inspection.setStatus(RoomInspection.Status.PASSED);
        inspection.setInspectedBy(employee);
        inspection.setInspectedAt(LocalDateTime.now());
        inspection.setNote(buildNote(request, answers, false));
        roomInspectionRepository.save(inspection);
    }

    @Transactional
    public void fail(User employee, UUID id, EmployeeInspectionResultRequest request) {
        if (request == null || !StringUtils.hasText(request.resolveNote())) {
            throw new BusinessException("Please describe the damage found");
        }
        RoomInspection inspection = loadInScope(employee.getId(), id);
        guardCanPerform(inspection, employee);
        claimIfUnassigned(inspection, employee);
        List<InspectionChecklistAnswer> answers = persistAnswers(inspection, request);
        inspection.setStatus(RoomInspection.Status.FAILED_WITH_DAMAGE);
        inspection.setInspectedBy(employee);
        inspection.setInspectedAt(LocalDateTime.now());
        inspection.setNote(buildNote(request, answers, true));
        roomInspectionRepository.save(inspection);
    }

    private List<InspectionChecklistAnswer> persistAnswers(
            RoomInspection inspection, EmployeeInspectionResultRequest request) {

        Map<UUID, Boolean> byId = resolveAnswerMap(request);
        if (byId.isEmpty()) {
            return List.of();
        }

        Map<UUID, ChecklistItemDefinition> defs = checklistItemDefinitionRepository.findAllById(byId.keySet())
                .stream()
                .collect(Collectors.toMap(ChecklistItemDefinition::getId, Function.identity()));

        for (UUID itemId : byId.keySet()) {
            ChecklistItemDefinition def = defs.get(itemId);
            if (def == null || !Boolean.TRUE.equals(def.getIsActive())) {
                throw new BusinessException("Checklist item không hợp lệ: " + itemId);
            }
        }

        checklistAnswerRepository.deleteByInspectionId(inspection.getId());
        checklistAnswerRepository.flush();

        List<InspectionChecklistAnswer> saved = new ArrayList<>();
        for (Map.Entry<UUID, Boolean> e : byId.entrySet()) {
            InspectionChecklistAnswer answer = new InspectionChecklistAnswer();
            answer.setInspection(inspection);
            answer.setChecklistItem(defs.get(e.getKey()));
            answer.setPassed(e.getValue());
            saved.add(checklistAnswerRepository.save(answer));
        }
        return saved;
    }

    private Map<UUID, Boolean> resolveAnswerMap(EmployeeInspectionResultRequest request) {
        Map<UUID, Boolean> map = new HashMap<>();
        if (request == null) {
            return map;
        }
        if (request.getAnswers() != null) {
            for (EmployeeInspectionResultRequest.Answer a : request.getAnswers()) {
                if (a.getItemId() != null && a.getPassed() != null) {
                    map.put(a.getItemId(), a.getPassed());
                }
            }
        }
        if (!map.isEmpty()) {
            return map;
        }
        // Legacy checklist by code
        EmployeeInspectionResultRequest.LegacyChecklist c = request.getChecklist();
        if (c == null) {
            return map;
        }
        putLegacy(map, "tv", c.getTv());
        putLegacy(map, "ac", c.getAc());
        putLegacy(map, "minibar", c.getMinibar());
        putLegacy(map, "bathroom", c.getBathroom());
        putLegacy(map, "beds", c.getBeds());
        return map;
    }

    private void putLegacy(Map<UUID, Boolean> map, String code, Boolean passed) {
        if (passed == null) {
            return;
        }
        checklistItemDefinitionRepository.findByCodeAndPropertyIsNull(code)
                .ifPresent(def -> map.put(def.getId(), passed));
    }

    private List<RoomInspection.Status> resolveStatuses(String status) {
        if (!StringUtils.hasText(status)) {
            return OPEN_STATUSES;
        }
        try {
            RoomInspection.Status parsed = RoomInspection.Status.valueOf(status.trim().toUpperCase());
            if (!OPEN_STATUSES.contains(parsed)) {
                throw new BusinessException("status filter must be PENDING or IN_PROGRESS");
            }
            return List.of(parsed);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid inspection status: " + status);
        }
    }

    private List<UUID> activePropertyIds(UUID employeeId) {
        return employeePropertyAssignmentRepository
                .findPropertyIdsByEmployeeIdAndStatus(employeeId, EmployeePropertyAssignment.Status.ACTIVE);
    }

    private RoomInspection loadInScope(UUID employeeId, UUID inspectionId) {
        List<UUID> propertyIds = activePropertyIds(employeeId);
        if (propertyIds.isEmpty()) {
            throw new ResourceNotFoundException("Room inspection not found");
        }
        return roomInspectionRepository.findByIdAndPropertyIdIn(inspectionId, propertyIds)
                .orElseThrow(() -> new ResourceNotFoundException("Room inspection not found"));
    }

    private void guardCanPerform(RoomInspection inspection, User employee) {
        RoomInspection.Status status = inspection.getStatus();
        if (status != RoomInspection.Status.PENDING && status != RoomInspection.Status.IN_PROGRESS) {
            throw new BusinessException("Inspection already completed");
        }
        User holder = inspection.getInspectedBy();
        if (holder != null && !holder.getId().equals(employee.getId())) {
            throw new BusinessException("Pass/Fail only when the inspection is assigned to you");
        }
    }

    private void claimIfUnassigned(RoomInspection inspection, User employee) {
        if (inspection.getInspectedBy() == null) {
            inspection.setInspectedBy(employee);
            if (inspection.getStatus() == RoomInspection.Status.PENDING) {
                inspection.setStatus(RoomInspection.Status.IN_PROGRESS);
            }
        }
    }

    private String buildNote(
            EmployeeInspectionResultRequest request,
            List<InspectionChecklistAnswer> answers,
            boolean fail) {
        StringBuilder sb = new StringBuilder();
        if (request != null && StringUtils.hasText(request.resolveNote())) {
            sb.append(request.resolveNote());
        }
        if (answers != null && !answers.isEmpty()) {
            List<String> parts = answers.stream()
                    .map(a -> a.getChecklistItem().getCode() + "=" + (Boolean.TRUE.equals(a.getPassed()) ? "OK" : "FAIL"))
                    .toList();
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("Checklist: ").append(String.join(", ", parts));
        }
        if (fail && sb.length() == 0) {
            throw new BusinessException("Please describe the damage found");
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
