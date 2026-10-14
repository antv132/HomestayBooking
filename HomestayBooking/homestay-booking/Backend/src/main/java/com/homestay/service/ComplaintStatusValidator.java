package com.homestay.service;

import com.homestay.entity.Complaint;
import com.homestay.exception.BusinessException;
import org.springframework.stereotype.Component;

/**
 * FR-17 — linear complaint transitions:
 * OPEN → INVESTIGATING → RESOLVED → CLOSED (terminal).
 * RESOLVED requires resolution notes (min 10 chars).
 */
@Component
public class ComplaintStatusValidator {

    public void validateTransition(Complaint.Status current, Complaint.Status next) {
        if (current == null || next == null) {
            throw new BusinessException("Trạng thái khiếu nại không hợp lệ");
        }
        if (current == next) {
            throw new BusinessException("Trạng thái mới trùng với trạng thái hiện tại");
        }
        boolean ok = switch (current) {
            case OPEN -> next == Complaint.Status.INVESTIGATING;
            case INVESTIGATING -> next == Complaint.Status.RESOLVED;
            case RESOLVED -> next == Complaint.Status.CLOSED;
            case CLOSED -> false;
        };
        if (!ok) {
            throw new BusinessException(
                    "Không thể chuyển từ " + current.name() + " sang " + next.name());
        }
    }

    public void validateResolutionNotes(Complaint.Status target, String notes, String existingNotes) {
        if (target != Complaint.Status.RESOLVED) {
            return;
        }
        String note = notes != null ? notes.trim() : "";
        if (note.isBlank()) {
            note = existingNotes != null ? existingNotes.trim() : "";
        }
        if (note.length() < 10) {
            throw new BusinessException(
                    "Ghi chú giải quyết bắt buộc ít nhất 10 ký tự khi chuyển sang RESOLVED");
        }
    }
}
