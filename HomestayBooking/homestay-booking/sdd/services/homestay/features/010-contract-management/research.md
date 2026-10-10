# Research: FR-10 Contract Management

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: `docs/Specification_v2.md` FR-10, FR-04 outbox, `contractApi.ts`

## 1. Table Ownership & Migration Order

**Decision**: FR-10 creates `contracts` (V024) after FR-04 `outbox_events` (V023). `contract_addendums` (V025) P2 after damage flow.

**Rationale**: Worker reads outbox + writes contracts; booking FK required.

**Alternatives considered**: Store PDF only in blob storage without DB row — rejected (no list/detail/status).

## 2. Outbox Event Types

**Decision**:

| Event (FR-04 writes) | FR-10 worker action |
|----------------------|---------------------|
| `CONTRACT_GENERATE_REQUESTED` | Generate PDF + insert Contract + enqueue `CONTRACT_EMAIL_SEND` |
| `CONTRACT_EMAIL_SEND` | Send email, update sentAt |
| `CONTRACT_RESEND` | Resend same pdfUrl, update sentAt |

**Rationale**: Align FR-04 research.md; separate generate vs email for retry granularity.

**Alternatives considered**: Single combined event — rejected (email retry would regenerate PDF).

## 3. Idempotency

**Decision**: `UNIQUE (booking_id)` on contracts; worker checks `existsByBookingId` before generate.

**Rationale**: spec FR-004; outbox replay safe.

**Alternatives considered**: Application-only check — rejected under concurrency.

## 4. PDF Generation Library

**Decision**: **OpenPDF** (fork iText 4 LGPL) with HTML template rendered via **Flying Saucer** or simple programmatic PDF in v1 if template simple.

**Rationale**: Java ecosystem standard; Apache 2.0 alternative PDFBox for programmatic layout.

**Alternatives considered**: External microservice — YAGNI v1.

## 5. PDF Storage

**Decision**: Local filesystem `app.upload.contracts-dir` (env `APP_CONTRACTS_DIR`) → public URL `/uploads/contracts/{id}.pdf` via Spring resource handler or signed download endpoint only.

**Rationale**: match FR-08 room upload pattern; S3 later.

**Alternatives considered**: Store BYTEA in DB — rejected (bloat).

## 6. Immutable Snapshot Fields

**Decision**: Persist on Contract row at generate time: customerId, roomId, checkInDate, checkOutDate, depositAmount, totalAmount, denormalized customerName, roomNumber, propertyName for list display without joins to mutable room price.

**Rationale**: spec FR-003; PDF and DB row match even if room renamed.

**Alternatives considered**: Join live room data on read — rejected (violates snapshot).

## 7. Customer vs Manager API Split

**Decision**:

| Role | Base |
|------|------|
| CUSTOMER | `/api/v1/contracts/me`, `/api/v1/contracts/{id}`, `/api/v1/contracts/{id}/pdf` |
| MANAGER | `/api/v1/manager/contracts`, `/api/v1/manager/contracts/{id}`, `/api/v1/manager/contracts/{id}/pdf`, `POST .../resend` |

**Rationale**: api-spec SCR-21/38; clear RBAC.

**Alternatives considered**: Single `/contracts` with role filter — rejected (FR-06 pattern).

## 8. Manager Property Scope

**Decision**: Filter contracts via `booking.room.property_id IN managerAssignedPropertyIds`; optional `?propertyId=` query param.

**Rationale**: spec US-3; FR-06 PropertyAccessValidator pattern.

**Alternatives considered**: Global manager view — rejected.

## 9. Contract Status Sync

**Decision**: `BookingStatusListener` or hook in ContractService.syncStatusFromBooking:

- Booking `CHECKED_OUT` → Contract `COMPLETED`
- Booking `CANCELLED` / `NO_SHOW` → Contract `CANCELLED`

**Rationale**: spec FR-011 + Edge Cases.

**Alternatives considered**: Cron batch — acceptable fallback v1.

## 10. Resend Behavior

**Decision**: Manager POST resend inserts `CONTRACT_RESEND` outbox with `{ contractId, email? }`; worker sends existing pdfUrl attachment; updates sentAt; ActivityLog `CONTRACT_RESENT`.

**Rationale**: spec — no PDF regeneration.

**Alternatives considered**: Synchronous SMTP in controller — rejected (Outbox reliability).

## 11. Email Content

**Decision**: Subject "Hợp đồng đặt phòng - {propertyName}"; body HTML + PDF attachment; fallback link to Customer SCR-21 detail.

**Rationale**: spec FR-002; bilingual property name from snapshot.

**Alternatives considered**: Link-only email — insufficient for offline access.

## 12. Contract Addendum (P2)

**Decision**: Separate table `contract_addendums` with `parent_contract_id`, `damage_fee_amount`, own pdfUrl; trigger `CONTRACT_ADDENDUM_REQUESTED` from FR-23 damage approve handler.

**Rationale**: spec FR-010 immutable parent.

**Alternatives considered**: Append pages to original PDF — rejected (breaks immutability).

## 13. Frontend PDF Viewer

**Decision**: Reuse drawer pattern from screendesign — iframe `pdfUrl` or blob from `/pdf` endpoint; Print via `window.print()` on iframe; Download via existing blob helper in `contractApi.ts`.

**Rationale**: spec print = browser; pages exist.

**Alternatives considered**: Dedicated PDF.js component — optional enhancement.

## 14. Outbox Worker Scheduling

**Decision**: `@Scheduled(fixedDelay = 15000)` single worker polls `status=PENDING` ORDER BY created_at LIMIT 10; max 5 retries then FAILED + alert log.

**Rationale**: spec assumption 15s poll, 5 retries.

**Alternatives considered**: Kafka — out of scope v1.

## 15. Pending Generation Status

**Decision**: Optional enum value `GENERATING` on contract before pdfUrl set; or no row until success (outbox-only pending). **Choose**: no contract row until PDF success — simpler idempotency.

**Rationale**: spec edge case "not Active until PDF ready".

**Alternatives considered**: GENERATING row — more complex UI.
