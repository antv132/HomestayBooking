# Quickstart: FR-20 Employee Management

**Feature**: `specs/020-employee-management` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-01 JWT (Manager, Admin); FR-06 properties + manager assignments; V036 migration applied.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Seed: Admin user, Manager with ACTIVE property assignment, optional test Employee

## Environment

```bash
# No feature-specific env vars — reuse FR-01 mail for invite emails
```

Vite proxy: `/api/v1` → `http://localhost:8080`

## Run

```bash
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

## Screen → Route → API

| Screen | Route | API |
|--------|-------|-----|
| SCR-39 Staff Directory (Manager) | `/manager/employees` | `GET /manager/employees?propertyId=` |
| SCR-39 Assign modal | `/manager/employees` (modal) | `POST /manager/employees/assign` |
| SCR-39 Create employee | `/manager/employees` (modal) | `POST /manager/employees` |
| SCR-39 (Admin) | `/admin/employees` | `GET /admin/employees?propertyId=` |

## Smoke test: Manager list employees

```bash
BASE=http://localhost:8080/api/v1
MANAGER_TOKEN="<manager-jwt>"
PROPERTY_ID="<assigned-property-uuid>"

curl -s "$BASE/manager/employees?propertyId=$PROPERTY_ID" \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq

# Expected: success true, data.content[] with ACTIVE assignments at property
```

## Smoke test: Manager create employee + assign

```bash
curl -s -X POST "$BASE/manager/employees" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Nguyen Van A",
    "email": "employee.demo@example.com",
    "phone": "0901234567",
    "propertyId": "'"$PROPERTY_ID"'"
  }' | jq

# Expected: success true; user role EMPLOYEE; assignment ACTIVE
```

## Smoke test: Assign existing unassigned employee

```bash
EMPLOYEE_ID="<unassigned-employee-uuid>"

curl -s -X POST "$BASE/manager/employees/assign" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "'"$EMPLOYEE_ID"'",
    "propertyId": "'"$PROPERTY_ID"'"
  }' | jq

# Expected: success true
```

## Smoke test: Duplicate assignment rejected

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -X POST "$BASE/manager/employees/assign" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "'"$EMPLOYEE_ID"'",
    "propertyId": "'"$PROPERTY_ID"'"
  }'
# Expected: 409 (already assigned)
```

## Smoke test: Suspend employee

```bash
curl -s -X PATCH "$BASE/manager/employees/$EMPLOYEE_ID/status" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "SUSPENDED"}' | jq '.data.status'
# Expected: SUSPENDED — login blocked per FR-01
```

## Smoke test: Manager cross-property denied

```bash
OTHER_PROPERTY="<property-manager-not-assigned>"

curl -s -o /dev/null -w "%{http_code}" \
  "$BASE/manager/employees?propertyId=$OTHER_PROPERTY" \
  -H "Authorization: Bearer $MANAGER_TOKEN"
# Expected: 403
```

## Smoke test: Admin reassign (P2)

```bash
ADMIN_TOKEN="<admin-jwt>"
NEW_PROPERTY="<target-property-uuid>"

curl -s -X PATCH "$BASE/admin/employees/$EMPLOYEE_ID/reassign" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"propertyId": "'"$NEW_PROPERTY"'"}' | jq

# Expected: old assignment INACTIVE; new property ACTIVE
```

## Smoke test: RBAC denial

```bash
CUSTOMER_TOKEN="<customer-jwt>"

curl -s -o /dev/null -w "%{http_code}" \
  "$BASE/manager/employees?propertyId=$PROPERTY_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
# Expected: 403
```

## Manual UI checklist

- [ ] Manager `/manager/employees` — Staff Directory table with search
- [ ] Empty state + **Assign Employee** CTA when no staff
- [ ] **Assign Employee** modal — pick unassigned or create new
- [ ] Edit employee name/phone inline or modal
- [ ] Suspend/Activate with confirmation dialog
- [ ] Manager cannot access other property's employees
- [ ] Admin `/admin/employees` — property filter dropdown
- [ ] Admin reassign employee across properties (P2)
- [ ] Suspended employee cannot login

## Troubleshooting

| Issue | Check |
|-------|-------|
| 403 on manager list | Manager must have ACTIVE assignment to propertyId |
| 409 on assign | Employee already has ACTIVE assignment elsewhere |
| 409 on create | Email duplicate |
| Empty unassigned list | All EMPLOYEE users already assigned |
| V036 not applied | Run Flyway migration |
| Invite email not sent | FR-01 mail config; check logs |
