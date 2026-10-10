# CONTEXT.md — FR-06 Property Management
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/006-property-management/spec.md`

## 1. PROBLEM STATEMENT

Đa chi nhánh mà không có “chủ” rõ → vận hành lệch scope, Guest vẫn thấy chi nhánh đã tắt.

Pain cụ thể:
- Admin không CRUD được danh mục Property (name/address/status) trên toàn hệ thống.
- Property ACTIVE không có Manager chịu trách nhiệm — hoặc nhiều Manager ACTIVE cùng lúc gây mơ hồ.
- Đổi Manager mất lịch sử gán → không audit được ai quản lý khi nào.
- Manager thấy/sửa property không được gán → vượt quyền.
- Property INACTIVE vẫn lộ discovery (FR-03) nếu status không khép với Guest.

Pain: quản trị chi nhánh + assignment trách nhiệm, chưa phải CRUD phòng/tầng.

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa |
|-----------|--------|
| Property | Homestay/resort chi nhánh (name, address, description, status) |
| ACTIVE / INACTIVE | ACTIVE vận hành + discovery; INACTIVE ẩn Guest |
| ManagerPropertyAssignment | Gán Manager ↔ Property; tối đa 1 ACTIVE / property |
| AssignedBy / AssignedAt | Audit khi gán |
| SCR-46–49 | List, create, edit, assign Manager |
| Scope Manager | Chỉ property có assignment ACTIVE |

Ranh giới: Floor = FR-07; Room = FR-08; Employee assignment = API riêng, không FR-06.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Admin | CRUD property + gán Manager |
| Manager | Xem property được gán (read) |
| Guest | Side-effect: không thấy INACTIVE |
| Spec Architect | Chốt rule ACTIVE bắt buộc có Manager |
| Agent Pilot | Implement Admin + Manager list |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring Security RBAC + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- Soft lifecycle: INACTIVE thay hard delete trong FR-06

**Business**
- Chỉ role MANAGER được gán (không gán Admin làm Manager)
- Một ACTIVE assignment per property; giữ record INACTIVE làm lịch sử
- Property ACTIVE MUST có Manager ACTIVE

**Time**
- Nền tảng trước Structure/Room/Booking scope

## 5. ASSUMPTIONS

1. Tạo property mặc định INACTIVE đến khi gán Manager (hoặc wizard gán cùng lúc).
2. Trùng tên property cho phép; phân biệt bằng id/address.
3. Manager SUSPENDED: assignment vẫn ACTIVE cho đến khi Admin reassign; login bị FR-01 chặn.
4. Hard delete property out of scope khi còn room/booking.
5. EmployeePropertyAssignment không thuộc FR-06.

## 6. OPEN QUESTIONS

1. Create + Assign Manager: một transaction bắt buộc hay cho draft INACTIVE?
2. Unassign khi ACTIVE: bắt buộc chọn Manager thay thế trong cùng request?
3. Stats summary trên detail Manager (số floor/room) có SLA v1?
4. Search list: full-text hay LIKE name/address?
5. Property INACTIVE: Manager còn CRUD floor/room nội bộ không? (align FR-07/08)
