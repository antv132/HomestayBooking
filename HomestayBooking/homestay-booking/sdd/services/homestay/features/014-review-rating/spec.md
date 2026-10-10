# Feature Specification: FR-14 Review & Rating

**Feature Branch**: `016-review-rating`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-14 Review & Rating — dựa vào docs (Specification_v2.md § FR-14, §5 Review, §7 Validation, §8 Acceptance Review, api-spec-by-screen SCR-24/25, screen.md, screendesign.md, entity-ui-mapping.md §1.13, figma SCR-30/31/65, frontend reviewApi.ts, ReviewPages.tsx)"

**Phụ thuộc**: FR-04 (booking lifecycle — review chỉ sau **Checked-out**); FR-08 (Room); FR-03 (hiển thị đánh giá công khai trên Room Detail — FR-14 owns dữ liệu Review, FR-03 owns read API discovery). **Ranh giới**: FR-14 owns Review CRUD, moderation, room rating aggregates; FR-03 consumes PUBLISHED reviews for public display; Complaint/khiếu nại thuộc feature riêng (§8 Review and Feedback).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Customer đánh giá phòng sau check-out (Priority: P1)

Là **Customer** đã hoàn tất **check-out** cho một booking, tôi muốn gửi đánh giá **sao (1–5)** và **bình luận** gắn với booking đó, để chia sẻ trải nghiệm lưu trú và giúp khách khác tham khảo.

**Why this priority**: FR-14 core — authenticated review after stay; ensures review authenticity via booking link.

**Independent Test**: Customer với booking CHECKED_OUT chưa có review → mở form SCR-25 → submit rating + comment → review PUBLISHED; booking đã review → từ chối tạo thêm.

**Acceptance Scenarios**:

1. **Given** Customer có booking status **Checked-out** chưa có review, **When** gửi rating (1–5) và comment hợp lệ kèm `bookingId`, **Then** tạo Review status **Published** gắn Customer, Booking, Room; hiển thị trong My Reviews (SCR-24).
2. **Given** booking chưa **Checked-out** (Confirmed, Checked-in, Cancelled, v.v.), **When** Customer cố gửi review, **Then** từ chối với thông báo cần hoàn tất check-out.
3. **Given** booking đã có **một** review, **When** Customer cố tạo review thứ hai cho cùng booking, **Then** từ chối (mỗi booking tối đa 1 review).
4. **Given** Customer A, **When** cố review booking của Customer B, **Then** từ chối.
5. **Given** rating ngoài 1–5 hoặc comment quá ngắn, **When** submit, **Then** validation error thân thiện; không tạo review.

---

### User Story 2 - Customer xem, chỉnh sửa và xóa đánh giá của mình (Priority: P1)

Là **Customer**, tôi muốn xem lại danh sách đánh giá đã viết, chỉnh sửa rating/comment hoặc xóa đánh giá của chính mình, để cập nhật ý kiến sau khi suy nghĩ thêm.

**Why this priority**: FR-14 explicit edit/delete; customer ownership of content.

**Independent Test**: Customer mở SCR-24 → edit review → saved; delete review → removed from list and public display.

**Acceptance Scenarios**:

1. **Given** Customer có reviews, **When** mở My Reviews (SCR-24), **Then** thấy danh sách rating, comment, phòng, property, ngày tạo — chỉ của mình.
2. **Given** review thuộc Customer, **When** cập nhật rating và/hoặc comment hợp lệ, **Then** lưu thành công; `updatedAt` cập nhật; nếu status **Published** thì nội dung công khai cập nhật.
3. **Given** review thuộc Customer, **When** xóa review, **Then** review không còn hiển thị (My Reviews và công khai); booking có thể được review lại (assumption: delete frees booking slot — spec says max 1 while exists; after delete allow new review).
4. **Given** Customer A, **When** cố edit/delete review của Customer B, **Then** từ chối.
5. **Given** review status **Hidden** (bị moderation), **When** Customer xem My Reviews, **Then** vẫn thấy review với trạng thái Hidden; có thể edit/delete.

---

### User Story 3 - Khách xem đánh giá công khai của phòng (Priority: P1)

Là **khách** (Guest hoặc Customer) đang xem thông tin phòng, tôi muốn đọc các đánh giá **đã công bố** kèm điểm trung bình và số lượng review, để quyết định đặt phòng.

**Why this priority**: FR-14 "Xem đánh giá công khai của phòng"; supports discovery trust.

**Independent Test**: Room có 3 Published + 1 Hidden reviews → public list shows 3; average from Published only.

**Acceptance Scenarios**:

