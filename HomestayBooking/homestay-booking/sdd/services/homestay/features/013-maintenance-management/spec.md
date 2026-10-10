# Feature Specification: FR-13 Maintenance Management

**Feature Branch**: `015-maintenance-management`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-13 Maintenance Management — dựa vào docs (Specification_v2.md § FR-13, §5 MaintenanceTicket/Attachment, §6 RBAC, §7 Validation, §8 Acceptance Maintenance, api-spec-by-screen SCR-22/23/41/61, screen.md, screendesign.md, entity-ui-mapping.md §1.9 §2.3)"

**Phụ thuộc**: FR-04 (booking lifecycle — ticket MUST link active booking); FR-06 (Manager/Employee property scope); FR-08 (Room); FR-01 (auth); FR-15 (notification delivery — FR-13 owns ticket lifecycle events and notification triggers). **Ranh giới**: FR-15 owns notification engine, WebSocket push, read/unread UI; FR-13 owns MaintenanceTicket CRUD, status workflow, assignment, attachments, and emits events for Customer notification on status changes. FR-08 room status **Maintenance** (Manager manual lock) is separate from MaintenanceTicket workflow.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Customer gửi yêu cầu bảo trì gắn booking (Priority: P1)

Là **Customer** đang có **booking đang hoạt động** (Confirmed hoặc Checked-in), tôi muốn tạo yêu cầu bảo trì cho phòng của booking đó, kèm tiêu đề, mô tả và hình ảnh minh họa tùy chọn, để báo sự cố trong thời gian lưu trú.

**Why this priority**: FR-13 core — Customer-initiated maintenance; without create flow there is no ticket pipeline.

**Independent Test**: Customer với booking Confirmed → mở form tạo ticket → chọn booking/phòng → nhập title + description → upload ảnh → submit → ticket **Open** xuất hiện trong danh sách của Customer.

**Acceptance Scenarios**:

1. **Given** Customer có booking **Confirmed** hoặc **Checked-in** thuộc phòng P, **When** gửi yêu cầu với title, description hợp lệ và bookingId hợp lệ, **Then** tạo MaintenanceTicket status **Open** gắn Customer, Booking, Room; hiển thị trong danh sách ticket của Customer (SCR-22).
2. **Given** Customer upload tối đa 5 ảnh hợp lệ (định dạng ảnh, dung lượng trong giới hạn), **When** submit thành công, **Then** ảnh được lưu dưới dạng Attachment liên kết ticket; Customer xem lại được trên chi tiết ticket.
3. **Given** Customer **không có** booking đang hoạt động, **When** cố tạo ticket, **Then** từ chối với thông báo cần booking hợp lệ.
4. **Given** Customer A, **When** cố tạo ticket gắn booking của Customer B, **Then** từ chối.
5. **Given** booking đã **Cancelled** hoặc **Checked-out**, **When** Customer cố tạo ticket, **Then** từ chối (không còn active booking).

---

### User Story 2 - Customer chỉnh sửa hoặc xóa ticket khi Open (Priority: P1)

Là **Customer**, tôi muốn sửa hoặc xóa yêu cầu bảo trì của mình **chỉ khi** ticket chưa được xử lý (status **Open**), để sửa thông tin sai hoặc hủy yêu cầu không còn cần thiết.

**Why this priority**: FR-13 explicit edit/delete rule; prevents tampering after operations started.

**Independent Test**: Ticket Open → Customer edit title/description/attachments → saved; ticket Assigned → edit/delete blocked.

**Acceptance Scenarios**:

1. **Given** ticket status **Open** thuộc Customer, **When** Customer cập nhật title, description hoặc thay đổi ảnh đính kèm, **Then** lưu thành công; `updatedAt` cập nhật.
2. **Given** ticket status **Open** thuộc Customer, **When** Customer xóa ticket, **Then** ticket bị xóa (hoặc soft-delete không hiển thị trong danh sách); attachments liên quan không còn hiển thị.
3. **Given** ticket status **Assigned**, **In Progress**, **Resolved**, hoặc **Closed**, **When** Customer cố edit hoặc delete, **Then** từ chối với thông báo chỉ Open mới được phép.
4. **Given** Customer A, **When** cố edit/delete ticket của Customer B, **Then** từ chối.

---

### User Story 3 - Manager xem danh sách và gán Employee (Priority: P1)

