# Implementation Plan: FR-08 Room Management

**Branch**: `008-room-management` | **Date**: 2026-07-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `sdd/services/homestay/features/008-room-management/spec.md`

**Clarifications**: Session 2026-07-23 — soft-delete `deletedAt`; roomNumber change always if unique; gallery max 10 × jpeg/png/webp × 10MB; create always AVAILABLE; floor change Occupied OK same property.

**Nguồn docs**: `docs/Specification_v2.md` FR-08 §5 Room/RoomImage; api-spec SCR-29–32, SCR-65; frontend `RoomListPage`, `AddRoomPage`, `EditRoomPage`, `RoomGalleryPage`, `roomsApi.ts`

**Phụ thuộc**: FR-06 (property scope); FR-07 (floor). **Ranh giới**: FR-05 status PATCH; FR-04 booking lifecycle; FR-03 guest discovery read.

## Summary

Hoàn thiện **FR-08 Room Management**: Manager CRUD phòng + gallery trong property được gán; Employee list read-only; **soft-delete** khi không còn booking blocking; validate gallery (MIME + 10MB + max 10 ảnh); default status AVAILABLE; unique room number per property. Stack cố định: **Java 17 + Spring Boot 3 + JPA + SQL Server** + **React 18 + TS + Vite**. Phần lớn CRUD/gallery đã có trên `/api/v1/manager/rooms` — plan tập trung **gap closure** (v1 DELETE soft-delete, fix delete guard, MIME/count caps, FE delete, tests).

## Technical Context

**Language/Version**: Java 17, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, Bean Validation; Vite, Axios, TanStack Query, Zustand, Tailwind  
**Storage**: SQL Server — `rooms`, `room_images`; read `floors`, `properties`, `bookings` (delete guard)  
**Testing**: JUnit 5 + Mockito (`RoomService*Test`); FE lint; Postman/curl smoke per quickstart  
**Target Platform**: Web (Manager / Employee SPA + REST API)  
**Project Type**: `Backend/` + `Frontend/` (Homestay Booking)  
**Performance Goals**: List filter p95 &lt; 3s for ≤500 rooms/property (SC-004); create visible in list &lt; 5s (SC-003)  
**Constraints**: Property-scoped RBAC; upload MIME jpeg/png/webp + 10MB (constitution IV); soft-delete only (no hard-delete with history); no FR-05 status PATCH in this feature; Asia/Ho_Chi_Minh not required for room CRUD timestamps  
**Scale/Scope**: ~9 manager/employee endpoints (extend DELETE + upload gates); 6 user stories; SCR-29–32, SCR-65

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*  
*Source: `.specify/memory/constitution.md` v1.0.0*

- [x] **I. Spec-First**: `CONTEXT.md` + `spec.md` present; clarifications 2026-07-23 encoded; Spec Sync assumed for Sprint 2 FR-08
- [x] **II. Tech Stack**: Java 17 / Spring Boot 3 / SQL Server / React 18+TS+Vite / JWT / REST `/api/v1` — no stack swap
- [x] **III. Layered Boundaries**: `RoomV1Controller` / `RoomImageV1Controller` → `RoomService` → repositories; DTOs + `ApiResponse`
- [x] **IV. Security**: Property scope via `PropertyAccessValidator`; upload MIME+size; no secrets; no booking-status invention
- [x] **V. Tests**: Unit tests for soft-delete guard, duplicate room number, gallery caps; DoD from `AGENTS.md`
- [x] **Complexity**: No unjustified violations (see Complexity Tracking — empty)

**Post-design re-check**: PASS — soft-delete keeps FK integrity; gallery caps match constitution 10MB; delete guard uses canonical booking enum (not legacy `COMPLETED`).

## Project Structure

### Documentation (this feature)

```text
sdd/services/homestay/features/008-room-management/
├── CONTEXT.md
├── spec.md
├── plan.md              # this file
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/room-api.yaml
├── checklists/
└── tasks.md             # /speckit-tasks — NOT created by /speckit-plan
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controller/
│   ├── RoomV1Controller.java          # manager list/CRUD (+ DELETE soft)
│   ├── RoomImageV1Controller.java     # gallery upload/reorder/primary/delete
│   └── RoomController.java            # legacy /api/rooms — prefer v1
├── service/RoomService.java
├── repository/
│   ├── RoomRepository.java            # hasActiveBookings → blocking statuses
│   └── RoomImageRepository.java
├── entity/
│   ├── Room.java                      # + deletedAt
│   └── RoomImage.java
├── dto/request|response/              # Create/Update/Detail/Summary
├── config/SchemaColumnPatcher.java    # optional deleted_at column widen/add
└── security/PropertyAccessValidator.java

Backend/src/test/java/com/homestay/service/
└── RoomService*Test.java              # soft-delete, unique, gallery caps

Frontend/src/
├── api/roomsApi.ts                    # DELETE v1 + gallery errors
├── pages/manager/
│   ├── RoomListPage.tsx               # delete action + hide soft-deleted
│   ├── AddRoomPage.tsx
│   ├── EditRoomPage.tsx
│   └── RoomGalleryPage.tsx            # max 10 + MIME UX
└── pages/employee/…                   # SCR-65 read-only list
```

**Structure Decision**: Extend existing `RoomService` + v1 controllers; add `rooms.deleted_at`; fix repository delete guard; wire FE delete on SCR-29. Do not invent parallel room modules.

## Implementation Phases (gap-focused)

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | Schema: `rooms.deleted_at` (+ filter `deleted_at IS NULL` on manager/public lists) | FR-004, data-model |
| **B** | Fix `hasActiveBookings` → blocking statuses (`PENDING_DEPOSIT`…`PENDING_DAMAGE_PAYMENT`; exclude `CANCELLED`/`CHECKED_OUT`/`NO_SHOW`) | FR-004, research §3 |
| **C** | `DELETE /api/v1/manager/rooms/{id}` soft-delete + property scope; deprecate hard delete path | US-4, FR-004 |
| **D** | Create: force `AVAILABLE`; `@NotBlank roomType`; service-level 409 duplicate room number | FR-005, FR-007 |
| **E** | Gallery: MIME jpeg/png/webp; 10MB; max 10 images/room before write | FR-012, LESSON-007 |
| **F** | Update: allow roomNumber/floor change (same property) even Occupied | FR-003, FR-007 |
| **G** | FE: delete confirm on SCR-29; gallery cap messaging; call v1 DELETE | US-4–5 |
| **H** | Unit tests + quickstart smoke | SC-002, SC-005, SC-007 |

## Frontend / Backend Gap Analysis

| Item | Hiện tại | Target (spec clarify) |
|------|----------|------------------------|
| Delete API | Legacy `DELETE /api/rooms/{id}` hard; **no** v1 DELETE | `DELETE /api/v1/manager/rooms/{id}` → soft-delete |
| Delete guard | `NOT IN ('CANCELLED','COMPLETED')` — wrong enum | Blocking statuses only; `CHECKED_OUT`/`NO_SHOW` allow soft-delete |
| Soft-delete column | Missing on `Room` | `deletedAt`; lists/discovery exclude non-null |
| Gallery MIME | Size via `UploadLimits` 10MB; **no** MIME check | jpeg/png/webp only |
| Gallery count | Unlimited | Max **10**/room |
| Create roomType | May lack `@NotBlank` | Required + AVAILABLE status |
| Duplicate room# | DB unique only | Service 409 Conflict |
| FE delete | Missing / legacy | Manager list action → v1 soft-delete |

## Complexity Tracking

> No constitution violations requiring justification.
