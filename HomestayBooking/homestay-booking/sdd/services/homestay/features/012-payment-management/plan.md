# Implementation Plan: FR-12 Payment Management & Reconciliation

**Branch**: `014-payment-management` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/012-payment-management/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-12, §5 Payment/PaymentReceipt, §6 RBAC, §8 VNPay), `docs/api-spec-by-screen.md` (SCR-16/20/26/36/37/52), `docs/screen.md`, `docs/screendesign.md`, `docs/entity-ui-mapping.md` §1.8, `specs/004-booking-inventory/contracts/booking-api.yaml`, frontend `paymentApi.ts`, `PaymentPages.tsx`, `PaymentMgmtListPage.tsx`, `PaymentMgmtVerificationPage.tsx`

**Phụ thuộc**: FR-11 (invoices + InvoiceStatusSyncService); FR-04 (booking lifecycle, DepositConfirmationService); FR-01 (auth); FR-06 (property scope); FR-10 (contract Outbox via deposit confirm); FR-15 (notifications/reminders); FR-17 (bank info display). **Ranh giới**: FR-11 owns Invoice; FR-12 owns Payment/VNPay/verify/reconciliation; FR-23 damage approve trigger — US6 P2.

## Summary

Triển khai **FR-12 Payment Management & Reconciliation**: full `payments` + `payment_receipts`; Customer deposit/remaining pay qua **VNPay** hoặc **bank transfer + receipt**; Manager verify SCR-37; **cron reconciliation 15 phút** + Admin SCR-52; payment history SCR-26/36. Stack: **Spring Boot 3 + VNPay SDK/HMAC + JPA** + **React/TypeScript** — migrate existing payment pages to `/api/v1`; wire `PaymentConfirmationService` → FR-11 invoice sync + FR-04 deposit confirm + FR-10 Outbox.

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, Flyway; VNPay HMAC-SHA512 (custom or vnpay-java lib); Spring `@Scheduled`  
**Storage**: SQL Server — `payments`, `payment_receipts` (FR-04 schema + V027 invoice_id + V028 extensions); read `invoices`, `bookings`  
**Testing**: JUnit 5 + Mockito; `VNPayServiceTest` signature; `PaymentConfirmationServiceTest` idempotency; `PaymentControllerIT` RBAC + IPN; WireMock VNPay API  
**Target Platform**: Web application  
**Project Type**: `Frontend/` + `Backend/`  
**Performance Goals**: IPN processing p95 < 30s; verify approve p95 < 10s; reconciliation batch < 2 min  
**Constraints**: UNIQUE order_ref; receipt required for approve; VNPay secrets from env; `@Transactional` confirmPaid; one Pending per invoice  
**Scale/Scope**: ~12 REST endpoints (+ public IPN); 6 user stories (US6 P2); SCR-16/20/26/36/37/52

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture | PASS | Controllers → Payment*Service / VNPayService → repositories |
| DTO + Bean Validation | PASS | VerifyPaymentRequest, CreateVNPayUrlResponse |
| Security-first (RBAC, VNPay signature) | PASS | IPN public + HMAC; scope Customer/Manager/Admin |
| No secrets in code | PASS | VNPAY_* from env only |
| Test coverage ≥80% | PASS | IPN idempotency + verify enforcement IT |
| Standard API envelope | PASS | api-spec §1 |
| Audit log PAYMENT_* | PASS | create, paid, verified, reconciled |
| Verify VNPay callback signature | PASS | FR-12 core |

**Post-design re-check**: PASS — confirmPaid single side-effect entry; FR-11 sync via InvoiceStatusSyncService only.

## Project Structure

### Documentation (this feature)