Là **Manager** được gán Property, tôi muốn xem tất cả yêu cầu bảo trì trong Property đó, xem chi tiết kèm ảnh khách gửi, và **gán Employee** cùng Property để xử lý — chuyển ticket từ **Open** sang **Assigned**.

**Why this priority**: FR-13 Manager assignment; gates Employee work.

**Independent Test**: Manager P1 sees only P1 tickets → assign Employee E1 (same property) → status Assigned, assignee set, Customer notified.

**Acceptance Scenarios**:

1. **Given** Manager được gán Property P, **When** mở Maintenance Tasks (SCR-41), **Then** chỉ tickets có Room thuộc P hiển thị; có thể lọc theo status.
2. **Given** ticket **Open**, **When** Manager chọn Employee E thuộc cùng Property và xác nhận gán, **Then** status → **Assigned**; `assignedEmployeeId`, `assignedAt` ghi nhận; Customer nhận thông báo cập nhật (FR-15).
3. **Given** Manager cố gán Employee thuộc Property khác, **When** submit, **Then** từ chối.
4. **Given** Manager không được gán Property P, **When** truy cập tickets của P, **Then** từ chối (UNAUTHORIZED_PROPERTY_ACCESS).
5. **Given** ticket đã **Assigned**, **When** Manager gán lại Employee khác (reassign), **Then** cập nhật assignee và `assignedAt`; Customer được thông báo (assumption: reassign allowed while not Closed).

---

### User Story 4 - Employee thực hiện bảo trì và cập nhật tiến độ (Priority: P1)

Là **Employee**, tôi muốn xem các ticket bảo trì **được gán cho mình**, bắt đầu công việc và đánh dấu hoàn thành sửa chữa — chuyển **Assigned → In Progress → Resolved**.

**Why this priority**: FR-13 Employee execution; core operational path.

**Independent Test**: Employee sees assigned tickets only → start → In Progress → finish → Resolved; invalid transitions rejected.

**Acceptance Scenarios**:

1. **Given** Employee E được gán ticket T, **When** mở Maintenance Workspace (SCR-61), **Then** chỉ tickets `assignedEmployeeId = E` hiển thị.
2. **Given** ticket **Assigned** gán cho E, **When** E bắt đầu xử lý, **Then** status → **In Progress**; Customer được thông báo.
3. **Given** ticket **In Progress** gán cho E, **When** E đánh dấu hoàn thành sửa chữa, **Then** status → **Resolved**; Customer được thông báo chờ Manager xác nhận.
4. **Given** Employee F **không** được gán ticket T, **When** F cố cập nhật status T, **Then** từ chối.
5. **Given** ticket **Open** (chưa gán), **When** Employee cố chuyển sang In Progress, **Then** từ chối — phải qua Assigned trước.
6. **Given** Employee hoàn thành, **When** ghi chú vật tư/thay thế (optional work note), **Then** lưu kèm ticket để Manager xem (assumption: internal note field, không bắt buộc).

---

### User Story 5 - Manager xác nhận hoàn thành và đóng ticket (Priority: P1)

Là **Manager**, sau khi Employee đánh dấu **Resolved**, tôi muốn xem kết quả, ghi **ghi chú giải quyết** (resolution note) và xác nhận đóng ticket — **Resolved → Closed** — để khách biết vấn đề đã xử lý xong.

**Why this priority**: FR-13 Manager verification closes the loop; quality gate before Customer sees final closure.

**Independent Test**: Ticket Resolved → Manager opens drawer → enters resolution note → confirm → Closed; Customer notified.

**Acceptance Scenarios**:

1. **Given** ticket **Resolved** trong Property được gán, **When** Manager xác nhận hoàn thành với resolution note (bắt buộc, tối thiểu 10 ký tự), **Then** status → **Closed**; `verifiedBy`, `verifiedAt`, `resolutionNote` ghi nhận; Customer nhận thông báo đóng ticket.
2. **Given** ticket **Resolved**, **When** Manager cố đóng **không có** resolution note, **Then** từ chối.
3. **Given** ticket **In Progress** hoặc **Assigned**, **When** Manager cố đóng trực tiếp sang Closed, **Then** từ chối — phải qua Resolved (Employee) trước.
4. **Given** ticket **Closed**, **When** bất kỳ vai nào cố thay đổi status, **Then** từ chối — ticket ở trạng thái cuối (read-only lifecycle).

---

### User Story 6 - Customer theo dõi tiến độ và nhận thông báo (Priority: P2)

