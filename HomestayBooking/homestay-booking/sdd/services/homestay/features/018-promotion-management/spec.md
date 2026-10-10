# Feature Specification: FR-18 Promotion Management

**Feature Branch**: `020-promotion-management`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-18 Promotion Management — dựa vào docs (Specification_v2.md § FR-18, §5 Promotion, §8 Administration acceptance, api-spec-by-screen SCR-57/58, screen.md, screendesign.md, entity-ui-mapping.md §1.16, frontend PromotionMgmtPage.tsx, publicApi.ts fetchPromotions, LandingPage.tsx)"

**Phụ thuộc**: FR-01 (RBAC Admin; public read không cần auth); FR-03 (Landing SCR-01 hiển thị banner — FR-18 owns dữ liệu Promotion, FR-03 owns discovery layout). **Ranh giới**: FR-17 Administration **không** bao gồm Promotion; FR-18 owns **banner khuyến mãi** CRUD Admin + public read; **không** mã giảm giá áp dụng vào booking/checkout v1; **không** hệ thống marketing tự động (§9); Manager routes `/manager/promotions` hiện tại **sai actor** — chuẩn hóa Admin SCR-57/58.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Khách xem banner khuyến mãi trên trang chủ (Priority: P1)

Là **Guest** (chưa đăng nhập hoặc bất kỳ vai trò), tôi muốn **xem các banner khuyến mãi đang hoạt động** trên trang chủ với tiêu đề, mô tả ngắn và nút hành động, để biết chương trình ưu đãi và điều hướng tới nội dung liên quan (SCR-01).

**Why this priority**: Specification Guest permission "Xem banner khuyến mãi trên trang chủ"; giá trị marketing ngay khi có banner Active.

**Independent Test**: Trang chủ hiển thị chỉ banner **Active**, sắp xếp theo thứ tự Admin cấu hình; banner Inactive không hiển thị; CTA điều hướng đúng URL.

**Acceptance Scenarios**:

1. **Given** có ít nhất một Promotion **Active**, **When** Guest mở trang chủ, **Then** thấy danh sách banner với `subtitle`, `title`, `description` (nếu có), nút CTA (`ctaText`).
2. **Given** Promotion **Inactive**, **When** Guest mở trang chủ, **Then** banner đó **không** hiển thị.
3. **Given** nhiều banner Active, **When** Guest xem trang chủ, **Then** hiển thị theo `sortOrder` tăng dần (số nhỏ trước).
4. **Given** Guest nhấn CTA trên banner, **When** `ctaUrl` hợp lệ (đường dẫn nội bộ hoặc URL), **Then** điều hướng tới đích tương ứng.
5. **Given** không có banner Active trong hệ thống, **When** Guest mở trang chủ, **Then** hiển thị empty state hoặc fallback mặc định (assumption: không lỗi trang).

---

### User Story 2 - Admin xem danh sách banner khuyến mãi (Priority: P1)

Là **Admin**, tôi muốn **xem danh sách** tất cả chương trình/banner khuyến mãi (Active và Inactive), để quản lý nội dung hiển thị trang chủ (SCR-57).

**Why this priority**: FR-18 "Admin quản lý danh sách các chương trình/banner khuyến mãi"; điểm vào CRUD.

**Independent Test**: Admin mở Promotion Management → thấy grid/danh sách với trạng thái Active/Inactive, thứ tự, actions Edit/Delete.

**Acceptance Scenarios**:

1. **Given** Admin đã đăng nhập, **When** mở Promotion Management (SCR-57), **Then** thấy danh sách banner: tiêu đề, subtitle, trạng thái Active/Inactive, thứ tự hiển thị.
2. **Given** Admin, **When** mở SCR-57, **Then** thấy nút **Add Promotion** dẫn tới tạo mới (SCR-58 hoặc form/modal tương đương).
3. **Given** không có banner, **When** Admin mở SCR-57, **Then** empty state với lời mời tạo banner đầu tiên.
4. **Given** Customer hoặc Manager, **When** cố truy cập quản lý promotion, **Then** từ chối.

---

### User Story 3 - Admin tạo và chỉnh sửa banner (Priority: P1)

Là **Admin**, tôi muốn **tạo mới** và **chỉnh sửa** banner với tiêu đề, phụ đề, mô tả, nút CTA, liên kết hành động và màu sắc/chủ đề hiển thị, để cập nhật nội dung marketing (SCR-58).

