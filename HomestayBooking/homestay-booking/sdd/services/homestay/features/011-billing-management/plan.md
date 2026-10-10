# Implementation Plan: FR-11 Billing Management

**Branch**: `013-billing-management` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/011-billing-management/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-11, §5 Booking/Payment), `docs/api-spec-by-screen.md` (SCR-18, SCR-26, SCR-35, SCR-36), `docs/screen.md`, `docs/screendesign.md` (Payment Breakdown), `docs/entity-ui-mapping.md` §1.8, frontend `paymentApi.ts`, `BookingPages.tsx`, `PaymentHistoryPage.tsx`, `PaymentMgmtListPage.tsx`

**Phụ thuộc**: FR-04 (booking create/modify/cancel, amount snapshot); FR-01 (auth); FR-06 (manager property scope). **Ranh giới**: FR-12 payment execution (VNPay, verify, receipts, reconciliation); FR-10 contract PDF; FR-23 damage-fee invoice — v1 chỉ DEPOSIT + REMAINING_BALANCE.

## Summary

Triển khai **FR-11 Billing Management**: bảng `invoices` (V026); tự động phát hành cặp hóa đơn **40% cọc + 60% còn lại** khi booking **Pending Deposit**; theo dõi trạng thái UNPAID → PENDING_PAYMENT → PAID / CANCELLED; embed **Payment Breakdown** trong SCR-18/SCR-35; danh sách billing SCR-26/SCR-36. Stack: **Spring Boot 3 + JPA + SQL Server** + **React/TypeScript** — mở rộng booking detail & payment history pages; FR-12 gọi `InvoiceStatusSyncService` khi Payment đổi trạng thái.

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, Flyway  
**Storage**: SQL Server — `invoices` (V026); alter `payments.invoice_id` (V027 shared với FR-12); read `bookings`, `rooms`, `properties`  
**Testing**: JUnit 5 + Mockito; `InvoiceIssuanceServiceTest` idempotency; `InvoiceStatusSyncServiceTest`; `InvoiceControllerIT` RBAC + breakdown  
**Target Platform**: Web application  
**Project Type**: `Frontend/` + `Backend/`  
**Performance Goals**: Invoice pair create p95 < 500ms trong cùng booking transaction; list p95 < 1s  
**Constraints**: UNIQUE (booking_id, type); deposit invoice immutable after PAID; property scope manager reads; `@Transactional` issue/cancel/adjust  
**Scale/Scope**: ~5 REST endpoints (+ booking detail extension); 4 user stories; SCR-18, SCR-26, SCR-35, SCR-36

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture | PASS | Controllers → Invoice*Service → repositories |
| DTO + Bean Validation | PASS | InvoiceSummaryResponse, InvoiceBreakdownResponse |
| Security-first (RBAC, scope) | PASS | Customer own; Manager property scope |
| No secrets in code | PASS | N/A for billing |
| Test coverage ≥80% | PASS | Idempotency + RBAC IT + sync unit tests |
| Standard API envelope | PASS | api-spec §1 `{ success, message, data }` |
| Audit log INVOICE_* | PASS | issued, paid, cancelled, adjusted |

**Post-design re-check**: PASS — Invoice owns billing state; FR-12 owns Payment; FR-04 hook documented for removing auto Payment insert on create.

## Project Structure

### Documentation (this feature)

