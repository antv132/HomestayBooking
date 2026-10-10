# Implementation Plan: FR-10 Contract Management

**Branch**: `012-contract-management` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/010-contract-management/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-10, §5 Contract/OutboxEvent), `docs/api-spec-by-screen.md` (SCR-21, SCR-38), `docs/screen.md`, `docs/screendesign.md`, `docs/entity-ui-mapping.md` §1.7, frontend `contractApi.ts`, `ContractListPage.tsx`, `ContractMgmtListPage.tsx`, `ContractMgmtDetailPage.tsx`, `ResendContractPage.tsx`

**Phụ thuộc**: FR-04 (CONFIRMED + `CONTRACT_GENERATE_REQUESTED` outbox); FR-12 (deposit success); FR-01 (Customer email); FR-06/08 (property/room snapshot). **Ranh giới**: FR-11 Billing; FR-12 payment verify UI; FR-15 notification transport; Addendum P2 tied to FR-23 damage.

## Summary

Triển khai **FR-10 Contract Management**: bảng `contracts` (+ optional `contract_addendums` P2); Outbox worker xử lý `CONTRACT_GENERATE_REQUESTED` → PDF immutable + email; Customer SCR-21 list/view/download/print; Manager SCR-38 list/resend scoped by property. Stack: **Spring Boot 3 + OpenPDF/PDFBox + JavaMail** + **React/TypeScript** — wire existing contract pages; migrate `/api/contracts` → `/api/v1/contracts/me` và `/api/v1/manager/contracts`.

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, OpenPDF (or Apache PDFBox); Spring `@Scheduled` Outbox processor; JavaMailSender; Flyway  
**Storage**: SQL Server — `contracts` (V024), `contract_addendums` (V025 P2); read `outbox_events` (FR-04 V023), `bookings`, `users`, `rooms`, `properties`  
**Testing**: JUnit 5 + Mockito; `ContractServiceTest` idempotency + status sync; `ContractOutboxWorkerTest`; `ContractControllerIT` RBAC + PDF download  
**Target Platform**: Web application  
**Project Type**: `Frontend/` + `Backend/`  
**Performance Goals**: Outbox process p95 < 5 min from CONFIRMED; PDF download p95 < 2s  
**Constraints**: One contract per booking UNIQUE; immutable pdfUrl; resend no regenerate; property scope on manager reads; `@Transactional` outbox mark PROCESSED  
**Scale/Scope**: ~8 REST endpoints; 4 user stories (US4 P2); SCR-21, SCR-38

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture | PASS | Controllers → ContractService / ContractOutboxWorker → repositories |
| DTO + Bean Validation | PASS | ContractSummaryResponse, ResendContractRequest |
| Security-first (RBAC, scope) | PASS | Customer own; Manager property scope |
| No secrets in code | PASS | SMTP + upload dir from env |
| Test coverage ≥80% | PASS | Idempotency + RBAC IT |
| Standard API envelope | PASS | api-spec §1 |
| Audit log CONTRACT_* | PASS | generate, email sent, resend |

**Post-design re-check**: PASS — FR-04 only writes outbox; FR-10 owns worker + PDF; addendum isolated from parent pdfUrl.

## Project Structure

### Documentation (this feature)

```text
specs/010-contract-management/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/contract-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   ├── CustomerContractController.java    # SCR-21 /contracts/me, /contracts/{id}/pdf
│   └── ManagerContractController.java     # SCR-38 /manager/contracts/**
├── dtos/contract/
│   ├── ContractSummaryResponse.java
│   ├── ContractDetailResponse.java
│   ├── ContractPageResponse.java
│   ├── ContractAddendumResponse.java      # P2
│   └── ResendContractRequest.java
├── entities/
│   ├── Contract.java
│   └── ContractAddendum.java              # P2
├── enums/ContractStatus.java              # ACTIVE, COMPLETED, CANCELLED
├── repositories/
│   ├── ContractRepository.java
│   └── ContractAddendumRepository.java    # P2
├── services/
│   ├── ContractService.java               # list, detail, download, resend, status sync
│   ├── ContractPdfService.java            # HTML→PDF snapshot generation
│   ├── ContractEmailService.java          # send + resend via SMTP
│   └── ContractOutboxWorker.java          # poll outbox CONTRACT_* events
├── templates/contract/
│   └── accommodation-contract.html        # Thymeleaf/Freemarker template v1
└── configs/SecurityConfig.java

Backend/src/main/resources/db/migration/
├── V024__contracts.sql
└── V025__contract_addendums.sql            # P2 optional

Backend/src/test/java/com/homestay/
├── unit/ContractServiceTest.java
├── unit/ContractOutboxWorkerTest.java
└── integration/ContractControllerIT.java

Frontend/src/
├── api/contractApi.ts                     # migrate → /api/v1/contracts/me, /manager/contracts
├── pages/customer/ContractListPage.tsx    # SCR-21
├── pages/manager/
│   ├── ContractMgmtListPage.tsx           # SCR-38
│   ├── ContractMgmtDetailPage.tsx
│   └── ResendContractPage.tsx
└── components/contract/
    └── ContractPdfDrawer.tsx              # iframe viewer + print/download
```

**Structure Decision**: FR-10 **owns** `contracts` table; **consumes** FR-04 `outbox_events`. Frontend contract pages **exist** — primary work is backend worker + API path migration.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | Flyway V024 contracts (+ V025 addendum P2) | data-model.md |
| **B** | Contract entity, repositories, DTOs | Foundational |
| **C** | ContractPdfService + template + local storage | US-1 |
| **D** | ContractOutboxWorker: GENERATE + EMAIL | US-1, FR-001–004 |
| **E** | ContractEmailService + resend outbox | US-1, US-3, FR-002, FR-009 |
| **F** | CustomerContractController list/detail/pdf | US-2, FR-005–006 |
| **G** | ManagerContractController list/detail/resend | US-3, FR-007–009 |
| **H** | Booking status → contract status sync | FR-011 |
| **I** | Frontend migration + PdfDrawer | US-2–3 |
| **J** | ContractAddendum worker (P2) | US-4, FR-010 |
| **K** | Tests + quickstart | SC-001–SC-007 |

## Frontend Gap Analysis

| Item | Hiện tại | Target |
|------|----------|--------|
| Customer list | `GET /api/contracts/my` | `GET /api/v1/contracts/me` |
| Manager list | `GET /api/contracts` | `GET /api/v1/manager/contracts?propertyId=` |
| Detail | `GET /api/contracts/{id}` | role-scoped GET same path under v1 |
| PDF download | `GET /api/contracts/{id}/pdf` | `/api/v1/contracts/{id}/pdf` or `/manager/contracts/{id}/pdf` |
| Resend | `POST /api/contracts/{id}/resend` | `POST /api/v1/manager/contracts/{id}/resend` |
| SCR-21 UX | table + pages | align drawer PDF viewer per screendesign |
| Booking link | `GET /api/contracts/booking/{id}` | keep as convenience endpoint |

## Risks

| Risk | Mitigation |
|------|------------|
| FR-04 outbox table missing | Blocker — implement FR-04 V023 first |
| SMTP not configured | Dev: MailHog; log pdfUrl in quickstart |
| PDF lib licensing | OpenPDF (LGPL) or PDFBox (Apache 2.0) |
| FR-23 damage not ready | Defer US4/V025; stub addendum interface |
| Duplicate contract race | UNIQUE booking_id + transactional idempotent check |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/contract-api.yaml](./contracts/contract-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
