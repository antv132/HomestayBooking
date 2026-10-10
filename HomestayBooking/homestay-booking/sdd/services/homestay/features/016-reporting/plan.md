# Implementation Plan: FR-16 Reporting

**Branch**: `018-reporting` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/016-reporting/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-16, §8 Reporting acceptance), `docs/api-spec-by-screen.md` (SCR-27/44/45/55), `docs/screen.md`, `docs/screendesign.md`, `docs/entity-ui-mapping.md` §1.17, `docs/component-library.md`, frontend `reportApi.ts`, `managerApi.ts`, `ManagerDashboardPage.tsx`, `RevenueReportPage.tsx`, `OccupancyReportPage.tsx`, `ReportsDashboardPage.tsx`

**Phụ thuộc**: FR-01 (RBAC); FR-06 (Manager property scope); FR-07 (floors count); FR-08 (rooms/status); FR-04 (bookings); FR-12 (payments PAID); FR-09 (customer counts — optional). **Ranh giới**: FR-16 owns **read-only** report aggregation APIs + dashboard/report UI wiring; **không** CRUD source entities; không hệ thống kế toán (§9); Employee/Customer excluded v1.

## Summary

Triển khai **FR-16 Reporting**: service layer aggregate từ `payments`, `bookings`, `rooms`, `properties`, `floors` — **không** bảng báo cáo riêng v1; optional Flyway **V032** indexes; REST `/api/v1/reports/**` (Manager) + `/api/v1/admin/reports/**` (Admin); property scope qua FR-06; frontend migrate `reportApi.ts` + `managerApi.ts` → `/api/v1/**`, wire `OccupancyReportPage` + `BookingTrendPage`, thêm Admin Dashboard (SCR-45) + Global Reports (SCR-55); CSV export client-side (P2) đã có trên `RevenueReportPage`.

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA (native/JPQL aggregates), Bean Validation, Recharts (frontend)  
**Storage**: SQL Server — read-only queries on FR-04/06/07/08/12 tables; optional V032 reporting indexes  
**Testing**: JUnit 5 + Mockito; `RevenueReportServiceTest`; `OccupancyReportServiceTest`; `ReportControllerIT` RBAC + revenue accuracy  
**Target Platform**: Web application  
**Project Type**: `Frontend/` + `Backend/`  
**Performance Goals**: KPI p95 < 3s; report range ≤12 months p95 < 5s  
**Constraints**: Manager property scope; max date range 366 days; timezone Asia/Ho_Chi_Minh; read-only  
**Scale/Scope**: ~8 REST endpoints; 5 user stories; SCR-27/44/45/55 + manager reports hub

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture | PASS | Controllers → Report*Service → repositories |
| DTO + Bean Validation | PASS | Date range, groupBy, propertyId validation |
| Security-first (RBAC, scope) | PASS | Manager PropertyScopeService; Admin global |
| No secrets in code | PASS | N/A for read reports |
| Test coverage ≥80% | PASS | Revenue accuracy + scope denial IT |
| Standard API envelope | PASS | api-spec §1 |
| Audit log | PASS | Optional REPORT_VIEWED P2 — omit v1 read-only |
| Pagination N/A | PASS | Aggregates bounded by date range |

**Post-design re-check**: PASS — no write paths; indexes optional not required for MVP.

## Project Structure

### Documentation (this feature)