Là **Customer**, tôi muốn xem danh sách và trạng thái các yêu cầu bảo trì đã gửi, và được **tự động thông báo** khi có cập nhật (gán nhân viên, đang xử lý, đã sửa xong, đã đóng), để không phải liên hệ thủ công.

**Why this priority**: FR-13 "theo dõi tiến độ và tự động thông báo"; delivery via FR-15.

**Independent Test**: Ticket progresses through lifecycle → Customer receives notification at each transition; list shows current status badge.

**Acceptance Scenarios**:

1. **Given** Customer có nhiều tickets, **When** mở danh sách (SCR-22), **Then** thấy Ticket ID/title, phòng, status badge, ngày tạo — sắp xếp mới nhất trước.
2. **Given** ticket chuyển **Open → Assigned → In Progress → Resolved → Closed**, **When** mỗi chuyển trạng thái hợp lệ xảy ra, **Then** hệ thống phát sự kiện thông báo tới Customer (FR-15 delivers); thông báo có liên kết tới ticket.
3. **Given** Customer mở chi tiết ticket, **When** xem, **Then** thấy timeline trạng thái hiện tại, mô tả, ảnh đính kèm; **không** thấy ghi chú nội bộ Employee (nếu có) — chỉ resolution note sau khi Closed (assumption: resolution note visible to Customer when Closed).

---

### Edge Cases

- Customer submit không có title hoặc description quá ngắn → validation error thân thiện.
- Upload ảnh vượt giới hạn số lượng hoặc dung lượng → từ chối; ticket không tạo nếu validation fail trước persist.
- Upload file không phải ảnh → từ chối.
- Manager Property P1 cố xem/gán ticket Room thuộc P2 → UNAUTHORIZED_PROPERTY_ACCESS.
- Employee cố skip status (Assigned → Resolved) → từ chối invalid transition.
- Ticket Open lâu không gán → vẫn hiển thị Manager dashboard; không auto-close (assumption v1).
- Customer có nhiều active bookings → form cho phép chọn booking/phòng tương ứng.
- Admin **Read-only** global maintenance list (RBAC R) — không assign/verify (assumption: Admin oversight only, same as matrix).
- FR-15 chưa triển khai → events queued/stub; ticket workflow vẫn hoạt động; notifications deferred (assumption).
- MaintenanceTicket **không** tự động đổi Room operational status sang Maintenance — đó là luồng FR-08 Manager manual lock (tách biệt).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Customer MUST tạo MaintenanceTicket gắn **booking đang hoạt động** (Confirmed hoặc Checked-in) thuộc chính Customer; ticket MUST liên kết Booking, Room, Customer.
- **FR-002**: Customer MUST cung cấp **title** (bắt buộc) và **description** (bắt buộc, tối thiểu 20 ký tự) khi tạo ticket.
- **FR-003**: Customer MAY đính kèm tối đa **5** hình ảnh minh họa; mỗi file MUST là ảnh và không vượt **5 MB** (assumption v1 limits).
- **FR-004**: Attachments MUST lưu qua entity Attachment (EntityType Maintenance) liên kết ticket; Customer và Manager xem được ảnh read-only.
- **FR-005**: Ticket mới MUST có status **Open**.
- **FR-006**: Customer MUST chỉnh sửa hoặc xóa ticket **của mình** khi và chỉ khi status **Open**.
- **FR-007**: Manager MUST xem danh sách MaintenanceTicket scoped theo **Property được gán**; MUST xem chi tiết kèm attachments.
- **FR-008**: Manager MUST gán Employee xử lý ticket **Open** (hoặc reassign **Assigned**); assignee MUST thuộc **cùng Property** với Room của ticket.
- **FR-009**: Gán Employee MUST chuyển status **Open → Assigned** và ghi `assignedEmployeeId`, `assignedAt`.
- **FR-010**: Employee MUST chỉ xem và cập nhật ticket **được gán cho mình** trong Property được phép.
- **FR-011**: Employee MUST cập nhật status theo luồng hợp lệ: **Assigned → In Progress → Resolved**; MUST từ chối chuyển trạng thái không hợp lệ hoặc ticket không thuộc assignee.
- **FR-012**: Manager MUST xác nhận hoàn thành ticket **Resolved** bằng **resolution note** bắt buộc; chuyển **Resolved → Closed**; ghi `verifiedBy`, `verifiedAt`, `resolutionNote`.
- **FR-013**: Ticket **Closed** MUST là trạng thái cuối — không cho phép thay đổi status hoặc nội dung vận hành.
- **FR-014**: Quy trình status MUST tuân thủ: **Open → Assigned → In Progress → Resolved → Closed** (không bỏ bước).
- **FR-015**: Hệ thống MUST phát sự kiện thông báo Customer khi ticket chuyển trạng thái (Assigned, In Progress, Resolved, Closed) — FR-15 thực hiện gửi/push.
- **FR-016**: Customer MUST xem danh sách và chi tiết ticket **của mình** với status hiện tại và metadata cơ bản (phòng, booking, ngày tạo, ảnh).
- **FR-017**: Admin MUST có quyền **Read** maintenance tickets toàn hệ thống; MUST NOT assign hoặc verify (Manager responsibility).
- **FR-018**: Hệ thống MUST từ chối tạo ticket khi booking không active (Cancelled, Checked-out, Pending Deposit chưa Confirmed, v.v.).
- **FR-019**: Hệ thống MUST ghi **ActivityLog** cho các hành động chính: TICKET_CREATED, TICKET_UPDATED, TICKET_DELETED, TICKET_ASSIGNED, TICKET_STATUS_CHANGED, TICKET_CLOSED.
- **FR-020**: FR-13 MUST **không** triển khai notification engine — thuộc FR-15; FR-13 chỉ emit domain events/triggers.

