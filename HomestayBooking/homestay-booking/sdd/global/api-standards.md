# api-standards.md — Cross-Service API Design Conventions
# Version: 1.0 | Updated: 2026-07-23
# Purpose: REST naming, pagination, error format, versioning
# Related: `sdd/global/system-architecture.md`, `sdd/global/security.md`, `AGENTS.md`, OpenAPI/Swagger

---

## 1. Scope

Conventions for the Homestay **REST API** (single Spring Boot service + React clients). “Cross-service” here means **stable contracts between SPA modules and the API**, and consistency across domain controllers — not a multi-repo microservice fleet.

**Examples of rules this doc owns:** REST resource naming, `/api/v1` preference, `PageResponse` pagination, `ApiResponse` error envelope, HTTP status mapping.

---

## 2. Base URL & versioning

| Rule | Convention |
|------|------------|
| Prefix | `/api/...` |
| Preferred | `/api/v1/{resource}` for new or dual-maintained endpoints |
| Legacy | Older `/api/{resource}` may exist — **prefer v1** when both exist |
| Role areas | `/api/v1/manager/...`, `/api/v1/employees/...`, `/api/admin/...`, `/api/v1/bookings/...` |
| Auth | `/api/auth/...` and `/api/v1/auth/...` |
| Public read | `/api/public/...`, selected `GET /api/v1/rooms/**`, etc. |
| Static | `/uploads/**` (not JSON API) |

Document new public matchers in `security.md` / `SecurityConfig` when adding permitAll routes.

---

## 3. REST naming

| Rule | Do | Don't |
|------|-----|--------|
| Resources | Plural nouns: `/bookings`, `/rooms`, `/properties` | Verbs in path: `/getBooking` |
| Nested | `/api/v1/manager/properties/{id}/rooms` when hierarchy is clear | Deep nests beyond ~2 levels without need |
| Actions | Prefer POST sub-resource or clear verb sparingly: `/check-in`, `/cancel` when state transition | Encode business action only in unclear PATCH bodies |
| IDs | UUID path params: `/bookings/{bookingId}` | Sequential guessable IDs as security boundary |
| Query | Filters as query: `?status=&page=&size=` | Body on GET |
| Case | kebab-case in URLs | camelCase paths |
| JSON fields | camelCase in JSON bodies | snake_case in JSON (DB remains snake_case) |

**HTTP methods**

| Method | Use |
|--------|-----|
| `GET` | Read (safe, idempotent) |
| `POST` | Create or non-idempotent action |
| `PUT` / `PATCH` | Update (prefer PATCH for partial) |
| `DELETE` | Remove / soft-delete per domain rules |

Controllers return `ResponseEntity<ApiResponse<T>>`. Business logic stays in services.

---

## 4. Response envelope

Success and failure use the same wrapper (`com.homestay.dto.response.ApiResponse`).

```json
{
  "success": true,
  "message": "Success",
  "data": { }
}
```

Error:

```json
{
  "success": false,
  "message": "Human-readable error",
  "data": null
}
```

Validation errors may put field map in `data`:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "email": "must not be blank",
    "checkOutDate": "must be after check-in"
  }
}
```

**Never** return JPA entities; map to response DTOs in the service (or static factory).

---

## 5. Pagination

List endpoints that can grow MUST paginate via Spring `Pageable` / query params and `PageResponse<T>`.

**Query**

| Param | Meaning | Default guidance |
|-------|---------|------------------|
| `page` | Zero-based page index | `0` |
| `size` | Page size | Reasonable cap (e.g. 20); reject absurd sizes in controller/service |
| `sort` | Optional; **whitelist** fields server-side | Do not pass raw column names from client unchecked |
| filters | `status`, `keyword`, `search`, dates… | Domain-specific |

**Body (`PageResponse`)**

```json
{
  "content": [ ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

Wrapped as usual: `{ "success": true, "message": "Success", "data": { /* PageResponse */ } }`.

FE types: mirror in `*Api.ts` (`PageResponse<T>`).

---

## 6. Error format & HTTP status

Centralized in `GlobalExceptionHandler`. Clients key off **HTTP status** + `success: false` + `message`.

| Situation | HTTP | Exception / source |
|-----------|------|--------------------|
| Bean validation / bad input | **400** | `@Valid`, `BusinessException`, `IllegalArgumentException`, type mismatch |
| Unauthenticated / bad-expired JWT | **401** | `JwtAuthenticationEntryPoint` |
| Forbidden / wrong role / scope | **403** | `ForbiddenException`, `AccessDeniedException`, account not verified |
| Missing resource | **404** | `ResourceNotFoundException` |
| Conflict (duplicate, overlap, integrity) | **409** | `ConflictException`, `DataIntegrityViolationException` |
| OTP / resource gone | **410** | `OtpExpiredException` |
| Unhandled | **500** | Generic handler — log server-side; generic client message |

Do not leak stack traces or SQL details to clients.

---

## 7. Auth headers & content types

```http
Authorization: Bearer <access_token>
Content-Type: application/json
Accept: application/json
```

Multipart uploads: `multipart/form-data` with validated parts (see `security.md` upload policy).

---

## 8. Idempotency & concurrency (domain)

| Concern | Convention |
|---------|------------|
| Booking create | Server enforces room lock + date overlap — client retries must handle **409** |
| Payment return | VNPay callback verify is server-authoritative; treat duplicate returns safely |
| Optimistic UI | FE may refetch via React Query; do not invent client-only inventory locks |

---

## 9. Documentation

- New/changed endpoints: update **OpenAPI / Swagger** annotations or equivalent.
- Prefer describing request/response DTOs, auth role, and error cases in feature `spec.md` / quickstart.
- Breaking changes: add `/api/v1` (or next version) rather than silently changing legacy contracts when clients still depend on them.

---

## 10. Checklist (new endpoint)

- [ ] Path follows resource naming + correct prefix (`/api/v1/...` preferred)
- [ ] Returns `ApiResponse<T>` / `ApiResponse<PageResponse<T>>`
- [ ] Auth: public vs `@PreAuthorize` + ownership/scope
- [ ] Validation on request DTO
- [ ] Correct HTTP statuses for error paths
- [ ] OpenAPI updated
- [ ] FE `*Api.ts` types updated (no `any`)

---

## 11. Related documents

| Doc | Role |
|-----|------|
| `sdd/global/system-architecture.md` | Topology & sync/async patterns |
| `sdd/global/security.md` | Who may call which routes |
| `sdd/global/data-governance.md` | What PII may appear in `data` |
| `AGENTS.md` | Layering & DoD |
| Feature `sdd/services/homestay/features/*/spec.md` | Endpoint-level FR |
