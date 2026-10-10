# CONTEXT.md — FR-22 Employee Dashboard
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/022-employee-dashboard/spec.md`

## 1. PROBLEM STATEMENT

Employee phải vào từng workspace (HK / maintenance / inspection) mới biết còn bao nhiêu việc; thiếu tổng quan ca làm và tác vụ “hôm nay”.

Pain cụ thể:
- Không có SCR-59 greeting + KPI pending housekeeping / maintenance / inspections đúng người.
- Preview list task được gán thiếu; Completed/Cancelled lẫn vào “đang chờ”.
- Không gom “tác vụ hôm nay” (check-out cần inspection, HK/maintenance due).
- Không tách Đang chờ vs Đã hoàn thành hôm nay → khó theo dõi tiến độ ca.
- Dashboard bị dùng để đổi status → lệch ranh giới SCR-60/61/62.

Không mô tả giải pháp — chỉ pain: thiếu read-only aggregate + navigation cho Employee theo Property assignment.

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa trong hệ thống này |
|-----------|-------------------------|
| Employee Dashboard (SCR-59) | KPI + preview lists + today + completed today |
| Pending housekeeping | HousekeepingTask Pending/In Progress được gán |
| Pending maintenance | MaintenanceTicket Assigned/In Progress được gán |
| Pending inspections | RoomInspection Pending/In Progress (FR-23) |
| Today section | Theo timezone property / Asia/Ho_Chi_Minh |
| Deep links | SCR-60 Housekeeping, SCR-61 Maintenance, SCR-62 Inspection |

Ranh giới: FR-22 không cập nhật status task; chỉ aggregate + navigate.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Employee | Người dùng chính SCR-59 |
| Manager / Customer / Admin | Không dùng Employee Dashboard |
| FR-20/21/13/23/04 | Scope property + nguồn task |
| Spec Architect / Agent Pilot | Spec + implement `022-employee-dashboard` |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- Auth EMPLOYEE; data chỉ assignment Property (FR-20)

**Business**
- Read-only; mọi Start/Finish/Pass trên workspace feature gốc
- Touch-friendly UI theo component-library (SCR-59)

**Time / Process**
- Artifacts: `sdd/services/homestay/features/022-employee-dashboard/`

## 5. ASSUMPTIONS

1. Preview max ~5 mục; pending ưu tiên trước Completed.
2. Completed section chỉ “hôm nay”.
3. KPI = 0 khi không có task, không lỗi trang.
4. Inspection assignment rule align FR-23 (`inspectedBy` / assigned).
5. Multi-property Employee (hiếm): hiện property name trên item.

## 6. OPEN QUESTIONS

1. Employee v1 có multi ACTIVE property không (FR-20 nói 1 ACTIVE)?
2. Định nghĩa “due today” cho maintenance chính xác?
3. Single aggregate API hay compose FE?
4. Unread notifications có trên SCR-59 không?
5. Empty states copy tiếng Việt chuẩn?
