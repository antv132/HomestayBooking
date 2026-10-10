# Data Model: FR-20 Employee Management

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `docs/Specification_v2.md` §5 EmployeePropertyAssignment

## Scope

FR-20 **owns** `employee_property_assignments`. **Reads/writes** `users` (EMPLOYEE role only for mutations). **Reads** `properties` (FR-06).

## ERD

```text
User (EMPLOYEE) 1──* EmployeePropertyAssignment *──1 Property
User (ADMIN|MANAGER) assigns via assigned_by FK

EmployeeManagementService:
  listByProperty(propertyId, search, page)
  listUnassigned(search, page)
  createEmployee(request, assignedBy)
  assignEmployee(employeeId, propertyId, assignedBy)
  updateEmployee(employeeId, request, actor)
  updateStatus(employeeId, status, actor)
  reassignEmployee(employeeId, newPropertyId, adminId)  # P2 Admin-only
```

## Table: employee_property_assignments

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| employee_id | UUID | FK users NOT NULL | Must be role EMPLOYEE |
| property_id | UUID | FK properties NOT NULL | |
| assigned_by | UUID | FK users NOT NULL | Admin or Manager |
| assigned_at | TIMESTAMPTZ | NOT NULL | |
| status | VARCHAR(16) | NOT NULL | ACTIVE, INACTIVE |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**:

- Partial unique: `(employee_id) WHERE status = 'ACTIVE'` — one active assignment per employee
- `(property_id, status)` — list employees by property
- `(employee_id, assigned_at DESC)` — assignment history

**Validation on assign/create**:

- Target employee: `users.role = EMPLOYEE`
- Employee `users.status` must be `ACTIVE` (reject SUSPENDED/INACTIVE for new assign)
- Property `properties.status = ACTIVE`
- Manager: must have ACTIVE `manager_property_assignments` for `property_id`

## State Transitions

### EmployeePropertyAssignment.status

```text
CREATE → ACTIVE (on assign/create)
ACTIVE → INACTIVE (on reassign away or unassign P2+)
INACTIVE → ACTIVE (on reassign back — reuse row or insert new; prefer new row + history)
```

### User.status (Employee scope)

```text
ACTIVE ↔ SUSPENDED (via FR-20 PATCH status)
INACTIVE → ACTIVE (via FR-01 email verification — read-only in FR-20 list)
```

## API DTOs

### EmployeeSummaryResponse

| Field | Type | Source |
|-------|------|--------|
| id | UUID | users.id |
| fullName | string | users |
| email | string | users |
| phone | string? | users |
| avatarUrl | string? | users |
| status | enum | users.status |
| propertyId | UUID | epa.property_id |
| propertyName | string | properties.name |
| assignedAt | datetime | epa.assigned_at |
| assignmentStatus | enum | epa.status |

### CreateEmployeeRequest

| Field | Required | Validation |
|-------|----------|------------|
| fullName | yes | 2–200 chars |
| email | yes | valid email, unique |
| phone | no | E.164 or local pattern |
| propertyId | yes | valid ACTIVE property |

### AssignEmployeeRequest

| Field | Required |
|-------|----------|
| employeeId | yes |
| propertyId | yes |

### UpdateEmployeeRequest

| Field | Required |
|-------|----------|
| fullName | yes |
| phone | no |

### UpdateEmployeeStatusRequest

| Field | Required | Values |
|-------|----------|--------|
| status | yes | ACTIVE, SUSPENDED |

### ReassignEmployeeRequest (P2)

| Field | Required |
|-------|----------|
| propertyId | yes |

## Error Codes

| Code | HTTP | When |
|------|------|------|
| UNAUTHORIZED_PROPERTY_ACCESS | 403 | Manager accesses wrong property |
| EMPLOYEE_ALREADY_ASSIGNED | 409 | Employee has ACTIVE assignment elsewhere |
| EMPLOYEE_SUSPENDED | 400 | Assign/suspend conflict |
| PROPERTY_INACTIVE | 400 | Assign to inactive property |
| EMAIL_ALREADY_EXISTS | 409 | Create duplicate email |
| EMPLOYEE_NOT_FOUND | 404 | Invalid employee id |
| INVALID_ROLE | 400 | Target user not EMPLOYEE |

## Migration: V036__employee_property_assignments_fr20.sql

```sql
CREATE TABLE employee_property_assignments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  employee_id UUID NOT NULL REFERENCES users(id),
  property_id UUID NOT NULL REFERENCES properties(id),
  assigned_by UUID NOT NULL REFERENCES users(id),
  assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  status VARCHAR(16) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_epa_one_active_per_employee
  ON employee_property_assignments (employee_id)
  WHERE status = 'ACTIVE';

CREATE INDEX idx_epa_property_status ON employee_property_assignments (property_id, status);
CREATE INDEX idx_epa_employee_history ON employee_property_assignments (employee_id, assigned_at DESC);

-- Optional dev seed: 1 EMPLOYEE user + assignment (comments only)
```

## Cross-FR Dependencies

| FR | Relationship |
|----|--------------|
| FR-01 | users table, EMPLOYEE role, SUSPENDED login, invite email |
| FR-06 | properties, manager_property_assignments, PropertyScopeService |
| FR-17 | ActivityLogService for audit (optional) |
| FR-21/13 | Downstream — assign tasks only to employees at same property |
