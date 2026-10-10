# Implementation Plan: FR-17 Administration

**Branch**: `019-administration` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/017-administration/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-17, §5 Complaint/SystemSetting/ActivityLog, §8 Administration), `docs/api-spec-by-screen.md` (SCR-54/56), `docs/screen.md`, `docs/screendesign.md`, `docs/entity-ui-mapping.md` §1.15, `docs/figma-generation-prompt.md` (SCR-56 tabs), frontend `complaintsApi.ts`, `CustomerComplaintPages.tsx`, `ComplaintListPage.tsx`, `ComplaintDetailPage.tsx`, `ActivityLogPage.tsx`, `ReviewMgmtPage.tsx`

**Phụ thuộc**: FR-01 (RBAC Admin; Customer auth); FR-02 (Customer display in complaint); FR-09 (**owns** Customer Directory SCR-51 — không duplicate); FR-14 (**owns** Review moderation APIs — FR-17 wires SCR-56 Content Moderation tab); FR-15 (optional Outbox FAILED tab SCR-56 P2). **Ranh giới**: FR-17 owns **Complaint** CRUD (Customer) + Admin resolve, **System Settings**, **Activity Logs read API** + shared `ActivityLogService` write helper; FR-18 Promotion out of scope.

## Summary

Triển khai **FR-17 Administration**: Flyway **V033** bảng `complaints`, `system_settings`, `activity_logs`; REST Customer `/api/v1/complaints` + Admin `/api/v1/admin/complaints`, `/api/v1/admin/settings`, `/api/v1/admin/activity-logs`; complaint workflow **OPEN → INVESTIGATING → RESOLVED → CLOSED** với `resolutionNotes` bắt buộc khi Resolved; System Settings key-value (deposit %, banking, system name, support email); Admin SCR-54 complaint UI chuẩn hóa từ `/manager/complaints` → `/admin/complaints`; SCR-56 hub tabs (Activity Logs, System Settings, Content Moderation → FR-14); migrate `complaintsApi.ts` → `/api/v1/**`.

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, Bean Validation; Vite, Axios, React Router  
**Storage**: SQL Server — `complaints`, `system_settings`, `activity_logs` (V033); seed default settings  
**Testing**: JUnit 5 + Mockito; `ComplaintServiceTest` status machine; `SystemSettingServiceTest`; `AdministrationControllerIT` RBAC + workflow  
**Target Platform**: Web application  
**Project Type**: `Frontend/` + `Backend/`  
**Performance Goals**: Complaint list p95 < 3s; settings GET/PUT p95 < 1s; activity logs 30-day filter p95 < 5s  
**Constraints**: ADMIN-only admin endpoints; Customer owns-complaint scope; Closed terminal state; deposit 10–50%  
**Scale/Scope**: ~10 REST endpoints; 6 user stories; SCR-54 + SCR-56 (+ customer complaint screens)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture | PASS | Controllers → *Service → repositories |
| DTO + Bean Validation | PASS | Complaint create/status, SettingsUpdateRequest |
| Security-first (RBAC) | PASS | `@PreAuthorize` ADMIN / CUSTOMER scope |
| No secrets in code | PASS | Bank info stored in DB settings, not env |
| Test coverage ≥80% | PASS | Status machine + RBAC IT |
| Standard API envelope | PASS | `{ success, message, data }` |
| Audit log | PASS | ActivityLog on complaint status + settings update |
| Pagination | PASS | Complaints + activity logs paginated |

**Post-design re-check**: PASS — FR-14 moderation delegated to existing contract; FR-09 customer directory linked not reimplemented.

## Project Structure

### Documentation (this feature)

