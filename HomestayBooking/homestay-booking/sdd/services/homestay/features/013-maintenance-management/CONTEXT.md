# CONTEXT.md — FR-13 Maintenance Management
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/013-maintenance-management/spec.md`

## 1. PROBLEM STATEMENT

Khách đang lưu trú gặp sự cố phòng nhưng không có kênh tin cậy để báo và theo dõi; vận hành thiếu vòng đời ticket rõ ràng.

Pain cụ thể:
- Customer có booking Confirmed/Checked-in muốn báo hỏng kèm ảnh nhưng không biết gửi đâu, hoặc gửi khi booking đã Cancelled/Checked-out.
- Ticket sau khi đã Assigned/In Progress vẫn bị sửa/xóa → mất kiểm soát vận hành.
- Manager không biết ticket nào thuộc Property mình; gán nhầm Employee Property khác.
- Employee không thấy ticket được gán / không cập nhật tiến độ được; Customer phải gọi hỏi vì không nhận cập nhật trạng thái.
- Manager đóng ticket khi chưa Resolved hoặc thiếu resolution note → khách không tin kết quả.

Không mô tả giải pháp kỹ thuật — chỉ pain: sự cố lưu trú không có pipeline Open → Assigned → In Progress → Resolved → Closed.

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa trong hệ thống này |
|-----------|-------------------------|
| MaintenanceTicket | Yêu cầu bảo trì gắn Customer + Booking + Room |
| Open / Assigned / In Progress / Resolved / Closed | Lifecycle ticket (Closed = terminal) |
| Active booking | Confirmed hoặc Checked-in — điều kiện tạo ticket |
| Attachment | Ảnh đính kèm (tối đa 5) liên kết ticket |
| Resolution note | Ghi chú Manager bắt buộc khi Closed (≥10 ký tự) |
| Work note | Ghi chú nội bộ Employee — không hiện cho Customer |
| Property scope | Manager/Employee chỉ ticket Room thuộc Property được gán |

Quy tắc: FR-13 owns ticket CRUD/workflow; FR-15 delivers notification; FR-08 room status Maintenance (manual lock) ≠ ticket workflow.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Customer | Tạo/sửa/xóa (Open), theo dõi tiến độ |
| Manager | List Property, assign/reassign, Closed + resolution note |
| Employee | Chỉ ticket được gán; Assigned → In Progress → Resolved |
| Spec Architect / Quality Gatekeeper | FR / RBAC / attachment limits |
| Agent Pilot | Implement theo `013-maintenance-management` |

## 4. CONSTRAINTS

**Tech**
- Java 17 + Spring Boot/Security/JPA; SQL Server; React 18 — theo `CONSTITUTION.md` / `AGENTS.md`
- Property-scoped RBAC; validate file trước khi ghi disk

**Business**
- Ticket MUST link active booking thuộc Customer; không cross-customer
- Customer edit/delete chỉ khi Open; Closed read-only
- Assignee phải cùng Property; emit events cho FR-15 khi đổi status

**Time / Process**
- Spec artifacts dưới `sdd/services/homestay/features/013-maintenance-management/`

## 5. ASSUMPTIONS

1. Reassign được phép khi ticket chưa Closed.
2. Soft-delete hoặc hard-delete Open ticket đều chấp nhận nếu list không còn hiển thị.
3. Resolution note visible cho Customer khi Closed; work note chỉ nội bộ.
4. Work note optional khi Employee → Resolved.
5. Max 5 ảnh; định dạng/dung lượng theo service hiện tại (~giới hạn upload team).

## 6. OPEN QUESTIONS

1. Soft-delete vs hard-delete ticket Open — chuẩn UAT?
2. Reassign có reset `assignedAt` và notify lại Customer mỗi lần không?
3. Giới hạn dung lượng/định dạng ảnh chính thức cho attachment?
4. Ticket Open quá lâu có SLA/auto-escalate không (v1)?
5. Manager có được reopen Closed trong v1 không (spec: terminal)?
