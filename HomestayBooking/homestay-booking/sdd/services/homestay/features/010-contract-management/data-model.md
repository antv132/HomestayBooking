# Data Model: FR-10 Contract Management

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `docs/Specification_v2.md` §5 Contract, OutboxEvent

## Scope

FR-10 **owns** `contracts` and (P2) `contract_addendums`. **Consumes** `outbox_events` (FR-04). Reads `bookings`, `users`, `rooms`, `properties` at generation time for snapshot.

## ERD

```text
Booking 1──0..1 Contract 1──* ContractAddendum (P2)
OutboxEvent ──triggers──> Contract generation / email / resend
User (Customer) 1──* Contract
Room 1──* Contract (snapshot roomId)
```

## Table: contracts

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| booking_id | UUID | FK bookings UNIQUE NOT NULL | One contract per booking |
| customer_id | UUID | FK users NOT NULL | |
| room_id | UUID | FK rooms NOT NULL | Snapshot ref |
| property_id | UUID | FK properties NOT NULL | Denormalized for manager filter |
| check_in_date | DATE | NOT NULL | Snapshot |
| check_out_date | DATE | NOT NULL | Snapshot |
| deposit_amount | DECIMAL(15,2) | NOT NULL | |
| total_amount | DECIMAL(15,2) | NOT NULL | |
| customer_name | VARCHAR(255) | NOT NULL | Denormalized snapshot |
| customer_email | VARCHAR(255) | NOT NULL | Email target |
| room_number | VARCHAR(32) | NOT NULL | Snapshot |
| property_name | VARCHAR(255) | NOT NULL | Snapshot |
| pdf_url | VARCHAR(1024) | NOT NULL | Immutable after create |
| generated_at | TIMESTAMPTZ | NOT NULL | |
| sent_at | TIMESTAMPTZ | nullable | Last email sent |
| status | VARCHAR(20) | NOT NULL DEFAULT 'ACTIVE' | ACTIVE, COMPLETED, CANCELLED |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**:

- `UNIQUE (booking_id)`
- `(customer_id, status)`
- `(property_id, created_at DESC)`

### ContractStatus enum

`ACTIVE` | `COMPLETED` | `CANCELLED`

### Status transitions

```text
(generate on deposit)     → ACTIVE
booking CHECKED_OUT       → COMPLETED
booking CANCELLED/NO_SHOW → CANCELLED
```

**Immutability**: `pdf_url` and amount/date snapshot columns MUST NOT update after insert (application enforced).

## Table: contract_addendums (P2)

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| parent_contract_id | UUID | FK contracts NOT NULL | |
| booking_id | UUID | FK bookings NOT NULL | |
| damage_fee_amount | DECIMAL(15,2) | NOT NULL | |
| pdf_url | VARCHAR(1024) | NOT NULL | Separate PDF |
| generated_at | TIMESTAMPTZ | NOT NULL | |
| sent_at | TIMESTAMPTZ | nullable | |
| created_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**: `(parent_contract_id)`

## Outbox integration (FR-04 owned table)

FR-10 **processes** (does not own schema):

| event_type | payload | Handler |
|------------|---------|---------|
| CONTRACT_GENERATE_REQUESTED | `{ bookingId }` | ContractOutboxWorker.generate |
| CONTRACT_EMAIL_SEND | `{ contractId }` | ContractEmailService.send |
| CONTRACT_RESEND | `{ contractId, email? }` | ContractEmailService.resend |

## API DTOs

### ContractSummaryResponse (list)

```json
{
  "id": "uuid",
  "bookingId": "uuid",
  "customerName": "...",
  "customerEmail": "...",
  "roomNumber": "101",
  "propertyName": "...",
  "checkInDate": "2026-07-01",
  "checkOutDate": "2026-07-03",
  "depositAmount": 400000,
  "totalAmount": 1000000,
  "status": "ACTIVE",
  "generatedAt": "...",
  "sentAt": "..."
}
```

### ContractDetailResponse

Extends summary + `customerPhone`, `pdfUrl`, `addendums[]` (P2).

### ResendContractRequest

```json
{
  "email": "optional-override@example.com"
}
```

## Flyway

```text
V024__contracts.sql
V025__contract_addendums.sql   # P2
```

**Note**: Run after FR-04 `V023__outbox_and_booking_stubs.sql` (or equivalent bookings + outbox).

## ActivityLog Events

| Event | When |
|-------|------|
| CONTRACT_GENERATED | PDF + row created |
| CONTRACT_EMAIL_SENT | First email success |
| CONTRACT_RESENT | Manager resend success |
| CONTRACT_ADDENDUM_GENERATED | P2 damage addendum |

## Integration Points

| Feature | Integration |
|---------|-------------|
| FR-04 | Writes `CONTRACT_GENERATE_REQUESTED` on CONFIRMED; booking status drives contract status |
| FR-12 | Deposit verify / VNPay IPN triggers CONFIRMED |
| FR-01 | Customer email from users |
| FR-06 | Manager property scope filter |
| FR-08 | Room/property data for snapshot |
| FR-23 | Damage approve → addendum outbox P2 |

## Security

| Endpoint | CUSTOMER | MANAGER | Others |
|----------|----------|---------|--------|
| GET /contracts/me | Own | — | Denied |
| GET /contracts/{id} | Own | — | Denied |
| GET /manager/contracts | — | Assigned property | Denied |
| POST /manager/contracts/{id}/resend | — | Assigned property | Denied |
| GET /*/pdf | Same as detail | Same as detail | Denied |