**Why this priority**: FR-18 explicit Tạo, sửa banner với title, CTA, màu sắc.

**Independent Test**: Admin tạo banner → lưu thành công → xuất hiện trên SCR-57 và (nếu Active) trang chủ; chỉnh sửa phản ánh ngay sau lưu.

**Acceptance Scenarios**:

1. **Given** Admin, **When** tạo banner với `title`, `subtitle`, `ctaText`, `ctaUrl`, `colorTheme` hợp lệ, **Then** lưu Promotion mới; mặc định **Active** hoặc theo lựa chọn Admin (assumption: default Active).
2. **Given** banner đã tồn tại, **When** Admin chỉnh sửa các trường trên và Save, **Then** cập nhật `updatedAt`; nội dung mới hiển thị trên Admin list và trang chủ (nếu Active).
3. **Given** `title` hoặc `subtitle` hoặc `ctaText`/`ctaUrl` trống, **When** Admin Save, **Then** validation error; không lưu.
4. **Given** `ctaUrl` không hợp lệ, **When** Save, **Then** validation error (assumption: relative path `/search` hoặc http/https URL).
5. **Given** Admin xem form, **When** nhập nội dung, **Then** có **preview** banner trước khi lưu (assumption theo UI hiện có).

---

### User Story 4 - Admin xóa, bật/tắt và sắp xếp thứ tự (Priority: P1)

Là **Admin**, tôi muốn **xóa** banner không còn dùng, **bật/tắt** hiển thị (Active/Inactive) và **điều chỉnh thứ tự** hiển thị, để kiểm soát nội dung trang chủ.

**Why this priority**: FR-18 explicit "Tùy chỉnh thứ tự hiển thị và bật/tắt (Active/Inactive)".

**Independent Test**: Admin set banner Inactive → biến mất khỏi trang chủ nhưng vẫn trong Admin list; đổi sortOrder → thứ tự trang chủ thay đổi; xóa → không còn trong list và trang chủ.

**Acceptance Scenarios**:

1. **Given** banner Active, **When** Admin chuyển **Inactive**, **Then** `isActive = false`; banner không còn trên trang chủ; vẫn hiển thị trong Admin list.
2. **Given** banner Inactive, **When** Admin chuyển **Active**, **Then** banner xuất hiện lại trên trang chủ (theo sortOrder).
3. **Given** Admin thay đổi `sortOrder` (số nguyên), **When** lưu, **Then** thứ tự trên trang chủ cập nhật tương ứng.
4. **Given** Admin xác nhận xóa, **When** Delete, **Then** Promotion bị xóa vĩnh viễn; không còn trên trang chủ và Admin list.
5. **Given** hai banner cùng `sortOrder`, **When** hiển thị công khai, **Then** sắp xếp phụ theo `createdAt` mới nhất trước (assumption tie-break).

---

### User Story 5 - Admin điều hướng SCR-57 ↔ SCR-58 (Priority: P2)

Là **Admin**, tôi muốn luồng **danh sách → form tạo/sửa → quay lại danh sách** rõ ràng, để quản lý banner thuận tiện (SCR-57 / SCR-58).

**Why this priority**: screen.md định nghĩa SCR-58 là dedicated page; P2 vì modal trên cùng trang cũng đáp ứng acceptance.

**Independent Test**: Admin click Add → form create; Edit → form prefill; Save/Cancel → quay SCR-57 list.

**Acceptance Scenarios**:

1. **Given** Admin trên SCR-57, **When** click Add Promotion, **Then** mở SCR-58 (trang riêng) hoặc modal tạo mới (assumption v1: modal hoặc route `/admin/promotions/new` đều chấp nhận).
2. **Given** Admin Save thành công, **When** hoàn tất, **Then** quay danh sách SCR-57 với banner mới/cập nhật.
3. **Given** Admin Cancel, **When** hủy form, **Then** không thay đổi dữ liệu; quay list.

---

### Edge Cases

