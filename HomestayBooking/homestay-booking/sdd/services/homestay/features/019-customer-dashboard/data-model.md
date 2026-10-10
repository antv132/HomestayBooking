# Data Model: FR-19 Customer Dashboard

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: read aggregates from FR-04/12/13/15

## Scope

FR-19 **does not own** persistent tables. **Reads** from:

- `bookings` (FR-04)
- `payments` (FR-12) via `bookings`
- `maintenance_tickets` (FR-13)
- `notifications` (FR-15)
- `damage_reports` (FR-23, P2 optional) for dispute alert

## ERD (read paths)

```text
Customer (users) 1──* Booking 1──* Payment
Customer 1──* MaintenanceTicket
User 1──* Notification
Customer 1──* DamageReport (P2 dispute flag)

CustomerDashboardService:
  countActiveBookings(customerId)
  countPendingPayments(customerId)
  countOpenTickets(customerId)
  countUnreadNotifications(userId)
  findUpcomingCheckIn(customerId)
  findUpcomingCheckOut(customerId)
  listUpcomingBookings(customerId, limit=5)
  listRecentPayments(customerId, limit=3)
  listRecentNotifications(userId, limit=5)
  findPendingDamageDispute(customerId)  # P2
```

## DTO: CustomerDashboardResponse

Align `Frontend/src/api/customersApi.ts` `CustomerDashboardData`:

| Field | Type | Source |
|-------|------|--------|
| activeBookings | int | COUNT CONFIRMED + CHECKED_IN |
| pendingPayments | int | COUNT PENDING payments |
| openTickets | int | COUNT OPEN + IN_PROGRESS tickets |
| unreadNotifications | int | COUNT unread notifications |
| upcomingCheckIn | UpcomingEventDto? | nearest CONFIRMED check-in |
| upcomingCheckOut | UpcomingEventDto? | nearest CHECKED_IN check-out |
| upcomingBookings | BookingSummaryDto[] | top 5 upcoming |
| recentPayments | PaymentSummaryDto[] | last 3 |
| recentNotifications | NotificationSummaryDto[] | last 5 |
| pendingDamageDispute | DamageDisputeAlertDto? | P2 optional |

### UpcomingEventDto

| Field | Type | Notes |
|-------|------|-------|
| bookingId | UUID | |
| roomNumber | string | from room join |
| propertyName | string | from property join |
| date | date | check-in or check-out date |
| daysUntil | int | ≥ 0 |

### PaymentSummaryDto

| Field | Type |
|-------|------|
| id | UUID |
| type | string | DEPOSIT, REMAINING_BALANCE, DAMAGE_FEE |
| amount | long |
| status | string |
| createdAt | datetime |

### NotificationSummaryDto

| Field | Type |
|-------|------|
| id | UUID |
| title | string |
| content | string |
| type | string |
| isRead | boolean |
| createdAt | datetime |

### BookingSummaryDto (subset FR-04)

| Field | Type |
|-------|------|
| id | UUID |
| roomNumber | string |
| roomType | string |
| propertyName | string |
| checkInDate | date |
| checkOutDate | date |
| status | string |
| totalAmount | long |

### DamageDisputeAlertDto (P2)

| Field | Type |
|-------|------|
| damageReportId | UUID |
| amount | long |
| disputeDeadline | datetime |

## Query Rules

### Active bookings count

```sql
status IN ('CONFIRMED', 'CHECKED_IN')
AND customer_id = :customerId
```

### Upcoming bookings list

```sql
status IN ('PENDING_DEPOSIT', 'CONFIRMED', 'CHECKED_IN')
AND check_out >= :today
ORDER BY check_in ASC
LIMIT 5
```

### Timezone

All date comparisons use `Asia/Ho_Chi_Minh` local date.

## Optional Migration: V035__customer_dashboard_indexes_fr19.sql

```sql
CREATE INDEX IF NOT EXISTS idx_bookings_customer_status_checkin
  ON bookings(customer_id, status, check_in);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read_created
  ON notifications(user_id, is_read, created_at DESC);
```

## Error Codes

| Code | HTTP | When |
|------|------|------|
| DASHBOARD_ACCESS_DENIED | 403 | Non-CUSTOMER |

## Cross-FR Dependencies

| FR | Relationship |
|----|--------------|
| FR-01 | JWT CUSTOMER role |
| FR-04 | bookings source |
| FR-12 | payments source |
| FR-13 | maintenance tickets source |
| FR-15 | notifications source |
| FR-23 | optional dispute alert P2 |
