# CONTEXT.md — Homestay Booking System (toàn dự án / Module Layer 2)
# Người viết: @team HomestayBooking | Ngày: 2026-07-24
# Module: `sdd/services/homestay/module.md`
# Specs: `sdd/services/homestay/features/001` … `023` (mỗi feature có CONTEXT + spec riêng)
# Global: `sdd/global/*` | Luật: `CONSTITUTION.md` | Agent: `AGENTS.md` / `CLAUDE.md`
#
# Hướng dẫn: CONTEXT module điền TRƯỚC khi mở rộng catalog feature hoặc Spec Sync lớn.
# Feature mới vẫn copy `features/_templates/CONTEXT.md` → `features/NNN-slug/CONTEXT.md`.

---

## 1. PROBLEM STATEMENT

Chủ homestay / resort và nhân viên vận hành đang quản lý phòng, đặt chỗ, cọc, hợp đồng và dọn phòng **thủ công** (sổ, Excel, Zalo/Facebook). Hệ quả:

- **Overbooking / mất chỗ:** không có nguồn sự thật chung về phòng trống theo ngày → bán trùng hoặc “treo” phòng vì khách giữ chỗ rồi bỏ.
- **Doanh thu cọc lệch:** cọc 40% / còn lại 60%, hủy gần ngày, no-show — chính sách hoàn tiền không nhất quán → chủ hoặc khách thiệt.
- **Vận hành tại chỗ đứt đoạn:** check-in/out, thu phần còn lại, kiểm tra hư hại, dọn phòng, bảo trì nằm rời nhau; Manager/Employee không cùng một property scope rõ ràng.
- **Đa chi nhánh khó quản:** Admin cần nhiều property, gán Manager, báo cáo toàn hệ thống; Manager chỉ nên thấy property được gán — làm tay dễ lộ data / sai quyền.
- **Khách thiếu self-service:** tìm phòng, đặt, trả cọc online, xem hợp đồng PDF, theo dõi booking phải nhờ nhân viên trả lời tin nhắn.

Pain cần giải (không nhảy giải pháp ở bước này): **mất doanh thu + mất chỗ + hỗn loạn vận hành + trải nghiệm khách kém + rủi ro phân quyền đa property.**

---

## 2. DOMAIN KNOWLEDGE

### 2.1 Hệ thống là gì

| Thuật ngữ | Nghĩa trong dự án |
|-----------|-------------------|
| Homestay Booking System | Web app full-stack số hóa đặt phòng + vận hành property (modular monolith: 1 API Spring Boot + 1 SPA React) |
| Property | Chi nhánh / cơ sở (resort/homestay); đơn vị phân quyền Manager/Employee |
| Floor / Room | Cấu trúc Property → Floor → Room |
| Booking | Yêu cầu lưu trú có vòng đời status chuẩn; khóa tồn kho theo khoảng ngày |
| Inventory | Không bán trùng phòng trong `[checkIn, checkOut)` (half-open) + `RoomStatusBlock` (bảo trì/OOS) |
| Deposit / Remaining | Snapshot lúc tạo booking: **40% cọc** / **60% còn lại** (stay); damage fee **tách** |
| Accommodation Contract | Hợp đồng PDF sinh sau khi cọc confirmed; gửi email |
| Hold timeout | `PENDING_DEPOSIT` hết hạn chưa cọc → `CANCELLED`, nhả phòng (mặc định **10 phút**) |
| No-show | `CONFIRMED` không check-in trước deadline → `NO_SHOW`, giữ cọc, nhả phòng |
| Property scope | Manager/Employee chỉ thao tác data thuộc property được gán |
| SCR-xx | Mã màn hình master trong `docs/screen.md` (~69 màn) |

### 2.2 Canonical booking statuses (bắt buộc)

`PENDING_DEPOSIT` → `CONFIRMED` → `CHECKED_IN` → `PENDING_INSPECTION` → `PENDING_DAMAGE_PAYMENT` → `CHECKED_OUT`  
Nhánh kết: `CANCELLED` | `NO_SHOW`

