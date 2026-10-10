# Feature Specification: [FEATURE NAME]

**Feature Branch**: `[NNN-feature-slug]`

**Created**: [YYYY-MM-DD]

**Status**: Draft | In Review | Approved

**Input**: User description: "[Mô tả ngắn / FR-xx từ Specification_v2 + màn hình SCR-xx]"

**Nguồn tham chiếu**: `docs/Specification_v2.md` §[...]; `docs/screen.md` / `docs/api-spec-by-screen.md`; `CONTEXT.md` (cùng folder)

**CONTEXT**: [./CONTEXT.md](./CONTEXT.md) — phải hoàn thành trước khi Approve

---

## User Scenarios & Testing *(mandatory)*

<!--
  User stories PRIORITIZED (P1, P2, P3…).
  Mỗi story phải INDEPENDENTLY TESTABLE (MVP riêng được).
  Viết ngôn ngữ nghiệp vụ — KHÔNG ghi Java/React/SQL trong phần này.
-->

### User Story 1 - [Brief Title] (Priority: P1)

[Mô tả hành trình người dùng bằng ngôn ngữ tự nhiên]

**Why this priority**: [Giá trị / vì sao P1]

**Independent Test**: [Cách test độc lập story này]

**Acceptance Scenarios**:

1. **Given** [trạng thái ban đầu], **When** [hành động], **Then** [kết quả]
2. **Given** [trạng thái ban đầu], **When** [hành động], **Then** [kết quả]

---

### User Story 2 - [Brief Title] (Priority: P2)

[Mô tả]

**Why this priority**: [...]

**Independent Test**: [...]

**Acceptance Scenarios**:

1. **Given** [...], **When** [...], **Then** [...]

---

### User Story 3 - [Brief Title] (Priority: P3)

[Mô tả]

**Why this priority**: [...]

**Independent Test**: [...]

**Acceptance Scenarios**:

1. **Given** [...], **When** [...], **Then** [...]

---

<!-- Thêm story khác nếu cần -->

### Edge Cases

- What happens when [điều kiện biên]?
- How does system handle [lỗi / thiếu quyền / hết hạn]?
- [Trường hợp race / trùng / empty state]?

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST [năng lực cụ thể, test được]
- **FR-002**: System MUST [năng lực cụ thể]
- **FR-003**: Users MUST be able to [tương tác chính]
- **FR-004**: System MUST [yêu cầu dữ liệu / trạng thái]
- **FR-005**: System MUST [hành vi bảo mật / phân quyền nếu có]

<!-- Đánh dấu chỗ chưa rõ:
- **FR-00X**: System MUST [NEEDS CLARIFICATION: ...]
-->

### Key Entities *(nếu feature có dữ liệu)*

- **[Entity 1]**: [Ý nghĩa, thuộc tính chính — không schema SQL]
- **[Entity 2]**: [Quan hệ với entity khác]

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: [Metric đo được, không phụ thuộc tech]
- **SC-002**: [Metric]
- **SC-003**: [Metric trải nghiệm / nghiệp vụ]

---

## Assumptions

- [Giả định về user / phạm vi / phụ thuộc hệ thống hiện có]
- [Default đã chọn khi stakeholder chưa trả lời OQ]
- [Out of scope v1 nếu có]

---

## Out of Scope *(recommended)*

- [Những gì cố ý không làm trong feature này]

---

## Dependencies *(recommended)*

- Spec / module: `[NNN-other-feature]` — [vì sao]
- Actors / roles: Guest | Customer | Employee | Manager | Admin
