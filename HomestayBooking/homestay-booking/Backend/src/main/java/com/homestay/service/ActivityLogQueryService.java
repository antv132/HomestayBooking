package com.homestay.service;

import com.homestay.dto.response.ActivityLogEntryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.ActivityLog;
import com.homestay.repository.ActivityLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityLogQueryService {

    private final ActivityLogRepository activityLogRepository;

    @Transactional(readOnly = true)
    public PageResponse<ActivityLogEntryResponse> listLogs(
            LocalDateTime from,
            LocalDateTime to,
            String action,
            UUID userId,
            String search,
            Pageable pageable) {

        String normalizedAction = (action != null && !action.isBlank()) ? action.trim() : null;
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        Specification<ActivityLog> spec = buildSpec(from, to, normalizedAction, userId, normalizedSearch);

        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ActivityLog> page = activityLogRepository.findAll(spec, sorted);

        return new PageResponse<>(
                page.getContent().stream().map(ActivityLogEntryResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /**
     * Criteria avoids SQL Server errors from JPQL {@code (:param IS NULL OR ...)} on
     * datetime/uuid and from {@code @EntityGraph} + DISTINCT + ORDER BY pagination.
     */
    static Specification<ActivityLog> buildSpec(
            LocalDateTime from,
            LocalDateTime to,
            String action,
            UUID userId,
            String search) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();

            if (from != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            if (action != null) {
                preds.add(cb.equal(root.get("action"), action));
            }
            if (userId != null) {
                preds.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("details"), "")), pattern),
                        cb.like(cb.lower(root.get("action")), pattern)));
            }

            if (preds.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
    }
}