Agent / người **không invent** status ngoài enum này (`AGENTS.md` / feature `004`).

### 2.3 Actors

| Role | Pain / nhu cầu chính |
|------|----------------------|
| Guest | Tìm phòng, xem lịch, đăng ký/đăng nhập |
| Customer | Đặt phòng, VNPay, hủy theo tier, hợp đồng, review, ticket |
| Employee | HK / maintenance / inspection trong property được gán |
| Manager | Booking, CI/CO, thu remaining cash, hợp đồng, gán việc, báo cáo property |
| Admin | Property, gán Manager, customer directory, escalation damage >5tr, global report, promo |

### 2.4 Quy tắc nghiệp vụ “bất thành văn” đã chốt (edu)

- Online pay = **VNPay**; confirm qua **Return URL only** (IPN deferred — TD-012b).
- **Cash remaining tại quầy** check-in được giữ (`remainingCollected` → CASH PAID) — không mở lại CK/biên lai (TD-011 deferred trừ cash desk).
- Customer cancel hoàn **cọc**: ≥7d 100% / 3–7d 50% / &lt;3d 0%; Manager cancel **100% cọc**.
- Damage fee &gt; 5.000.000 VND cần Admin co-approval.
- Conflict docs: feature `spec.md` > `AGENTS.md` > `CLAUDE.md` > `docs/Specification_v2.md`.

### 2.5 Feature catalog (Layer 3)

001 Auth · 002 Profile · 003 Discovery · **004 Booking** · **005 Calendar** · 006–009 Property/Structure/Room/Customer · **010 Contract** · 011 Billing · **012 Payment** · 013–023 Maintenance … Inspection/Damage.  
Chi tiết: `module.md`. Sprint 2 primary: **004, 005, 010, 012**.

---

## 3. STAKEHOLDERS

| Ai | Được lợi / ảnh hưởng | Quyền quyết định |
|----|----------------------|------------------|
| Chủ property / vận hành thực tế (persona) | Giảm overbooking, thu cọc đúng, vận hành CI/CO–HK | Yêu cầu nghiệp vụ qua Spec Sync |
| Customer / Guest | Self-service đặt & trả tiền | Feedback UAT; không veto stack |
| Manager / Employee (persona) | Tool đúng scope property | Feedback UX vận hành |
| Admin (persona) | Đa chi nhánh, báo cáo, escalate | Policy hệ thống |
| Team HomestayBooking (Dev / Spec Architect / Agent Pilot / Quality) | Delivery + điểm môn | Spec Sync (Monday), Constitution vote, PR approve |
| Mentor / giảng viên | Rubric, scope edu | Có thể reopen TD deferred; không sửa Constitution một mình |
| AI Agent (Cursor / Claude) | Implement theo Spec | **Không** quyết định scope; human review plan + PR |

---

## 4. CONSTRAINTS (ràng buộc không thể thay đổi tùy tiện)

### 4.1 Tech (`CONSTITUTION.md` Art.1–3)

- Java 17 + Spring Boot 3 + Spring Security + Spring Data JPA + Maven  
- SQL Server (không NoSQL không vote)  
- React 18 + TypeScript + Vite; TanStack Query + Zustand; Tailwind + `components/ui`  
- Auth: JWT access+refresh + BCrypt (≥12) + Google OAuth — không session cookie primary  
- API: REST (`/api/...`, prefer `/api/v1/...`) — không GraphQL không vote  
- Architecture: **modular monolith** — không tách microservice / Kafka / BFF trừ khi amend Constitution  
- Upload max **10MB**; secrets chỉ env; JPA/JPQL only; RBAC + property scope  
- Workspaces canonical: `Backend/`, `Frontend/`  
- Spec-as-Code OPTION A: `sdd/` cùng repo với code  

### 4.2 Business / product

