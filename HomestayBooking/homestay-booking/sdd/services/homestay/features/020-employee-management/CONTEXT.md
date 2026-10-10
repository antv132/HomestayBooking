# CONTEXT.md — FR-20 Employee Management
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/020-employee-management/spec.md`

## 1. PROBLEM STATEMENT

Cơ sở thiếu danh bạ nhân viên theo Property; không gán/tạo/sửa/suspend Employee đúng scope → tác vụ vận hành (HK/maintenance/inspection) không biết giao ai.

Pain cụ thể:
- Manager không thấy Staff Directory Property mình; hoặc thấy Property khác (lỗ hổng RBAC).
- Không tạo được tài khoản EMPLOYEE + assignment ACTIVE trong một luồng onboard.
- Employee có thể bị gán nhiều Property ACTIVE cùng lúc → scope dữ liệu loạn.
- Suspend/Activate không khớp FR-01 → nhân viên nghỉ vẫn login được.
- Chuyển Property (reassign) thiếu quy trình; tác vụ mở tại Property cũ bị bỏ quên.

Không mô tả giải pháp — chỉ pain: thiếu quản trị Employee + EmployeePropertyAssignment theo property RBAC.

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa trong hệ thống này |
|-----------|-------------------------|
| Employee | User role EMPLOYEE |
| EmployeePropertyAssignment | Gán Employee ↔ Property; ACTIVE / INACTIVE |
| One ACTIVE assignment | Mỗi Employee tối đa 1 ACTIVE tại một thời điểm |
| Staff Directory (SCR-39) | List theo propertyId + actions Assign/Create/Edit/Suspend |
| Suspended | Chặn login (FR-01) |
| Reassign | Admin P2: A→INACTIVE, B→ACTIVE giữ lịch sử |

Ranh giới: Self profile/password = FR-02; task assignment = FR-21/13/23; dashboard = FR-22.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Manager | List/create/assign/update/suspend trong Property được gán |
| Admin | Mọi Property + reassign cross-property |
| Employee | Đối tượng quản trị; không vào SCR-39 |
| Spec Architect / Agent Pilot | Spec + implement `020-employee-management` |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- Property-scoped RBAC; `UNAUTHORIZED_PROPERTY_ACCESS` khi vượt scope

**Business**
- Không đổi role qua FR-20; email unique
- Suspend không auto-cancel task mở (reassign tay FR-21/13)
- Invite/reset password tái sử dụng FR-01

**Time / Process**
- Artifacts: `sdd/services/homestay/features/020-employee-management/`

## 5. ASSUMPTIONS

1. Email định danh — Manager không đổi email v1 (hoặc chỉ Admin).
2. Tạo mới status ACTIVE hoặc INACTIVE chờ set password (align FR-01).
3. Manager update vs FR-02 self-edit: last-write-wins v1.
4. Cross-property reassign = Admin-only.
5. Cảnh báo khi reassign còn task mở; không auto-complete.

## 6. OPEN QUESTIONS

1. Flow mật khẩu Employee mới: invite email bắt buộc?
2. Manager có được unassign (INACTIVE) trong Property không?
3. Soft history assignment hiển thị trên UI thế nào?
4. Phone/fullName validation rules chính thức?
5. Suspend có revoke refresh tokens ngay không?
