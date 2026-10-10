# Research: FR-13 Maintenance Management

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: `docs/Specification_v2.md` FR-13, `maintenanceApi.ts`, `MaintenanceMgmtDetailPage.tsx`

## 1. Table Ownership

**Decision**: FR-13 **owns** `maintenance_tickets` and **MAINTENANCE** rows in shared `attachments` table (EntityType discriminator per Specification §5).

**Rationale**: Spec FR-004; Attachment entity shared with DamageItem (FR-23) — same pattern as PaymentReceipt separate table vs generic Attachment.

**Alternatives considered**: `photo_urls` JSON column on ticket — rejected (Specification §5 Attachment entity; file metadata needed).

## 2. Active Booking Gate

**Decision**: `MaintenanceBookingValidator.assertActiveForCustomer(bookingId, customerId)` allows only bookings with status **CONFIRMED** or **CHECKED_IN** owned by customer; derives `roomId` and `propertyId` from booking.

**Rationale**: Spec Assumptions + §7 "Maintenance ticket requires an active booking"; auto-fill room from booking prevents customer picking wrong room.

**Alternatives considered**: Customer picks room without booking — rejected per FR-13.

## 3. Status State Machine

**Decision**: Central `MaintenanceTicketStatusService.transition(ticket, actorRole, targetStatus, context)` enforces:

| From | To | Actor |
|------|-----|-------|
| OPEN | ASSIGNED | MANAGER (assign) |
| ASSIGNED | IN_PROGRESS | EMPLOYEE (assignee) |
| IN_PROGRESS | RESOLVED | EMPLOYEE (assignee) |
| RESOLVED | CLOSED | MANAGER (close + resolutionNote) |

Reassign: MANAGER updates `assignedEmployeeId` on ASSIGNED/IN_PROGRESS/RESOLVED (not CLOSED) without status change unless Open→Assigned on first assign.

**Rationale**: Spec FR-014; entity-ui-mapping §2.3; prevents Manager skipping Employee steps (current frontend bug).

**Alternatives considered**: Manager can set any status — rejected (violates spec segregation).

## 4. File Upload Pattern

**Decision**: Multipart `POST /api/v1/maintenance-tickets` with fields `bookingId`, `title`, `description`, `files[]` (0–5). Store under `APP_UPLOADS_DIR/maintenance/{ticketId}/`; persist `attachments` rows.

Update (Open only): `PUT /api/v1/maintenance-tickets/{id}` multipart — replace attachment set via `keepAttachmentIds[]` + new `files[]`.

**Rationale**: Existing `MaintenancePages.tsx` already uses FormData; aligns with PaymentReceipt upload pattern.

**Alternatives considered**: Pre-signed URL direct to S3 — deferred v1 (local disk like receipts).

## 5. API Surface Alignment

**Decision**: Split role-based controllers per `api-spec-by-screen.md`:

| Role | Base path |
|------|-----------|
| Customer | `/api/v1/maintenance-tickets/me`, `POST /maintenance-tickets`, `GET/PUT/DELETE /maintenance-tickets/{id}` |
| Manager | `/api/v1/manager/maintenance-tickets`, `PATCH .../assign`, `PATCH .../close` |
| Employee | `/api/v1/employee/maintenance-tickets`, `PATCH .../status` |
| Admin | `/api/v1/admin/maintenance-tickets` (read-only) |

**Rationale**: RBAC Segregation; api-spec SCR-22 uses `/me` suffix; frontend currently uses monolithic `/api/maintenance-tickets/all` — migrate.

**Alternatives considered**: Single controller with role checks — rejected (inconsistent with payment/contract split).

## 6. Manager Assign Employee

**Decision**: `PATCH /manager/maintenance-tickets/{id}/assign` body `{ assigneeId }`. Validate assignee is EMPLOYEE role with active assignment to ticket's property (FR-06). First assign: OPEN→ASSIGNED; reassign: update assignee + `assignedAt`.

**Rationale**: api-spec SCR-41; §7 same-property rule.

**Alternatives considered**: Combined assign+status endpoint — rejected (clearer audit trail).

## 7. Employee Status Update

**Decision**: `PATCH /employee/maintenance-tickets/{id}/status` body `{ status: IN_PROGRESS|RESOLVED, workNote?: string }`. Only assignee; only valid next status.

**Rationale**: api-spec SCR-61; optional internal `workNote` (Manager-visible, not Customer).

**Alternatives considered**: Separate start/complete endpoints — single PATCH sufficient with enum guard.

## 8. Manager Close (Verify)

**Decision**: `PATCH /manager/maintenance-tickets/{id}/close` body `{ resolutionNote }` (min 10 chars). RESOLVED→CLOSED; sets `verifiedBy`, `verifiedAt`, `resolutionNote`.

**Rationale**: Spec US-5; api-spec lacks explicit close endpoint — add per spec (Manager verify completion).

**Alternatives considered**: Reuse generic `updateTicketStatus` — rejected (current frontend lets Manager skip workflow).

## 9. Notification Integration (FR-15)

**Decision**: `MaintenanceNotificationPublisher.publishStatusChanged(ticket, previousStatus)` publishes Spring `MaintenanceTicketStatusChangedEvent` + optional `OutboxEvent` type `MAINTENANCE_STATUS_CHANGED` for FR-15 worker. v1: log + insert outbox stub if FR-15 table exists.

**Rationale**: Spec FR-015, FR-020 boundary; US-6 P2 acceptable with stub.

**Alternatives considered**: Inline email send — rejected (FR-15 owns delivery).

## 10. Soft Delete

**Decision**: `deleted_at TIMESTAMPTZ` on `maintenance_tickets`; Customer DELETE sets timestamp; all list queries filter `deleted_at IS NULL`. Admin audit can include deleted.

**Rationale**: Spec Assumptions soft-delete; preserves audit trail.

**Alternatives considered**: Hard delete — rejected.

## 11. Property Scope Queries

**Decision**: Manager list: `JOIN rooms r ON ticket.room_id = r.id WHERE r.property_id IN (:managerPropertyIds)`. Employee list: `assigned_employee_id = :currentUserId`. Customer: `customer_id = :currentUserId`.

**Rationale**: §6 RBAC + §7 UNAUTHORIZED_PROPERTY_ACCESS.

**Alternatives considered**: Denormalize property_id on ticket — add column for index performance (included in V029).

## 12. Frontend Consolidation

**Decision**: Keep `MaintenancePages.tsx` as canonical Customer screens; deprecate duplicate `MaintenanceListPage.tsx` / `CreateMaintenancePage.tsx` / `MaintenanceDetailPage.tsx` routes if redundant. Create `MaintenanceWorkspacePage.tsx` for Employee SCR-61 (pattern from screendesign SCR-60 swipe/list).

**Rationale**: Reduce drift; Employee workspace missing entirely.

**Alternatives considered**: New feature folder — unnecessary; extend existing pages.

## 13. Flyway Version

**Decision**: `V029__maintenance_tickets_fr13.sql` — next after FR-12 V028.

**Rationale**: Sequential migration numbering across features.

**Alternatives considered**: Combine with FR-04 — rejected (feature ownership clarity).