1. **Given** phòng có reviews status **Published**, **When** xem đánh giá công khai (Room Detail / discovery), **Then** chỉ **Published** hiển thị; mỗi item có rating, comment (truncate nếu dài), tên reviewer (hoặc tên hiển thị), ngày.
2. **Given** phòng có reviews, **When** hiển thị tóm tắt, **Then** **average rating** và **total review count** tính từ Published only (làm tròn 1 chữ số thập phân).
3. **Given** review status **Hidden**, **When** xem công khai, **Then** không hiển thị trong danh sách và không tính vào aggregate.
4. **Given** phòng chưa có review Published, **When** xem, **Then** empty state "Chưa có đánh giá" (hoặc tương đương).
5. **Given** danh sách công khai, **When** phân trang, **Then** sắp xếp mới nhất trước (assumption v1).

---

### User Story 4 - Manager kiểm duyệt và ẩn đánh giá không phù hợp (Priority: P1)

Là **Manager** được gán Property, tôi muốn xem reviews liên quan phòng thuộc Property của mình và **ẩn (Hide)** hoặc **hiện lại (Show)** đánh giá không phù hợp, để bảo vệ uy tín property.

**Why this priority**: FR-14 moderation; Manager responsibility per RBAC.

**Independent Test**: Manager P sees only P room reviews → Hide Published review → status Hidden, removed from public; Show restores Published.

**Acceptance Scenarios**:

1. **Given** Manager được gán Property P, **When** mở Content Moderation (SCR-65), **Then** chỉ reviews có Room thuộc P hiển thị; lọc All / Published / Hidden.
2. **Given** review **Published** trong scope, **When** Manager chọn Hide với xác nhận, **Then** status → **Hidden**; không còn công khai; aggregate phòng cập nhật.
3. **Given** review **Hidden** trong scope, **When** Manager chọn Show, **Then** status → **Published**; hiển thị lại công khai.
4. **Given** Manager không được gán Property P, **When** cố moderate review phòng thuộc P, **Then** từ chối.
5. **Given** Manager moderate, **When** thành công, **Then** ghi nhận người kiểm duyệt và thời điểm (assumption: `moderatedBy`, `moderatedAt` audit fields).

---

### User Story 5 - Admin kiểm duyệt đánh giá toàn hệ thống (Priority: P2)

Là **Admin**, tôi muốn xem và ẩn/hiện reviews trên **toàn hệ thống** (tab Content Moderation trong SCR-56), để xử lý nội dung vi phạm khi Manager leo thang hoặc cross-property.

**Why this priority**: FR-14 "Manager hoặc Admin"; Admin global oversight.

**Independent Test**: Admin sees all reviews globally → Hide/Show any review regardless of property.

**Acceptance Scenarios**:

1. **Given** Admin, **When** mở System Administration → Content Moderation tab (SCR-56), **Then** xem reviews toàn hệ thống với filter Published/Hidden.
2. **Given** bất kỳ review, **When** Admin Hide hoặc Show, **Then** cập nhật status tương tự Manager; aggregate phòng cập nhật.
3. **Given** Admin, **When** moderate, **Then** không bị giới hạn property scope.

---

### User Story 6 - Điều hướng từ booking đã check-out (Priority: P2)

Là **Customer**, sau check-out tôi muốn được gợi ý viết review từ **Booking Detail** hoặc danh sách booking khi chưa review, để dễ tìm form đánh giá.

**Why this priority**: UX entry point from booking flow; screendesign "Write Review" CTA.

**Independent Test**: Booking CHECKED_OUT without review → CTA "Write Review" → SCR-25 pre-filled bookingId; with review → CTA hidden or "View My Review".

**Acceptance Scenarios**:

1. **Given** booking **Checked-out** chưa có review, **When** Customer mở Booking Detail, **Then** hiển thị nút **Write Review** dẫn tới SCR-25 với `bookingId`.
2. **Given** booking đã có review, **When** mở Booking Detail, **Then** không hiển thị Write Review (hoặc link tới My Reviews).
3. **Given** booking chưa Checked-out, **When** mở detail, **Then** không hiển thị Write Review.

---

### Edge Cases