- Không overbooking; inventory half-open + lock trong cùng DB process  
- Snapshot giá booking không bị đổi khi sửa `PricePerNight` sau  
- Status & refund/deposit rules theo feature specs (đặc biệt `004`, `012`)  
- Edu scope cut (không tự implement): VNPay reconcile SCR-52 (TD-010), bank transfer + CK verify (TD-011), VNPay IPN (TD-012b) — trừ mentor reopen  
- ~69 SCR trong `docs/screen.md` là master số màn  

### 4.3 Process / time

- Spec-First: không code feature trước Spec Sync approve `CONTEXT` + `spec`  
- Sprint hiện tại (context 2026-07): Sprint 2 — booking hardening, payment, calendar, contract  
- PR ≤ ~400 dòng; ≥1 approve; coverage critical Auth/Booking/Payment ≥80% LINE khi đụng path đó  
- Definition of Done: `AGENTS.md` §6 + `sdd/sprints/pre-commit-checklist.md`  

---

## 5. ASSUMPTIONS (giả định — rủi ro nếu sai)

1. **Edu sandbox đủ** với VNPay Return URL + cash remaining quầy; mentor không bắt buộc IPN/CK/reconcile trong rubric hiện tại.  
2. **Một timezone vận hành** mặc định nghiệp vụ ngày: Asia/Ho_Chi_Minh (jobs/hold/no-show neo theo config server + `LocalDate` booking).  
3. **Date-only check-in** (`LocalDate`) đủ cho no-show = `checkInDate.atStartOfDay() + 24h` (đã clarify 004) — không cần giờ check-in riêng trong Sprint 2.  
4. **Modular monolith** đủ scale môn học / số property demo; chưa cần split service.  
5. **SMTP + Google OAuth + VNPay sandbox** luôn cấu hình được trên máy DEV qua `.env` (không commit secret).  
6. Narrative `docs/Specification_v2.md` có thể lệch tier/payment cũ — team luôn ưu tiên feature `spec.md` đã clarify.  
7. FE Vitest/e2e Playwright chưa phải gate cứng bằng BE coverage-critical (theo tech-debt / CI hiện tại).  
8. Damage dispute phía Customer (feature 023 mở rộng) có thể deferred optional sau edu core.

---

## 6. OPEN QUESTIONS (cần Spec Sync / mentor khi mở rộng)

1. Mentor có **reopen** TD-010 / TD-011 / TD-012b trước khi kết thúc môn không?  
2. Retention / xóa tài khoản Customer (hard delete vs anonymize) — khóa trong admin/profile specs tới mức nào cho UAT?  
3. Invoice/billing layer (`011`) bắt buộc trước demo cuối hay báo cáo Manager (`016`) đủ?  
4. E2E Playwright có phải DoD demo cuối không, hay manual UAT + Postman đủ?  
5. Đa timezone property thật (khách quốc tế) — có trong scope post-edu không?  
6. About CMS / Landing copy — BE API đã đủ cho FE đã ship chưa (LESSON-002)?  
7. Customer damage dispute UX — làm trong 023 hay tách feature sau?

---

## Related documents (đọc kèm)

| Doc | Vai trò |
|-----|---------|
| `sdd/services/homestay/module.md` | Catalog feature + boundaries |
| `sdd/global/system-architecture.md` | Topology + communication |
| `sdd/global/security.md` | AuthN/AuthZ / OWASP policy |
| `sdd/global/data-governance.md` | PII / ownership / retention |
| `sdd/global/api-standards.md` | REST envelope / pagination |
| `sdd/sprints/tech-debt.md` | Done vs Deferred edu |
| `docs/Specification_v2.md` | SRS narrative (secondary) |
| `docs/screen.md` | SCR master |
| Feature `*/CONTEXT.md` + `spec.md` | Nguồn sự thật từng FR |

---

**Next:** Feature mới → copy `_templates/CONTEXT.md` → điền pain riêng → Spec Sync → `spec.md` → plan/tasks → implement.  
Mọi thay đổi giả định/OQ ở cấp dự án → cập nhật file này trong Monday Spec Sync + ghi ADR/LESSON vào `CLAUDE.md` nếu cần.
