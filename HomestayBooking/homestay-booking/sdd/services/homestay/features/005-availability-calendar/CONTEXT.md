# CONTEXT.md — FR-05 Availability Calendar
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/005-availability-calendar/spec.md`

## 1. PROBLEM STATEMENT

Lịch phòng không phản ánh đúng vận hành → khách đặt nhầm ngày, Manager khóa/mở sai inventory.

Pain cụ thể:
- Guest chỉ thấy “booked/available” thô — không phân biệt Pending Deposit, Occupied, Cleaning, Maintenance.
- Manager không khóa được Maintenance/OOS theo khoảng ngày + lý do; hoặc khóa đè booking đang active.
- Phòng sau checkout vẫn hiện Available trong khi đang Pending Cleaning → book chồng dọn phòng.
- Employee cần xem lịch nhưng không được sửa — thiếu read-only scope.
- Nhiều nguồn trạng thái cùng ngày (booking + HK + block) → UI mâu thuẫn nếu không có ưu tiên.

Pain: minh bạch tồn kho theo ngày + kiểm soát thủ công an toàn.

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa |
|-----------|--------|
| 8 statuses | Available, Pending Deposit, Reserved, Occupied, Pending Cleaning, Cleaning In Progress, Maintenance, Out Of Service |
| Half-open dates | `[check-in, check-out)` theo Asia/Ho_Chi_Minh |
| Manual block | Manager/Admin set Maintenance/OOS + range + reason (SCR-33) |
| Housekeeping gate | Không cho Available thủ công khi HousekeepingTask chưa Completed (FR-21) |
| SCR-09 vs SCR-33 | Guest calendar vs Manager update |
| Priority | Một status/ngày — booking lifecycle / HK / block theo bảng ưu tiên spec |

Ranh giới: FR-03 calendar đơn giản; WebSocket realtime = FR-15 (refresh chấp nhận được v1).

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Guest / Customer | Xem & chọn ngày bookable |
| Manager / Admin | Xem + cập nhật Maintenance/OOS |
| Employee | Xem read-only property được gán |
| Domain Owner | Chốt priority table khi conflict |
| Agent Pilot | API calendar + status update |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- Dates `LocalDate`; timezone property VN
- Inventory conflict với FR-04 lock — calendar eventual consistency ≤ refresh

**Business**
- Cấm Maintenance overlap booking active
- Cấm Available khi HK chưa xong
- Guest không thấy tên khách trên calendar

**Time**
- Sprint 2 liên quan availability; align `sdd/services/homestay/features/005-availability-calendar`

## 5. ASSUMPTIONS

1. Maintenance/OOS bắt buộc có end date (không open-ended v1).
2. Cancelled/No-show nhả ngày → Available trừ khi còn HK/block.
3. Admin toàn property; Manager chỉ property assignment (FR-06).
4. Realtime push optional; poll/refresh đủ MVP.
5. Occupied/Reserved map từ booking status FR-04 canonical.

## 6. OPEN QUESTIONS

1. Bảng ưu tiên status chính thức khi booking + cleaning cùng ngày?
2. Reserved vs Occupied: cắt theo check-in date thế nào với date-only?
3. Overview multi-room calendar có trong v1 SCR-33 không?
4. Interval refresh mặc định (giây/phút) khi chưa có FR-15?
5. Out Of Service dài hạn: status room-level hay chỉ date blocks?
