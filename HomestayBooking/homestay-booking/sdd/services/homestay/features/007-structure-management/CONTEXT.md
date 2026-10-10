# CONTEXT.md — FR-07 Structure Management
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/007-structure-management/spec.md`

## 1. PROBLEM STATEMENT

Không có cây Property → Floor → Room → Manager không nắm layout chi nhánh trước khi thêm/sửa phòng.

Pain cụ thể:
- Không biết property được gán có bao nhiêu tầng/phòng; selector lẫn property ngoài scope.
- Thêm tầng trùng số / số tầng không hợp lệ → cấu trúc loạn.
- Xóa tầng khi vẫn còn phòng → mất liên kết hoặc orphan rooms.
- Admin cần giám sát cấu trúc nhưng không được ghi đè CRUD floor của Manager.
- Room trên tree nếu editable nhầm → đụng FR-08; thiếu ranh giới gây double UI.

Pain: cấu trúc vật lý (Floor), không phải inventory booking hay gallery phòng.

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa |
|-----------|--------|
| Structure Tree | Property → Floor → Room (room node read-only) |
| Floor number | Số nguyên dương; unique trong cùng Property |
| SCR-28 | Structure Management + Property Selector |
| Manager write | CRUD Floor chỉ trên property assignment ACTIVE |
| Admin read | Xem mọi property; không Add/Edit/Delete floor trong FR-07 |
| Empty tree | Chưa có floor → empty state + gợi ý thêm tầng |

Phụ thuộc FR-06 assignment; CRUD Room = FR-08.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Manager | CRUD Floor trong scope |
| Admin | Xem tree read-only |
| Employee / Customer | Không write Floor |
| Agent Pilot | Tree UI + floorApi |
| Spec Architect | Chốt không xóa floor khi còn room |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- Align frontend `StructureTreePage` / `floorApi` nếu đã có
- Tree sort floor number tăng dần; collapse/expand chấp nhận được (<20 tầng)

**Business**
- Unique floor number / property
- Xóa Floor chỉ khi không còn Room con
- Write ngoài assignment → 403

**Time**
- Làm trước/cùng FR-08 để gắn phòng đúng Floor

## 5. ASSUMPTIONS

1. Floor number không cần liên tục (1, 3, 5 OK).
2. Property INACTIVE: Manager vẫn quản lý floor nội bộ; Guest không thấy (FR-03).
3. Concurrent edit: last-write-wins / optimistic conflict message đủ v1.
4. Click room node → điều hướng Room Detail (FR-08) optional.
5. Admin KPI “Total Floors” đọc từ cùng dữ liệu, không CRUD.

## 6. OPEN QUESTIONS

1. Đổi floor number khi đã có rooms: cho phép nếu không trùng?
2. Soft-delete floor hay hard delete khi empty?
3. Description floor bắt buộc hay optional?
4. Admin UI structure dùng chung SCR-28 hay màn riêng?
5. Giới hạn max floors/rooms per property có enforce không?
