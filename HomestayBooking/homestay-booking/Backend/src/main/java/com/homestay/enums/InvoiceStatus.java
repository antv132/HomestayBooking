package com.homestay.enums;

/** FR-11 invoice status (mirrors {@link com.homestay.entity.Invoice.Status}). */
public enum InvoiceStatus {
    UNPAID,
    PENDING_PAYMENT,
    PAID,
    CANCELLED
}