```text
specs/011-billing-management/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/billing-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   ├── CustomerInvoiceController.java       # SCR-26 GET /invoices/me
│   └── ManagerInvoiceController.java        # SCR-36 GET /manager/invoices
├── dtos/invoice/
│   ├── InvoiceSummaryResponse.java
│   ├── InvoiceBreakdownResponse.java
│   ├── InvoicePageResponse.java
│   └── InvoiceDetailResponse.java
├── entities/
│   └── Invoice.java
├── enums/
│   ├── InvoiceType.java                     # DEPOSIT, REMAINING_BALANCE
│   └── InvoiceStatus.java                   # UNPAID, PENDING_PAYMENT, PAID, CANCELLED
├── repositories/
│   └── InvoiceRepository.java
├── services/
│   ├── InvoiceIssuanceService.java          # issuePair on booking create
│   ├── InvoiceAdjustmentService.java        # modify booking → remaining update
│   ├── InvoiceCancellationService.java      # cancel on booking cancel/timeout
│   ├── InvoiceStatusSyncService.java        # FR-12 payment hooks
│   └── InvoiceQueryService.java             # list/filter/breakdown
└── listeners/
    └── BookingInvoiceListener.java          # optional; or inline in BookingService

Backend/src/main/resources/db/migration/
├── V026__invoices.sql
└── V027__payments_invoice_id.sql

Backend/src/test/java/com/homestay/
├── unit/InvoiceIssuanceServiceTest.java
├── unit/InvoiceStatusSyncServiceTest.java
├── unit/InvoiceAdjustmentServiceTest.java
└── integration/InvoiceControllerIT.java

Frontend/src/
├── api/invoiceApi.ts                        # GET /invoices/me, manager list
├── api/bookingApi.ts                        # extend BookingDetail with invoiceBreakdown
├── pages/customer/
│   ├── BookingPages.tsx                     # SCR-18 breakdown → invoices[]
│   ├── BookingDetailPage.tsx
│   └── PaymentHistoryPage.tsx               # SCR-26 → invoiceApi
└── pages/manager/
    ├── BookingMgmtDetailPage.tsx            # SCR-35 Payment Breakdown
    └── PaymentMgmtListPage.tsx              # SCR-36 unpaid tab → manager/invoices
```

**Structure Decision**: FR-11 **owns** `invoices` table; **extends** FR-04 booking create/cancel/modify; **exposes** sync interface for FR-12. Frontend payment pages **exist** — primary work is invoice API + breakdown UI migration from legacy `payments[]` preview.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | Flyway V026 invoices + V027 payments.invoice_id | data-model.md |
| **B** | Invoice entity, enums, repository, DTOs | Foundational |
| **C** | InvoiceIssuanceService.issuePair in BookingService.create | US-1, FR-001–002 |
| **D** | InvoiceCancellationService on cancel/timeout | US-4, FR-007 |
| **E** | InvoiceAdjustmentService on booking modify | US-4, FR-003–004 |
| **F** | InvoiceStatusSyncService (FR-12 hook) | US-1, FR-006 |
| **G** | CustomerInvoiceController + extend booking detail | US-2, FR-008 |
| **H** | ManagerInvoiceController + extend manager booking detail | US-3, FR-009 |
| **I** | ActivityLog INVOICE_* events | FR-011 |
| **J** | Frontend invoiceApi + breakdown UI migration | US-2–3 |
| **K** | Tests + quickstart | SC-001–SC-007 |

## FR-04 Adjustment Note

FR-04 plan currently inserts `Payment DEPOSIT PENDING` on booking create. **FR-11 replaces this** with invoice pair only. Coordinate FR-04 task update:

- Remove: auto Payment insert on `createBooking`
- Add: call `InvoiceIssuanceService.issuePair(booking)` in same transaction

Payment rows created only when customer initiates pay (FR-12).

## Frontend Gap Analysis

| Item | Hiện tại | Target |
|------|----------|--------|
| SCR-18 breakdown | `booking.payments[]` inferred | `booking.invoiceBreakdown.invoices[]` |
| SCR-26 history | mock / payments API | `GET /api/v1/invoices/me` |
| SCR-35 breakdown | missing or payments link | `invoiceBreakdown` on manager booking detail |
| SCR-36 pending tab | `GET /api/manager/payments` | `GET /api/v1/manager/invoices?status=UNPAID` (+ FR-12 payments for verify) |
| paymentApi paths | `/api/manager/payments` | FR-12 migrates to `/api/v1/...`; FR-11 adds invoiceApi |

## Risks

| Risk | Mitigation |
|------|------------|
| FR-04 not updated (dual Payment+Invoice) | Document in tasks; block merge until Payment insert removed |
| FR-12 not ready for sync hook | Stub `InvoiceStatusSyncService`; manual test via unit tests |
| Frontend shows empty breakdown | Fallback read invoices from breakdown endpoint |
| Booking modify race | `@Transactional` + optimistic lock on booking row_version |
| Manager list performance | Denormalize property_id on invoice; index (property_id, status) |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/billing-api.yaml](./contracts/billing-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