- Banner với `ctaUrl` trỏ route không tồn tại — hệ thống vẫn cho phép lưu; lỗi 404 khi Guest click (assumption v1).
- Xóa banner đang là banner duy nhất Active — trang chủ empty/fallback, không lỗi.
- `colorTheme` không nằm trong palette — fallback theme mặc định (assumption: red/default gradient).
- **Hình ảnh banner**: Specification FR-18 bullet đề cập "hình ảnh" nhưng entity §5 dùng `ColorTheme` — v1 **gradient theo colorTheme**, không upload ảnh; image attachment **P2/out of scope** v1 (api-spec `attachments` không áp dụng v1).
- **Mã giảm giá / discountPercent**: api-spec SCR-58 payload `code`, `discountPercent` — **out of scope** v1; banner chỉ marketing/điều hướng, không giảm giá booking.
- Manager/Employee không có quyền quản lý promotion — Admin only.
- Hệ thống marketing tự động, A/B test, scheduling theo ngày — **out of scope** (§9).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống MUST cung cấp danh sách Promotion **Active** cho Guest trên trang chủ, sắp xếp theo `sortOrder`.
- **FR-002**: Guest MUST **không** thấy Promotion **Inactive**.
- **FR-003**: Admin MUST xem danh sách **tất cả** Promotion (Active + Inactive) trên SCR-57.
- **FR-004**: Admin MUST **tạo** Promotion với `title`, `subtitle`, `description` (optional), `ctaText`, `ctaUrl`, `colorTheme`, `isActive`, `sortOrder`.
- **FR-005**: Admin MUST **chỉnh sửa** Promotion đã tồn tại.
- **FR-006**: Admin MUST **xóa** Promotion với xác nhận.
- **FR-007**: Admin MUST **bật/tắt** `isActive` (Active/Inactive).
- **FR-008**: Admin MUST điều chỉnh `sortOrder` để kiểm soát thứ tự hiển thị trang chủ.
- **FR-009**: FR-18 MUST **từ chối** Customer, Manager, Employee truy cập CRUD promotion.
- **FR-010**: CTA trên banner MUST sử dụng `ctaText` và điều hướng `ctaUrl`.
- **FR-011**: Validation MUST bắt buộc `title`, `subtitle`, `ctaText`, `ctaUrl` khi tạo/sửa.
- **FR-012**: Promotion Management thuộc **FR-18** — **không** implement trong FR-17 Administration.
- **FR-013**: **Không** áp dụng mã giảm giá vào tổng tiền booking v1.
- **FR-014**: Public read endpoint MUST không yêu cầu đăng nhập (Guest).

### Key Entities

- **Promotion**: id, title, subtitle, description, ctaText, ctaUrl, colorTheme, isActive, sortOrder, createdAt, updatedAt.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Guest thấy banner Active trên trang chủ trong **dưới 3 giây** sau khi tải trang (95% sessions).
- **SC-002**: Admin hoàn tất tạo banner mới trong **dưới 3 phút** (90% sessions).
- **SC-003**: **100%** banner Inactive bị ẩn khỏi trang chủ.
- **SC-004**: **100%** thay đổi `sortOrder` phản ánh đúng thứ tự trên trang chủ sau reload.
- **SC-005**: **100%** truy cập CRUD promotion bởi non-Admin bị chặn.
- **SC-006**: Admin chỉnh sửa và lưu banner — nội dung cập nhật trên trang chủ trong **dưới 5 giây** sau save (khi Active).
- **SC-007**: Trang chủ không lỗi khi zero Active promotions (empty/fallback graceful).

## Assumptions

- **Actor**: SCR-57/58 **Admin only** — migrate từ `/manager/promotions` → `/admin/promotions`.
- **Public API**: `GET` promotions active-only cho Guest (SCR-01); align `publicApi.fetchPromotions` → `/api/v1/public/promotions`.
- **colorTheme**: palette cố định (red, blue, green, purple, orange) — khớp `PromotionMgmtPage.tsx`.
- **Image upload**: out of scope v1 — visual qua `colorTheme` gradient; entity §5 không có imageUrl.
- **Promo code / discount**: api-spec SCR-58 `code`, `discountPercent` — **không** dùng v1; banner marketing-only.
- **SCR-58**: dedicated page hoặc modal trên SCR-57 — cả hai đáp ứng US5; ưu tiên migrate UI hiện có.
- **FR-03**: LandingPage owns layout carousel/grid; FR-18 owns data + public endpoint.
- **ActivityLog**: optional `PROMOTION_CREATED/UPDATED/DELETED` P2 — không bắt buộc v1.
- **Max banners**: không giới hạn cứng v1; khuyến nghị ≤10 Active trên trang chủ (UX assumption).