```text
specs/016-reporting/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/reporting-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   ├── ReportController.java              # Manager SCR-27/44 — /reports/**
│   └── AdminReportController.java         # Admin SCR-45/55 — /admin/reports/**, /reports/global-kpis
├── dtos/report/
│   ├── PropertyKpisResponse.java
│   ├── GlobalKpisResponse.java
│   ├── RevenueReportResponse.java
│   ├── OccupancyReportResponse.java
│   ├── BookingTrendReportResponse.java
│   └── PropertyReportSummaryResponse.java
├── repositories/
│   └── ReportQueryRepository.java         # native aggregate queries (or extend Payment/Booking repos)
├── services/
│   ├── ReportPropertyScopeValidator.java  # FR-06 manager assignment check
│   ├── RevenueReportService.java
│   ├── OccupancyReportService.java
│   ├── BookingTrendReportService.java
│   ├── PropertyKpisService.java             # SCR-27
│   └── GlobalKpisService.java               # SCR-45
└── configs/
    └── SecurityConfig.java                  # MANAGER /reports/**, ADMIN /admin/reports/**

Backend/src/main/resources/db/migration/
└── V032__reporting_indexes_fr16.sql       # optional performance indexes

Backend/src/test/java/com/homestay/
├── unit/RevenueReportServiceTest.java
├── unit/OccupancyReportServiceTest.java
└── integration/ReportControllerIT.java

Frontend/src/
├── api/reportApi.ts                         # migrate → /api/v1/reports/**
├── api/managerApi.ts                        # dashboard → property-kpis or unified reports
├── pages/manager/
│   ├── ManagerDashboardPage.tsx             # SCR-27 — wire property-kpis + charts
│   ├── ReportsDashboardPage.tsx             # hub SCR-44
│   ├── RevenueReportPage.tsx                # Revenue tab
│   ├── OccupancyReportPage.tsx              # replace mock → live API
│   └── BookingTrendReportPage.tsx           # new — booking trends tab
├── pages/admin/
│   ├── AdminDashboardPage.tsx               # SCR-45 — new
│   └── GlobalReportsPage.tsx                # SCR-55 — new
└── App.tsx                                  # /admin/dashboard, /admin/reports routes
```

**Structure Decision**: FR-16 **owns** report query services and REST contracts. **Consumes** FR-04 bookings, FR-12 payments, FR-06/07/08 property structure. Frontend report UI **partially exists** — replace mocks, align API paths, add Admin screens.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | Optional V032 reporting indexes | data-model.md |
| **B** | Report DTOs + `ReportPropertyScopeValidator` | Foundational |
| **C** | `RevenueReportService` + `GET /reports/revenue` | US-2, US-4 |
| **D** | `OccupancyReportService` + `GET /reports/occupancy` | US-2, US-4 |
| **E** | `BookingTrendReportService` + `GET /reports/booking-trends` | US-2, US-4 |
| **F** | `PropertyKpisService` + `GET /reports/property-kpis` | US-1 |
| **G** | `GlobalKpisService` + `GET /reports/global-kpis` | US-3 |
| **H** | `AdminReportController` revenue/year + property filter | US-4 |
| **I** | Frontend migration + Occupancy + BookingTrend pages | US-2 |
| **J** | Manager Dashboard + Admin Dashboard/Global Reports | US-1, US-3 |
| **K** | CSV export verify (client-side P2) | US-5 |
| **L** | Tests + quickstart | SC-001–SC-008 |

## Frontend Gap Analysis

| Item | Hiện tại | Target |
|------|----------|--------|
| Revenue API | `GET /api/reports/revenue` | `GET /api/v1/reports/revenue` |
| Manager dashboard | `GET /api/manager/dashboard` monolith | `GET /api/v1/reports/property-kpis` + chart endpoints or composite |
| Occupancy page | hardcoded `PROPERTIES_OCC` mock | `GET /api/v1/reports/occupancy` |
| Booking trends | route points to `ReportsDashboardPage` stub | `BookingTrendReportPage.tsx` + API |
| Admin dashboard | **missing** | `AdminDashboardPage` SCR-45 |
| Admin global reports | **missing** | `GlobalReportsPage` SCR-55 |
| Property selector | `RevenueReportPage` uses `propertyApi` | reuse on all report pages |
| CSV export | client-side on Revenue | verify after live data (US-5) |

## Risks

| Risk | Mitigation |
|------|------------|
| FR-12 payments not implemented | Blocker — seed PAID payments for dev |
| Slow aggregates on large data | V032 indexes; max 366-day range |
| Manager dashboard monolith API | Phase J: refactor `managerApi` to call report services |
| Occupancy formula disputes | Document room-nights algo in research.md; unit test |
| api-spec SCR-44 minimal | Extend contract with revenue/occupancy/trend endpoints |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/reporting-api.yaml](./contracts/reporting-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