- Customer submit review ngay sau check-out trong khi aggregate chưa sync → public page reflects within reasonable refresh (assumption: immediate on commit).
- Comment chứa nội dung rỗng/whitespace only → validation reject.
- Review edit sau khi Hidden → vẫn Hidden; nội dung cập nhật nhưng không public until Show.
- Customer delete review → recalculate room average; booking eligible for new review (assumption v1).
- Manager Hide review đang là review duy nhất Published → average becomes null/0 display.
- Guest (unauthenticated) chỉ đọc public reviews — không tạo/sửa.
- Employee không có quyền review moderation (out of scope).
- Profanity filter — không bắt buộc v1; moderation thủ công qua Hide (assumption).
- FR-03 `GET /rooms/{id}/reviews` — FR-14 provides data; FR-03 owns discovery route (boundary).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Customer MUST tạo Review chỉ khi booking status **Checked-out** và thuộc chính Customer.
- **FR-002**: Mỗi **Booking** MUST có tối đa **một** Review (unique per booking).
- **FR-003**: Review MUST liên kết **bookingId**, **roomId**, **customerId** — roomId derived from booking at create.
- **FR-004**: Review MUST có **rating** integer **1–5** (bắt buộc) và **comment** text (bắt buộc, tối thiểu 20 ký tự).
- **FR-005**: Review mới MUST có status **Published** mặc định.
- **FR-006**: Customer MUST xem danh sách reviews **của mình** (SCR-24) với rating, comment, phòng, trạng thái, ngày.
- **FR-007**: Customer MUST chỉnh sửa rating/comment review **của mình** bất kỳ lúc nào (không phụ thuộc Published/Hidden).
- **FR-008**: Customer MUST xóa review **của mình**; sau xóa booking MAY được review lại (assumption v1).
- **FR-009**: Hệ thống MUST cung cấp **đánh giá công khai** theo phòng — chỉ status **Published**.
- **FR-010**: Hệ thống MUST tính **average rating** và **total review count** per room từ Published reviews only; cập nhật khi create/update/delete/moderate.
- **FR-011**: Manager MUST xem và moderate (Hide/Show) reviews scoped theo **Property được gán** (qua Room).
- **FR-012**: Hide MUST chuyển status **Published → Hidden**; Show MUST chuyển **Hidden → Published**.
- **FR-013**: Admin MUST moderate reviews **toàn hệ thống** (Hide/Show).
- **FR-014**: Moderation actions MUST ghi audit (`moderatedBy`, `moderatedAt` hoặc ActivityLog REVIEW_HIDDEN/REVIEW_PUBLISHED).
- **FR-015**: Public review list MUST **không** hiển thị thông tin nhạy cảm (email, phone) — chỉ tên hiển thị reviewer (assumption: customer display name).
- **FR-016**: Hệ thống MUST từ chối tạo review khi booking không Checked-out hoặc đã có review.
- **FR-017**: FR-14 MUST cung cấp dữ liệu cho FR-03 public room reviews read API — FR-03 không ghi Review.
- **FR-018**: Hệ thống MUST ghi **ActivityLog**: REVIEW_CREATED, REVIEW_UPDATED, REVIEW_DELETED, REVIEW_HIDDEN, REVIEW_PUBLISHED.

### Key Entities

- **Review**: id, customerId, bookingId, roomId, rating (1–5), comment, status (Published | Hidden), moderatedBy, moderatedAt, createdAt, updatedAt.
- **Booking** (read/link): FR-04 owns; FR-14 validates Checked-out + one-review rule.
- **Room** (read/link): FR-08 owns; denormalized aggregates `averageRating`, `totalReviews` on room or computed view (assumption: cached on room for discovery performance).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Customer hoàn tất gửi review (form + submit) trong **dưới 2 phút** sau khi mở SCR-25.
- **SC-002**: **100%** review tạo thành công gắn đúng booking/room/customer và status **Published**.
- **SC-003**: **100%** attempt tạo review thứ hai cho cùng booking bị từ chối.
- **SC-004**: **100%** attempt review trước Checked-out bị từ chối.
- **SC-005**: **100%** public room review lists chỉ hiển thị **Published** — zero Hidden leakage.
- **SC-006**: Room average rating cập nhật chính xác trong **dưới 5 giây** sau create/update/delete/moderate.
- **SC-007**: **100%** Manager moderation scoped đúng Property — cross-property Hide bị từ chối.
- **SC-008**: **95%** khách tìm thấy ít nhất một review Published trên phòng có ≥1 review (discovery UX).

## Assumptions

- **Checked-out** = booking status `CHECKED_OUT` (FR-04 enum).
- Comment max **1000** ký tự (align figma SCR-30); rating required 1–5 stars.
- Customer display name from user profile (FR-02) for public reviews — no anonymous reviews v1.
- Delete review **hard delete** or soft with exclusion from counts — booking slot freed for re-review after delete (assumption v1).
- Room aggregates: denormalize `average_rating`, `total_reviews` on `rooms` table updated transactionally (assumption for FR-03 performance).
- Manager moderation UI: SCR-65 Manager layout; Admin uses SCR-56 Content Moderation tab.
- No auto-moderation / ML content filter v1 — manual Hide only.
- Pagination: 20 per page on public and my-reviews lists.
- Complaint/khiếu nại workflow **out of scope** — separate feature per §8 boundary note.
- Employee role: no review write or moderate access.
- Edit review does not reset to pending moderation — stays current status (Published stays public after edit unless Hidden).
