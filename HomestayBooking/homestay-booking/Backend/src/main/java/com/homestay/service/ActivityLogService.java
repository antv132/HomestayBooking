package com.homestay.service;

import com.homestay.entity.ActivityLog;
import com.homestay.entity.User;
import com.homestay.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Writes ActivityLog rows using action names already enumerated on {@link ActivityLog}.
 */
@Service
public class ActivityLogService {

    public static final String CONTRACT_GENERATED = "CONTRACT_GENERATED";
    public static final String CONTRACT_EMAIL_SENT = "CONTRACT_EMAIL_SENT";
    public static final String CONTRACT_RESENT = "CONTRACT_RESENT";
    public static final String PAYMENT_CREATED = "PAYMENT_CREATED";
    public static final String PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String PAYMENT_REJECTED = "PAYMENT_REJECTED";
    public static final String INVOICE_ISSUED = "INVOICE_ISSUED";
    public static final String INVOICE_PAID = "INVOICE_PAID";
    public static final String INVOICE_CANCELLED = "INVOICE_CANCELLED";
    public static final String INVOICE_ADJUSTED = "INVOICE_ADJUSTED";
    /** FR-09 SCR-51 — Admin Suspend / Activate Customer */
    public static final String USER_SUSPENDED = "USER_SUSPENDED";
    public static final String USER_ACTIVATED = "USER_ACTIVATED";
    /** FR-15 */
    public static final String NOTIFICATION_CREATED = "NOTIFICATION_CREATED";
    public static final String NOTIFICATION_MARKED_READ = "NOTIFICATION_MARKED_READ";
    public static final String NOTIFICATION_MARKED_ALL_READ = "NOTIFICATION_MARKED_ALL_READ";
    public static final String OUTBOX_PROCESSED = "OUTBOX_PROCESSED";
    public static final String OUTBOX_FAILED = "OUTBOX_FAILED";
    public static final String OUTBOX_RETRY_REQUESTED = "OUTBOX_RETRY_REQUESTED";
    /** FR-13 MaintenanceTicket lifecycle */
    public static final String TICKET_CREATED = "TICKET_CREATED";
    public static final String TICKET_UPDATED = "TICKET_UPDATED";
    public static final String TICKET_DELETED = "TICKET_DELETED";
    public static final String TICKET_ASSIGNED = "TICKET_ASSIGNED";
    public static final String TICKET_STATUS_CHANGED = "TICKET_STATUS_CHANGED";
    public static final String TICKET_CLOSED = "TICKET_CLOSED";
    /** FR-14 Review & Rating */
    public static final String REVIEW_CREATED = "REVIEW_CREATED";
    public static final String REVIEW_UPDATED = "REVIEW_UPDATED";
    public static final String REVIEW_DELETED = "REVIEW_DELETED";
    public static final String REVIEW_HIDDEN = "REVIEW_HIDDEN";
    public static final String REVIEW_PUBLISHED = "REVIEW_PUBLISHED";
    /** FR-20 Employee Management */
    public static final String EMPLOYEE_CREATED = "EMPLOYEE_CREATED";
    public static final String EMPLOYEE_ASSIGNED = "EMPLOYEE_ASSIGNED";
    public static final String EMPLOYEE_UPDATED = "EMPLOYEE_UPDATED";
    public static final String EMPLOYEE_STATUS_CHANGED = "EMPLOYEE_STATUS_CHANGED";
    public static final String EMPLOYEE_REASSIGNED = "EMPLOYEE_REASSIGNED";
    /** FR-17 Administration */
    public static final String COMPLAINT_CREATED = "COMPLAINT_CREATED";
    public static final String COMPLAINT_STATUS_CHANGED = "COMPLAINT_STATUS_CHANGED";
    public static final String SETTINGS_UPDATED = "SETTINGS_UPDATED";

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional
    public void log(User actor, String action, String entityType, UUID entityId, String details) {
        log(actor, action, entityType, entityId, details, null);
    }

    @Transactional
    public void log(User actor, String action, String entityType, UUID entityId, String details, String ipAddress) {
        ActivityLog entry = new ActivityLog();
        entry.setUser(actor);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);
        entry.setIpAddress(ipAddress);
        activityLogRepository.save(entry);
    }
}
