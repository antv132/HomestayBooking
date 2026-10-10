# Implementation Plan: FR-19 Customer Dashboard

**Branch**: `021-customer-dashboard` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/019-customer-dashboard/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-19), `docs/api-spec-by-screen.md` (SCR-15), `docs/screen.md`, `docs/screendesign.md`, `docs/figma-generation-prompt.md` (SCR-16 KPIs), frontend `CustomerDashboardPage.tsx`, `customersApi.ts`

**Phụ thuộc**: FR-01 (CUSTOMER auth); FR-04 (bookings active/upcoming); FR-12 (payments pending/recent); FR-13 (maintenance open count); FR-15 (notifications unread/recent); FR-02 (greeting name). **Ranh giới**: FR-19 owns **composite read** `GET /api/v1/customer/dashboard` + wire SCR-15 UI; **không** bảng mới; **không** CRUD; Damage Dispute alert **P2** (FR-23).

## Summary

Triển khai **FR-19 Customer Dashboard**: `CustomerDashboardService` aggregate từ `bookings`, `payments`, `maintenance_tickets`, `notifications` — **không** migration bắt buộc (optional V035 indexes); REST `GET /api/v1/customer/dashboard` trả `CustomerDashboardData` shape; frontend migrate `customersApi.ts` → `/api/v1/customer/dashboard`; wire `CustomerDashboardPage.tsx` (đã có ~90% UI) + **thêm section Recent Payments**; quick links + optional damage dispute banner P2.

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA (read queries); Vite, Axios, React Router  
**Storage**: SQL Server — read-only joins on FR-04/12/13/15 tables; optional V035 dashboard indexes  
**Testing**: JUnit 5 + Mockito; `CustomerDashboardServiceTest` KPI counts; `CustomerDashboardControllerIT` RBAC + scope  
**Target Platform**: Web application  
**Project Type**: `Frontend/` + `Backend/`  
**Performance Goals**: Dashboard composite p95 < 3s; single round-trip  
**Constraints**: CUSTOMER-only; customerId scope on all queries; timezone Asia/Ho_Chi_Minh  
**Scale/Scope**: 1 REST endpoint; 6 user stories; SCR-15

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture | PASS | Controller → CustomerDashboardService → repos |
| DTO + Bean Validation | PASS | Response DTO only; no write payloads |
| Security-first (RBAC) | PASS | `@PreAuthorize CUSTOMER`; customerId from JWT |
| No secrets in code | PASS | N/A |
| Test coverage ≥80% | PASS | KPI accuracy + scope IT |
| Standard API envelope | PASS | `{ success, message, data }` |
| Audit log | PASS | Read-only — omit v1 |
| Pagination | PASS | Bounded lists (5 bookings, 5 notifications, 3 payments) |

**Post-design re-check**: PASS — no new entities; aggregate service only.

## Project Structure

### Documentation (this feature)

```text
specs/019-customer-dashboard/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/customer-dashboard-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   └── CustomerDashboardController.java     # GET /api/v1/customer/dashboard
├── dtos/dashboard/
│   ├── CustomerDashboardResponse.java
│   ├── UpcomingEventDto.java
│   ├── PaymentSummaryDto.java
│   └── NotificationSummaryDto.java
├── services/
│   └── CustomerDashboardService.java        # aggregate KPIs + lists
└── configs/
    └── SecurityConfig.java                  # CUSTOMER /customer/dashboard

Backend/src/main/resources/db/migration/
└── V035__customer_dashboard_indexes_fr19.sql   # optional

Backend/src/test/java/com/homestay/
├── unit/CustomerDashboardServiceTest.java
└── integration/CustomerDashboardControllerIT.java

Frontend/src/
├── api/
│   └── customersApi.ts                      # → /api/v1/customer/dashboard
├── pages/customer/
│   └── CustomerDashboardPage.tsx            # wire live + add Recent Payments section
└── App.tsx                                  # /customer/dashboard (exists)
```

**Structure Decision**: FR-19 **owns** composite dashboard service + single endpoint. **Consumes** FR-04/12/13/15 repositories — no duplicate CRUD.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | Optional V035 indexes on booking/payment/notification queries | data-model.md |
| **B** | Dashboard DTOs + `CustomerDashboardService` | Foundational |
| **C** | `CustomerDashboardController` + security | US-1–US-5 |
| **D** | Frontend `customersApi` migration + wire page | US-1–US-5 |
| **E** | Add Recent Payments UI section | US-5 |
| **F** | Quick links verify + Damage dispute banner P2 | US-6 |
| **G** | Tests + quickstart | SC-001–SC-007 |

## Frontend Gap Analysis

| Item | Hiện tại | Target |
|------|----------|--------|
| API path | `/api/customers/dashboard` | `/api/v1/customer/dashboard` |
| KPI row | UI exists | wire live counts |
| Check-in/out cards | UI exists | wire `upcomingCheckIn/Out` |
| Upcoming bookings | UI exists | wire `upcomingBookings` |
| Notifications sidebar | UI exists | wire `recentNotifications` |
| Recent payments | **missing section** | add 3-item list + link SCR-23 |
| Damage dispute banner | missing | P2 FR-23 optional field |
| Page shell | `CustomerDashboardPage.tsx` complete | migrate API only + payments section |

## Risks

| Risk | Mitigation |
|------|------------|
| FR-04/12/13/15 not implemented | Blocker — seed data per dependency quickstarts |
| N+1 queries on dashboard | Single service method; optional V035 indexes |
| api-spec minimal JSON | Extend contract to full `CustomerDashboardData` |
| Slow composite | One endpoint; parallel repo calls or single optimized query |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/customer-dashboard-api.yaml](./contracts/customer-dashboard-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