```text
specs/012-payment-management/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/payment-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   ├── CustomerPaymentController.java      # GET /payments/me, detail
│   ├── PaymentVNPayController.java         # create-url, ipn, return
│   ├── CustomerReceiptController.java      # POST /bookings/{id}/receipts
│   ├── ManagerPaymentController.java       # SCR-36/37 list/detail/verify
│   └── AdminReconciliationController.java  # SCR-52
├── dtos/payment/
│   ├── PaymentSummaryResponse.java
│   ├── PaymentDetailResponse.java
│   ├── PaymentPageResponse.java
│   ├── VerifyPaymentRequest.java
│   ├── CreateVNPayUrlResponse.java
│   └── UploadReceiptRequest.java
├── entities/
│   ├── Payment.java
│   └── PaymentReceipt.java
├── enums/
│   ├── PaymentType.java
│   ├── PaymentMethod.java
│   ├── PaymentStatus.java
│   └── ReconciliationStatus.java
├── repositories/
│   ├── PaymentRepository.java
│   └── PaymentReceiptRepository.java
├── services/
│   ├── PaymentService.java
│   ├── VNPayService.java
│   ├── PaymentConfirmationService.java     # confirmPaid idempotent
│   ├── PaymentVerificationService.java
│   ├── PaymentReceiptService.java
│   └── AdminReconciliationService.java
├── jobs/
│   ├── PaymentReconciliationJob.java       # cron 15 min
│   └── PaymentReminderJob.java             # 24h bank transfer
├── configs/
│   ├── VNPayConfig.java
│   └── SecurityConfig.java                 # permit IPN/return
└── listeners/
    └── DamagePaymentListener.java          # P2 stub FR-23

Backend/src/main/resources/db/migration/
├── V027__payments_invoice_id.sql           # shared FR-11
└── V028__payments_fr12_extensions.sql

Backend/src/test/java/com/homestay/
├── unit/VNPayServiceTest.java
├── unit/PaymentConfirmationServiceTest.java
├── unit/PaymentVerificationServiceTest.java
└── integration/PaymentControllerIT.java

Frontend/src/
├── api/paymentApi.ts                       # migrate → /api/v1/**
├── pages/customer/
│   ├── PaymentPages.tsx                    # deposit/remaining/VNPay result
│   └── PaymentHistoryPage.tsx              # SCR-26
└── pages/manager/
    ├── PaymentMgmtListPage.tsx             # SCR-36
    ├── PaymentMgmtVerificationPage.tsx     # SCR-37
    └── PaymentMgmtDetailPage.tsx
└── pages/admin/
    └── PaymentReconciliationPage.tsx       # SCR-52 (new)
```

**Structure Decision**: FR-12 **owns** payment tables and VNPay; **consumes** FR-11 invoices; **delegates** deposit confirm to FR-04 `DepositConfirmationService`. Frontend payment UI **exists** — migrate API paths + add Admin reconciliation page.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | Flyway V027/V028 payments schema | data-model.md |
| **B** | Payment/PaymentReceipt entities, repos, DTOs, VNPayConfig | Foundational |
| **C** | PaymentService + VNPayService create-url/IPN | US-1, US-2 |
| **D** | PaymentConfirmationService + InvoiceStatusSync + DepositConfirm | US-1, FR-005 |
| **E** | PaymentReceiptService + SCR-20 upload | US-1, US-2 |
| **F** | PaymentVerificationService + Manager controller | US-3 |
| **G** | PaymentReconciliationJob + AdminReconciliationService | US-4 |
| **H** | PaymentReminderJob + FR-15 notification | US-3, FR-013 |
| **I** | Customer payment history APIs + frontend migration | US-5 |
| **J** | DamagePaymentListener stub | US-6 P2 |
| **K** | Tests + quickstart | SC-001–SC-008 |

## Frontend Gap Analysis

| Item | Hiện tại | Target |
|------|----------|--------|
| VNPay URL | `/api/payments/vnpay/create-url` | `POST /api/v1/payments/vnpay/create-url` |
| Manager list | `/api/manager/payments` | `GET /api/v1/manager/payments?propertyId=` |
| Verify | POST verify `{status: PAID\|FAILED}` | PATCH `{status: APPROVED\|REJECTED}` |
| Customer history | mock/static | `GET /api/v1/payments/me` |
| Admin SCR-52 | missing page | `PaymentReconciliationPage.tsx` + route |
| VNPay result | `/customer/payments/vnpay-result` | keep; align query params |

## Risks

| Risk | Mitigation |
|------|------------|
| FR-11 invoices missing | Blocker — implement FR-11 US1 first |
| VNPay IPN unreachable locally | ngrok + quickstart docs |
| FR-04 DepositConfirmationService missing | Implement stub in FR-12 or FR-04 task T068 |
| Duplicate Pending per invoice | App check + unique partial index optional |
| Frontend verify POST vs PATCH | Update paymentApi + backend accept both v1 transition |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/payment-api.yaml](./contracts/payment-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
