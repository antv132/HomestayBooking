# CONTEXT.md — FR-21 Housekeeping Management
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/021-housekeeping-management/spec.md`

## 1. PROBLEM STATEMENT

Sau check-out, phòng không tự vào quy trình dọn; Manager/Employee thiếu board gán việc và cập nhật trạng thái → phòng không Available kịp cho booking mới.

Pain cụ thể:
- Check-out hợp lệ (inspection + payments) nhưng không có HousekeepingTask / phòng không Pending Cleaning.
- Manager không thấy task theo Property (Pending / In Progress / Completed); gán nhầm Employee Property khác.
- Employee không Start/Finish được → room không Cleaning In Progress → Available.
- Ai đó cố Available thủ công khi task chưa Completed → phá inventory.
- Cần hủy task / xem lịch sử thỉnh thoảng; Customer không được vào HK management.

Không mô tả giải pháp — chỉ pain: thiếu vòng đời HousekeepingTask gắn room status sau checkout.

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa trong hệ thống này |
|-----------|-------------------------|
| HousekeepingTask | Tác vụ dọn: Pending → In Progress → Completed / Cancelled |
| Pending Cleaning / Cleaning In Progress / Available | Room status gắn lifecycle HK |
| Auto-create | Sau Checked-out hợp lệ — 1 task/idempotent theo checkout |
| SCR-40 / SCR-60 | Manager board / Employee workspace |
| Assignee | Employee cùng Property; chỉ assignee cập nhật |
| Manual task | Manager tạo thêm khi phòng cần dọn |

Ranh giới: FR-22 chỉ hiển thị; FR-04 owns checkout UI; FR-23/12 là precondition checkout.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| System | Auto-create task + room Pending Cleaning |
| Manager | List/assign/cancel/history (Property scope) |
| Employee | Start/Complete task được gán |
| Admin | Read-only assumption |
| Spec Architect / Agent Pilot | Spec + implement `021-housekeeping-management` |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- Property-scoped RBAC; không bypass room Available khi task chưa Completed

**Business**
- Strict transition: không Complete từ Pending; idempotent auto-create
- Employee chỉ task assigned; cùng Property
- Checkout thiếu inspection/payment → không tạo task

**Time / Process**
- Artifacts: `sdd/services/homestay/features/021-housekeeping-management/`

## 5. ASSUMPTIONS

1. Reassign chỉ Pending/In Progress.
2. Cancel khi Cleaning In Progress → room về Pending Cleaning hoặc Maintenance (Manager follow-up).
3. Admin R-only trên list.
4. Một assignee active / task.
5. History filter Completed ~30 ngày hữu ích cho audit.

## 6. OPEN QUESTIONS

1. Cancel bắt buộc note/reason format?
2. Room status sau Cancel chính thức là gì?
3. Manual task khi room đang Occupied có cho phép?
4. SLA dọn phòng / overdue badge có trong v1?
5. Notify Employee khi được gán (FR-15) bắt buộc?
