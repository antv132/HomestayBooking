# CONTEXT.md — FR-08 Room Management
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/008-room-management/spec.md`

## 1. PROBLEM STATEMENT

Inventory phòng không quản lý được đúng Property/Floor → Guest thấy sai giá/ảnh hoặc Manager xóa phòng đang book.

Pain cụ thể:
- Manager không list/filter/search phòng trong scope được gán.
- Tạo phòng sai floor/property hoặc trùng room number trong cùng property.
- Sửa giá ảnh hưởng booking cũ nếu không snapshot (đụng FR-04) — cần rõ “giá mới chỉ booking mới”.
- Xóa phòng khi còn booking active → gãy lịch sử / over-release.
- Gallery: nhiều primary, thứ tự loạn, file quá lớn/không phải ảnh → Guest detail xấu hoặc lỗi server.
- Employee cần xem list nhưng không được CRUD.

Pain: CRUD phòng + gallery vận hành; không phải calendar manual (FR-05) hay discovery public (FR-03).

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa |
|-----------|--------|
| Room | Thuộc đúng một Property + Floor; status mặc định Available khi tạo |
| Room number uniqueness | Unique trong scope Property |
| RoomImage | Gallery; đúng một primary / room; sort order |
| SCR-29–32 | List, add, edit, gallery |
| SCR-65 | Employee read-only list |
| Active booking block | Không xóa khi PENDING_DEPOSIT / CONFIRMED / CHECKED_IN… (FR-04) |

Ranh giới: Maintenance date-range = FR-05; booking status transitions = FR-04.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Manager | CRUD room + gallery trong assignment |
| Employee | List read-only property được gán |
| Guest | Side-effect: thấy data qua FR-03 |
| Quality Gatekeeper | Validate file size/type trước ghi disk (LESSON-007) |
| Agent Pilot | Room pages + APIs |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- Validate MIME/size trước khi lưu ảnh (~50MB theo service hiện tại / LESSON)
- Layered Controller→Service→Repository; DTO boundaries

**Business**
- Không đổi `propertyId` sang property khác khi edit
- Giá mới không đè snapshot booking cũ (FR-04)
- Không xóa khi còn booking blocking

**Time**
- Phụ thuộc FR-06/07; song song Sprint inventory

## 5. ASSUMPTIONS

1. Hard delete room (+ images) khi không booking; soft-delete có thể amend sau.
2. Xóa primary → auto-promote ảnh còn lại đầu tiên.
3. Property INACTIVE: Manager vẫn quản lý nội bộ; Guest ẩn (FR-03).
4. Amenities lưu theo model đã có entity (JSON/list) — không invent schema lệch spec.
5. Employee assignment dùng cùng property scope pattern như Manager (entity riêng).

## 6. OPEN QUESTIONS

1. ~~Soft-delete vs hard-delete chính thức cho Room/RoomImage?~~ → Soft-delete (`deletedAt`) — Session 2026-07-23
2. ~~Đổi room number khi đã có booking history: cho phép không?~~ → Cho phép nếu unique trong property — Session 2026-07-23
3. ~~Max số ảnh / room và kích thước file UAT?~~ → 10 ảnh/room; jpeg/png/webp; 10MB/file — Session 2026-07-23
4. ~~Status Available lúc create vs đồng bộ FR-05 room operational status?~~ → Luôn AVAILABLE khi create; FR-05/booking/HK sau đó — Session 2026-07-23
5. ~~Di chuyển room sang floor khác khi đang Occupied: cho phép không?~~ → Cho phép cùng property kể cả Occupied — Session 2026-07-23

---

**Next:** Clarifications closed (5/5) → `/speckit-plan` (refresh nếu cần) hoặc `/speckit-implement` theo gaps FR-08.