### Key Entities

- **MaintenanceTicket**: id, customerId, roomId, bookingId, title, description, assignedEmployeeId, assignedAt, status (Open | Assigned | In Progress | Resolved | Closed), resolutionNote, verifiedBy, verifiedAt, createdAt, updatedAt.
- **Attachment**: id, entityType (Maintenance), entityId, fileUrl, fileName, fileSize, uploadedAt — optional images on ticket create/edit (Open only).
- **Booking** (read/link): FR-04 owns lifecycle; FR-13 validates active booking for create.
- **Room** (read/link): FR-08 owns; ticket inherits property scope via room.
- **Employee** (read/link): assignee must belong to same property as room.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Customer hoàn tất tạo yêu cầu bảo trì (form + submit) trong **dưới 3 phút** khi đã có booking active.
- **SC-002**: **100%** ticket tạo thành công có status ban đầu **Open** và liên kết đúng booking/room/customer.
- **SC-003**: **100%** attempt edit/delete ticket không-Open bị từ chối (enforcement test).
- **SC-004**: **100%** Manager assignment chỉ chấp nhận Employee cùng Property — cross-property assign bị từ chối.
- **SC-005**: **100%** status transition tuân thủ luồng Open→Assigned→In Progress→Resolved→Closed — invalid skip bị từ chối.
- **SC-006**: Manager đóng ticket Resolved với resolution note trong **dưới 2 phút** sau khi Employee hoàn thành (UX target).
- **SC-007**: **100%** lifecycle transitions (Assigned, In Progress, Resolved, Closed) phát notification event tới Customer (verify via FR-15 integration or event log).
- **SC-008**: **100%** Manager/Employee list requests trả đúng scope — không leak ticket Property khác.
- **SC-009**: **95%** Customer tìm thấy trạng thái ticket cập nhật trên danh sách trong **dưới 5 giây** sau khi status thay đổi (refresh or real-time via FR-15).

## Assumptions

- **Active booking** = status **Confirmed** hoặc **Checked-in**; booking MUST thuộc Customer đang đăng nhập.
- Title max 200 ký tự; description min 20, max 2000 ký tự (assumption v1).
- Attachment limits: max 5 files, 5 MB each, image types JPEG/PNG/WebP only.
- Manager **reassign** allowed while ticket not Closed (Assigned/In Progress/Resolved) — updates assignee and notifies Customer.
- Employee optional **work note** on status update — internal, Manager-visible; not shown to Customer until Closed resolution note.
- Customer sees **resolution note** on Closed ticket as closure summary from Manager.
- Soft-delete for Customer delete Open ticket — hidden from lists; Admin may still audit (assumption v1).
- Pagination on list views — standard page size (assumption: 20 per page).
- FR-15 notification stub acceptable v1 — events logged; full push when FR-15 ships.
- MaintenanceTicket workflow independent from Room calendar status **Maintenance** (FR-08 manual lock).
- Segregation: Employee không gán ticket; Manager không tự chuyển In Progress/Resolved thay Employee (Manager only assign + verify close).
- One Employee may have multiple assigned maintenance tickets concurrently — rule "one ACTIVE assignment" in spec applies to Manager-Property assignment, not maintenance ticket count (per §7 context).