```text
specs/017-administration/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/administration-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   ├── ComplaintController.java              # Customer SCR — POST/GET /complaints
│   └── AdminComplaintController.java         # Admin SCR-54 — list/detail/status
│   └── AdminSettingsController.java          # SCR-56 System Settings tab
│   └── AdminActivityLogController.java       # SCR-56 Activity Logs tab
├── dtos/administration/
│   ├── CreateComplaintRequest.java
│   ├── ComplaintSummaryResponse.java
│   ├── ComplaintDetailResponse.java
│   ├── UpdateComplaintStatusRequest.java
│   ├── SystemSettingsResponse.java
│   ├── UpdateSystemSettingsRequest.java
│   └── ActivityLogResponse.java
├── entities/
│   ├── Complaint.java
│   ├── SystemSetting.java
│   └── ActivityLog.java
├── enums/
│   └── ComplaintStatus.java
├── repositories/
│   ├── ComplaintRepository.java
│   ├── SystemSettingRepository.java
│   └── ActivityLogRepository.java
├── services/
│   ├── ComplaintService.java
│   ├── ComplaintStatusValidator.java
│   ├── SystemSettingService.java
│   ├── ActivityLogService.java               # shared write helper for all FRs
│   └── ActivityLogQueryService.java
└── configs/
    └── SecurityConfig.java                   # CUSTOMER /complaints/**, ADMIN /admin/**

Backend/src/main/resources/db/migration/
└── V033__administration_fr17.sql

Backend/src/test/java/com/homestay/
├── unit/ComplaintStatusValidatorTest.java
├── unit/SystemSettingServiceTest.java
└── integration/AdministrationControllerIT.java

Frontend/src/
├── api/
│   ├── complaintsApi.ts                      # migrate → /api/v1/complaints, /admin/complaints
│   ├── settingsApi.ts                        # new — GET/PUT /admin/settings
│   └── activityLogApi.ts                     # new — GET /admin/activity-logs
├── pages/customer/
│   └── CustomerComplaintPages.tsx            # wire live API (exists)
├── pages/admin/
│   ├── ComplaintListPage.tsx                 # move from manager/ — SCR-54
│   ├── ComplaintDetailPage.tsx               # drawer/detail — SCR-54
│   ├── SystemAdministrationPage.tsx          # SCR-56 tabs hub (new)
│   ├── SystemSettingsTab.tsx                 # SCR-56
│   ├── ActivityLogsTab.tsx                   # replace mock ActivityLogPage
│   └── ContentModerationTab.tsx              # wire FR-14 GET/PATCH /admin/reviews
└── App.tsx                                   # /admin/complaints, /admin/system routes
```

**Structure Decision**: FR-17 **owns** Complaint + SystemSetting persistence and Admin read path for ActivityLog. **Consumes** FR-14 for Content Moderation tab APIs. **Defers** Customer Directory to FR-09 (optional nav link from admin layout).

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | V033 migration + seed default SystemSettings | data-model.md |
| **B** | Entities, repos, `ActivityLogService` (shared) | Foundational |
| **C** | `ComplaintService` + Customer `ComplaintController` | US-1 |
| **D** | `AdminComplaintController` list/detail/status | US-2 |
| **E** | `SystemSettingService` + `AdminSettingsController` | US-3 |
| **F** | `ActivityLogQueryService` + `AdminActivityLogController` | US-4 |
| **G** | SCR-56 `SystemAdministrationPage` + Content Moderation tab (FR-14) | US-5, US-6 |
| **H** | Frontend route migration manager → admin complaints | US-2 |
| **I** | Optional Outbox tab link (FR-15 P2) | US-6 |
| **J** | Tests + quickstart | SC-001–SC-008 |

## Frontend Gap Analysis

| Item | Hiện tại | Target |
|------|----------|--------|
| Complaint API paths | `/api/manager/complaints`, `/api/complaints` | `/api/v1/complaints`, `/api/v1/admin/complaints` |
| Admin complaint routes | `/manager/complaints` (wrong actor/layout) | `/admin/complaints` SCR-54 + AdminLayout |
| Activity logs | `ActivityLogPage.tsx` mock `_sharedAdminData` | `GET /api/v1/admin/activity-logs` in SCR-56 tab |
| System settings | **missing** (manager nav stub only) | SCR-56 System Settings tab + `settingsApi.ts` |
| Content moderation | `ReviewMgmtPage` under manager | SCR-56 tab → FR-14 `/admin/reviews` |
| SCR-56 hub | separate pages | `SystemAdministrationPage.tsx` with tabs |
| Customer complaints | UI exists | wire `complaintsApi` to v1 endpoints |

## Risks

| Risk | Mitigation |
|------|------------|
| api-spec SCR-54 only PATCH `/resolve` | Extend to `PATCH /status` full workflow per spec; keep `resolution` alias optional |
| ActivityLog table not yet in other migrations | V033 creates table; `ActivityLogService` for cross-FR writes |
| Manager complaint pages mislabeled | Phase H: move to `pages/admin/`, update App.tsx + nav |
| FR-14 not implemented | Content Moderation tab blocked — stub with FR-14 dependency note |
| Deposit % used by FR-04/12 | `SystemSettingService.getDepositPercentage()` public for booking services |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/administration-api.yaml](./contracts/administration-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
