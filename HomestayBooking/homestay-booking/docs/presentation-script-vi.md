# Lời trình bày chi tiết — Tài liệu dự án Homestay Booking

**Mục đích:** Script đọc khi demo / bảo vệ / Spec Sync.  
**Ngày soạn:** 24/07/2026  
**Cập nhật:** 24/07/2026 (v1.2) — lời trình bày đầy đủ, câu đọc được; thống nhất path Spec_v2 dưới `sdd/services/homestay/`.  
**File:** `docs/presentation-script-vi.md`  
**Cách dùng:** Đoạn trong blockquote `>` = lời nói. Phần ngoài = ghi chú thời gian / demo. Có thể chia theo thành viên (phụ lục B).

**Thứ tự xung đột yêu cầu (nhắc mỗi khi mentor hỏi “theo đâu?”):**  
`sdd/services/homestay/features/<feature>/spec.md` > `AGENTS.md` > `CLAUDE.md` > `sdd/services/homestay/Specification_v2.md`

**Lớp tài liệu (nhanh):**

| Lớp | Path | Vai trò |
|-----|------|---------|
| Root | `CONSTITUTION`, `AGENTS`, `CLAUDE`, … | Luật team + AI memory |
| Global | `sdd/global/*` | Architecture, security, API, data |
| Module | `CONTEXT.md` + `module.md` + `Specification_v2.md` | Pain/domain toàn hệ + catalog + SRS narrative |
| Feature | `features/NNN-slug/` | Spec thi hành từng FR |
| Product UI | `docs/screen.md`, UAT, setup… | SCR / nghiệm thu / DEV |
| Deploy | `deployment/` + CI | Môi trường & gate chất lượng |

---

## 0. Mở đầu chung (1–2 phút)

> Xin chào thầy/cô và các bạn. Em xin trình bày **hệ thống tài liệu** của dự án **Homestay Booking System** (HomestayBooking).
>
> Nhóm không quản lý yêu cầu bằng file Word rời, mà vận hành theo **SDD + ADD** — Spec-Driven và Agent-Driven Development. Nghĩa là: yêu cầu nằm trong Git dưới dạng Spec; code cùng một repository; AI hỗ trợ viết code nhưng phải bám Spec và luật team; còn **con người** review và chịu trách nhiệm cuối cùng.
>
> Tài liệu được chia rõ tầng để tránh “mỗi người một nguồn sự thật”:
>
> 1. **Root** — `README`, `CONTRIBUTING`, `CONSTITUTION`, `AGENTS`, `CLAUDE`: luật team và bộ nhớ cho AI.  
> 2. **`sdd/global`** — luật kỹ thuật toàn hệ: kiến trúc, bảo mật, API, dữ liệu.  
> 3. **`sdd/services/homestay`** — CONTEXT toàn dự án, `module.md`, SRS narrative `Specification_v2.md`, và **23 feature** dưới `features/`.  
> 4. **`docs/`** — catalog màn hình SCR, design, UAT, environment, testing.  
> 5. **`deployment/` và CI** — Docker, environments, GitHub Actions.
>
> Trong phần tiếp theo, em trình bày từng nhóm file theo công thức: **dùng để làm gì**, **ai đọc**, và **điểm nhấn khi demo**. Nếu thầy/cô hỏi “theo tài liệu nào?”, nhóm luôn trả lời theo hierarchy: **feature `spec.md` trước**, rồi mới tới AGENTS, CLAUDE, và cuối cùng mới tới Specification_v2.

---

# PHẦN A — FILE GỐC REPO (GOVERNANCE)

## A1. `README.md` (khoảng 1 phút)

> **README.md** là cửa vào repository — người mới clone repo đọc file này trước.
>
> README giới thiệu ngắn: đây là Full-stack Web App gồm API và SPA, domain Hospitality / Property Management / Booking. Điểm nhóm muốn nhấn mạnh ngay từ dòng đầu: dự án **strict follow SDD + ADD**, không code theo cảm tính hay theo chat AI một chiều.
>
> README cũng là bản đồ tài liệu: `AGENTS.md` quy tắc cho AI agent; `CLAUDE.md` ghi ADR, lessons và sprint notes; `CONSTITUTION.md` là luật bất biến; thư mục `sdd/` chứa Spec-as-Code từ global xuống module CONTEXT rồi tới từng feature; thư mục `docs/` giữ UI screens, mapping và testing notes.
>
> Tech stack đã khóa trong README: Backend Java 17, Spring Boot 3, JPA, Maven; Frontend React 18, TypeScript, Vite, TanStack Query, Zustand; Database SQL Server; styling Tailwind kèm UI kit kiểu shadcn.
>
> Nếu muốn đóng góp đúng quy trình, README dẫn sang **CONTRIBUTING.md**.
>
> **Khi demo:** mở README trên IDE hoặc GitHub trước, rồi drill-down từng file được liệt kê — chứng minh repo “tự giải thích được”.

---

## A2. `CONTRIBUTING.md` (khoảng 2 phút)

> **CONTRIBUTING.md** là playbook đóng góp — trả lời câu hỏi: “Vào dự án này thì làm việc theo thứ tự nào?”
>
> Nhóm quy định pipeline **bốn pha**:
>
> **Pha 1 — Context Setup.** Trước khi viết code hay prompt AI, phải đọc `CONSTITUTION` và `CLAUDE`. Nếu task liên quan nhiều feature, đọc thêm CONTEXT module ở `sdd/services/homestay/CONTEXT.md`.
>
> **Pha 2 — Specification, tức SDD.** Mỗi feature bắt đầu bằng `CONTEXT.md`, rồi mới viết `spec.md` theo kiểu EARS — WHEN / THE system SHALL — kèm Acceptance Criteria và Out of Scope. **Không có Spec Sync approve thì không được code.**
>
> **Pha 3 — Agentic Execution, tức ADD.** Từ Spec sinh `plan.md`, cắt thành `tasks.md`, rồi implement từng task nhỏ bằng Cursor hoặc Claude Code.
>
> **Pha 4 — Human Review.** Test phải xanh, Acceptance Criteria phải đạt, rồi mới merge.
>
> Luật dành cho AI cũng ghi rõ trong CONTRIBUTING: đọc AGENTS và Constitution; không approve thứ mình không giải thích được; **Spec là nguồn sự thật**; tránh Blind Trust — tin mù kết quả AI — và Context Blindness — làm khi chưa đọc context.
>
> Skills Spec Kit nằm ở `.cursor/skills/speckit-*`.
>
> **Khi demo:** nhấn một câu — “AI được phép giúp code, nhưng **con người là gatekeeper**.”

---

## A3. `CONSTITUTION.md` (khoảng 3–4 phút)

> **CONSTITUTION.md** là hiến pháp dự án, phiên bản 1.0, phê chuẩn ngày 23 tháng 7 năm 2026. Mọi thay đổi phải được **toàn team đồng thuận** — không sửa một mình.
>
> **Điều 1 — Tech stack bất biến.** Backend Java 17 và Spring Boot; database SQL Server; Frontend React 18 với Vite; state dùng TanStack Query và Zustand; UI Tailwind; auth JWT cộng BCrypt và Google OAuth; API REST. Ba bản constitution phải cùng nghĩa: file root này, `.specify/memory/constitution.md`, và `sdd/global/constitution.md`.
>
> **Điều 2 — Coding standards.** TypeScript strict; Backend layered Controller → Service → Repository; chỉ trả DTO, không leak Entity; business logic chỉ nằm ở Service; khuyến nghị khoảng 40 dòng một hàm, 300 dòng một file; logging bằng SLF4J, không `System.out`.
>
> **Điều 3 — Security không thương lượng.** BCrypt cost tối thiểu 12; secrets chỉ qua biến môi trường; truy vấn bằng JPA, không nối chuỗi SQL; upload tối đa 10MB; validate input; CORS whitelist; RBAC kèm property scope; tuyệt đối không bypass luật booking hay payment.
>
> **Điều 4 — Git workflow.** Nhánh `main` và `DEV` được bảo vệ; đặt tên nhánh `feat`, `fix`, `hotfix`, `spec`…; commit theo dạng `type(scope): mô tả`; pull request cần ít nhất một người approve và nên dưới khoảng 400 dòng thay đổi.
>
> **Điều 5 — Testing.** Code critical — booking, payment, auth — hướng tới coverage từ 80% trở lên; unit test bắt buộc cho logic nghiệp vụ; API phải có happy path và error path; test cũ fail thì không merge.
>
> **Điều 6 — AI agent.** Phải đọc AGENTS và CLAUDE; review plan trước khi cho agent chạy; human refactor sau mỗi cụm 3 đến 5 task; **không invent** booking status ngoài enum chuẩn.
>
> **Điều 7 — Review process.** Thứ Hai Spec Sync duyệt yêu cầu; buổi Agent Jam review code; hotfix khẩn được phép nhưng phải có post-mortem trong 48 giờ.
>
> **Khi demo:** giải thích vì sao team không nhảy stack giữa chừng — Constitution khóa stack để AI và người cùng một “luật chơi”.

---

## A4. `AGENTS.md` (khoảng 3 phút)

> **AGENTS.md** là context bắt buộc cho mọi AI agent làm việc trên repo. File này nói rõ overview dự án, tech stack **STRICT**, nguyên tắc kiến trúc layered, quy ước đặt tên file, và các **forbidden patterns**.
>
> Phần nghiệp vụ quan trọng nhất là **tám trạng thái booking chuẩn**, không được invent thêm:
>
> `PENDING_DEPOSIT` chuyển `CONFIRMED`, rồi `CHECKED_IN`, rồi `PENDING_INSPECTION`, rồi có thể `PENDING_DAMAGE_PAYMENT`, rồi `CHECKED_OUT`; hai nhánh kết thúc khác là `CANCELLED` và `NO_SHOW`.
>
> Các rule nổi bật agent phải nhớ: cọc 40% và còn lại 60%; hold timeout mặc định **10 phút**; no-show sau 24 giờ kể từ mốc check-in; customer hủy hoàn cọc theo tier từ bảy ngày trở lên một trăm phần trăm, từ ba đến dưới bảy ngày năm mươi phần trăm, dưới ba ngày không hoàn; Manager hủy hoàn một trăm phần trăm cọc; damage fee trên năm triệu cần Admin đồng duyệt; không overbooking.
>
> Definition of Done cũng nằm ở đây: có unit test; nếu đụng Auth, Booking, Payment thì chạy `mvn -Pcoverage-critical verify`; lint sạch; Swagger cập nhật; HTTP status đúng; không để TODO trong phạm vi đã làm xong.
>
> Sprint 2 primary là feature **004, 005, 010, 012**. Các khoản deferred edu gồm TD-010, TD-011 và TD-012b — agent **không tự mở** trừ khi Spec Sync / mentor reopen.
>
> **Khi demo:** mở section Forbidden Patterns và enum status — trả lời rõ “AI được phép / không được phép làm gì”.

---

## A5. `CLAUDE.md` (khoảng 3–4 phút)

> **CLAUDE.md** là project memory — đặc biệt cho Claude Code, nhưng cả team cũng dùng như nhật ký quyết định.
>
> Phần **ADR** — Architecture Decision Records — ghi các quyết định đã chốt: JWT thay session; JPA trên SQL Server kèm pessimistic lock phòng; Frontend tách server state và client state bằng Query và Zustand; enum booking theo feature 004; cọc 40/60 và Outbox cho hợp đồng; Spec Kit qua cursor-agent; thanh toán online bằng VNPay confirm qua **Return URL**, giữ cash remaining tại quầy, chưa làm IPN.
>
> Phần **LESSON** ghi bài học từ incident và code review: không double-declare export gây trắng trang Vite; About CMS thiếu BE endpoint; hold đúng 10 phút; đã có NoShowJob; legacy check-in/out bị block; cancel phải theo tier chứ không flat 50%; validate file trước khi ghi disk; index/filter theo property, room, booking để tránh full scan.
>
> Phần sprint notes liệt kê việc Done theo conflict-fix và P0–P1, việc Deferred edu, lệnh chạy Frontend và Backend, cùng danh sách “never edit” như `.env`, `node_modules`, `target`.
>
> **Khi demo:** chứng minh team không chỉ có code — còn **ghi nhớ quyết định và bài học**, giảm lặp lại lỗi cũ.

---

# PHẦN B — `sdd/` SPEC-AS-CODE

## B1. `sdd/README.md` (khoảng 2 phút)

> Thư mục **`sdd/`** là Spec-as-Code theo **OPTION A monorepo**: Spec nằm cùng Git với code, không tách repo riêng.
>
> Cấu trúc gồm ba lớp chính:
>
> ```text
> sdd/
> ├── global/                         # Layer 1 — luật chung hệ thống
> ├── services/homestay/              # Layer 2 — module Homestay
> │   ├── CONTEXT.md                  # CONTEXT toàn dự án
> │   ├── module.md                   # Catalog feature 001–023
> │   ├── Specification_v2.md         # SRS narrative (thứ cấp)
> │   └── features/001…023/           # Layer 3 — spec thi hành từng FR
> └── sprints/                        # backlog, tech-debt, checklist
> ```
>
> Code thực thi nằm ở `Backend/` và `Frontend/`. Tài liệu UI, UAT và setup vẫn ở `docs/`. Khi xung đột: feature `spec.md` thắng AGENTS, CLAUDE, và **Specification_v2** — path canonical hiện tại là `sdd/services/homestay/Specification_v2.md`.
>
> **Khi demo:** chỉ cần mở cây thư mục `sdd/` trên IDE — mentor thấy ngay kỷ luật “spec trước, code sau”.

---

## B2. `sdd/global/constitution.md` (khoảng 1–2 phút)

> Đây là bản constitution **Layer 1** trong catalog SDD. Nội dung tóm tắt cùng tinh thần root: Spec-First; stack bất biến; layered architecture kèm DTO; security chi tiết ở `security.md`; test coverage critical từ 80% trở lên.
>
> Em nhấn lại: ba bản constitution — root, Spec Kit memory, và file này — phải **đồng nghĩa**. Hierarchy đọc tài liệu: global → CONTEXT module cộng `module.md` → từng folder `features/[NNN]-slug/`.

---

## B3. `sdd/global/system-architecture.md` (khoảng 4–5 phút)

> File này mô tả **kiến trúc tổng thể** phiên bản 1.0: topology dịch vụ và cách các phần giao tiếp.
>
> **Overview:** hệ thống là **modular monolith** — một process API Spring Boot 3 trên Java 17, một SPA React phục vụ năm role, database SQL Server, auth JWT cộng Google và BCrypt. Workspace code chuẩn là `Backend/` và `Frontend/`.
>
> **Topology runtime:** React gọi REST có JWT tới API; API dùng JPA tới SQL Server; có thư mục `/uploads`; VNPay return URL; Google OAuth; SMTP gửi mail. Môi trường DEV điển hình: Frontend cổng 3000, Vite proxy sang Backend cổng 8080, rồi tới SQL. Quan trọng: **một** API process — không phải mesh microservice.
>
> **Communication patterns** được phép: REST đồng bộ; redirect trình duyệt sang VNPay; job `@Scheduled` cho hold timeout và no-show; Outbox là hướng tới cho email bất đồng bộ; phục vụ static file. Những thứ **không** đưa vào nếu chưa amend Constitution: Kafka, RabbitMQ, GraphQL, BFF riêng.
>
> **Layering nội bộ:** Controller mỏng → Service chứa business rules → Repository → Entity. Frontend: pages, layouts, components, api, hooks, store; server state bằng TanStack Query, client state bằng Zustand, HTTP bằng Axios.
>
> **Booking flow** ảnh hưởng topology: từ `PENDING_DEPOSIT` tới `CONFIRMED`, `CHECKED_IN`, rồi checkout; nhánh `CANCELLED` hoặc `NO_SHOW`. Inventory chống overbooking bằng pessimistic Room lock và kiểm tra overlap trong cùng database.
>
> **Khi demo:** đây là slide kiến trúc chính — mở file hoặc vẽ sơ đồ React ↔ API ↔ SQL Server trên bảng.

---

## B4. `sdd/global/security.md` (khoảng 3–4 phút)

> **security.md** là policy bảo mật nguồn; code thực thi nằm package `com.homestay.security`.
>
> Các điểm nhóm trình bày: production dùng HTTPS; access token kiểu JWT bearer; CORS whitelist, không wildcard trên prod; mật khẩu BCrypt cost từ 12; refresh token có thể revoke; tài khoản SUSPENDED bị từ chối; thông báo lỗi đăng nhập generic để chống enumeration.
>
> Phân quyền là **RBAC cộng property scope** — Manager và Employee không được tin `propertyId` từ client một mình. Secrets chỉ qua env. File có map OWASP Top 10 sang control cụ thể: A01 liên quan scope, A03 chống SQL injection bằng JPA, A08 verify chữ ký VNPay…
>
> Upload tối đa 10MB; thanh toán phải kiểm ownership và hash; mỗi pull request có checklist security.
>
> **Khi demo:** gắn file này với **Điều 3 Constitution** — chứng minh security không chỉ là “có JWT”.

---

## B5. `sdd/global/data-governance.md` (khoảng 2–3 phút)

> File này trả lời dữ liệu thuộc về ai, giữ bao lâu, và PII xử lý thế nào.
>
> Dữ liệu được phân lớp từ Public, Internal, PII, Sensitive, Secrets, tới Financial. Ownership: Customer sở hữu booking của mình; Manager và Employee chỉ trong property được gán; Admin cross-property.
>
> Quy tắc nổi bật: mask PII trên log; retention activity log khoảng **90 ngày**; OTP sống ngắn; dữ liệu tài chính giữ để audit. Các processor bên ngoài — Google, VNPay, SMTP — chỉ share tối thiểu cần thiết.
>
> **Khi demo:** nếu mentor hỏi “PII xử lý thế nào?”, mở mục classification và retention.

---

## B6. `sdd/global/api-standards.md` (khoảng 2–3 phút)

> Chuẩn thiết kế REST cho **một** Spring service: ưu tiên `/api/v1`; có prefix theo role khi cần; resource số nhiều kiểu kebab-case; định danh UUID; JSON camelCase.
>
> Mọi response đi qua envelope `ApiResponse` với các field `success`, `message`, `data`. Phân trang dùng `PageResponse`. Không trả Entity trực tiếp; lỗi tập trung ở `GlobalExceptionHandler`.
>
> **Khi demo:** mở Swagger hoặc một JSON response thật trên Network tab để khớp với chuẩn này.

---

## B7. `sdd/services/homestay/CONTEXT.md` — CONTEXT toàn dự án (khoảng 4–5 phút)

> Đây là file **CONTEXT Layer 2** — bối cảnh **cả hệ thống Homestay**, điền theo template `_templates/CONTEXT.md`, đặt cạnh `module.md`.  
> Khác với `features/*/CONTEXT.md` chỉ mô tả pain từng FR: file này trả lời câu hỏi “**vì sao làm sản phẩm này**” trước khi đào sâu từng feature.
>
> ### 1. PROBLEM STATEMENT — pain, chưa nhảy giải pháp
>
> Chủ homestay và nhân viên đang vận hành thủ công bằng sổ, Excel, Zalo. Hệ quả:
>
> - Overbooking hoặc treo phòng vì không có nguồn sự thật lịch trống.  
> - Cọc 40/60, hủy gần ngày, no-show — chính sách hoàn tiền không nhất quán.  
> - Check-in/out, thu remaining, inspection, housekeeping, bảo trì **đứt đoạn**.  
> - Đa property: Admin, Manager, Employee dễ sai **property scope**.  
> - Khách thiếu self-service đặt phòng, trả cọc, xem hợp đồng.
>
> Pain gói lại thành một câu: **mất doanh thu, mất chỗ, hỗn loạn vận hành, UX khách kém, và rủi ro phân quyền.**
>
> ### 2. DOMAIN KNOWLEDGE
>
> Thuật ngữ bắt buộc: Property, Floor, Room; Booking; inventory half-open cộng RoomStatusBlock; Deposit và Remaining; Contract PDF; Hold 10 phút; No-show; Property scope; mã màn SCR-xx.
>
> Tám status booking khóa cứng — không invent. Actors gồm Guest, Customer, Employee, Manager, Admin. Phạm vi edu đã chốt: VNPay Return URL only; cash remaining tại quầy được giữ; cancel theo tier; damage trên năm triệu cần Admin; xung đột tài liệu thì feature spec thắng.
>
> Catalog feature 001 đến 023; Sprint 2 tập trung **004, 005, 010, 012**.
>
> ### 3. STAKEHOLDERS
>
> Persona chủ property, Customer, Manager-Employee, Admin; team HomestayBooking; mentor có thể reopen tech debt; **AI không được quyết định scope**.
>
> ### 4. CONSTRAINTS
>
> Tech theo Constitution; modular monolith; upload 10MB; Spec-First; TD-010, TD-011, TD-012b deferred; SCR master ở `docs/screen.md`; coverage-critical và PR khoảng tối đa 400 dòng.
>
> ### 5. ASSUMPTIONS
>
> Return URL cộng cash quầy đủ cho edu; timezone vận hành Asia/Ho_Chi_Minh; check-in kiểu date-only; Specification_v2 có thể lệch — luôn ưu tiên feature spec; E2E chưa phải gate cứng…
>
> ### 6. OPEN QUESTIONS
>
> Mentor có reopen TD không? Retention và xóa account? Billing 011 so với Report 016? Playwright có vào DoD không? About CMS Backend? Damage dispute UX?
>
> **Khi demo:** mở `CONTEXT.md` ngay sau architecture — mentor thấy nhóm bắt đầu từ **pain**, không từ “code CRUD”.  
> **Bước tiếp trong quy trình:** feature mới → copy `_templates/CONTEXT.md` → Spec Sync → viết `spec.md`.

---

## B8. `sdd/services/homestay/module.md` (khoảng 2–3 phút)

> **module.md** là Layer 2 — định danh module và catalog feature, bổ sung cho CONTEXT ở trên.
>
> Module `homestay` là **modular monolith**: một API cộng một SPA. Domain auth, booking, payment… chỉ là **package** trong một process — không phải `order-service` hay `payment-service` deploy riêng.
>
> Trách nhiệm chính: Guest discovery dẫn tới Customer booking, payment, contract; Manager và Employee thao tác theo property được gán; Admin quản trị cross-property; tích hợp VNPay, Google OAuth, SMTP và uploads.
>
> Catalog nhóm nhanh:
>
> | Nhóm | ID | Nội dung |
> |------|-----|----------|
> | Nền tảng | 001–003 | Auth, Profile, Discovery |
> | Cốt lõi Sprint 2 | 004, 005, 010, 012 | Booking, Calendar, Contract, Payment |
> | Vận hành | 006–009, 013, 020–023 | Property … Inspection |
> | Tăng trưởng | 011, 014–019 | Billing, Review, Notify, Report, Admin, Promo, Dashboard |
>
> Boundaries: inbound REST, OAuth, VNPay return; outbound SQL, SMTP, VNPay, disk. Cách thêm feature: copy template → điền CONTEXT → spec → Spec Sync → plan/tasks → code; pull request ghi path spec để truy vết.
>
> Header `module.md` đã trỏ sang `CONTEXT.md`.
>
> **Khi demo:** mở cây `features/` trên IDE và chỉ bốn folder Sprint 2.

---

## B9. `sdd/services/homestay/Specification_v2.md` (khoảng 2–3 phút)

> Đây là **SRS narrative** toàn hệ thống. Trước đây từng nằm dưới `docs/`; **canonical hiện tại** là `sdd/services/homestay/Specification_v2.md`.
>
> Nội dung lớn gồm: Context và Goal; Actors và quyền; overview Functional Requirements FR-01 đến FR-23; mô tả entity và acceptance criteria ở mức tổng quan.
>
> Goals điển hình mentor dễ nhận: đặt phòng online; hợp đồng; cọc 40/60; PDF email; lịch trống; cấu trúc Property → Floor → Room; RBAC; housekeeping; báo cáo; admin.
>
> **Cực kỳ quan trọng khi trình bày:** đây là tài liệu **thứ cấp**. Nếu narrative còn đoạn cancel hay payment cũ lệch với clarify ngày 23 tháng 7, team **không code theo Spec_v2** — mà theo feature `spec.md` của 004 và 012.
>
> Dùng Spec_v2 để mentor nắm **bức tranh lớn trong vài phút**; không dùng làm source of truth khi implement.
>
> **Khi demo:** nói rõ path mới dưới `homestay/` và nhắc lại hierarchy conflict một lần.

---

## B10. Cấu trúc một feature folder (lời chung cho 001–023)

> Mỗi feature folder thường chứa:
>
> - **`CONTEXT.md`** — pain, domain, stakeholder **của FR đó**  
> - **`spec.md`** — nguồn sự thật: user stories, functional requirements, success criteria, out of scope  
> - **`plan.md` và `tasks.md`** — thiết kế kỹ thuật và checklist task theo phase  
> - Đôi khi thêm `data-model.md`, `research.md`, `quickstart.md`, và `checklists/requirements.md`
>
> Pipeline chuẩn: Context cộng Spec được Spec Sync approve → Plan → Tasks → Implement → Review.
>
> **Không đọc hết 23 spec khi bảo vệ.** Chỉ đào sâu nhóm Sprint 2 ở mục B11.

---

## B11. Feature trọng tâm Sprint 2

### B11.1 `004-booking-inventory` (3–4 phút)

> Feature **004** là trái tim hệ thống — Booking và Inventory.
>
> Sau session clarify ngày 23 tháng 7, các điểm khóa gồm: khoảng ngày half-open `[checkIn, checkOut)`; chặn đặt khi có `RoomStatusBlock`; damage fee tách khỏi remaining 60%; hủy đúng ngày thứ bảy trước check-in hoàn 100% cọc, từ ba đến dưới bảy ngày 50%, dưới ba ngày 0%; no-show lấy mốc `checkInDate` đầu ngày cộng 24 giờ.
>
> User stories chính: Customer đặt phòng và trả cọc VNPay; xem và theo dõi booking; Manager check-in/check-out kèm thu remaining; Customer hủy theo tier; cộng thêm hold timeout, no-show job, Manager sửa booking Confirmed, và block legacy check-in/out — các khoản này đã Done theo tech-debt.
>
> Màn hình liên quan tiêu biểu: SCR-15 đến 20, 34–35, 37, 69…
>
> **Khi demo:** mở `spec.md` mục Clarifications và User Story 1 — chứng minh Spec đủ chi tiết để code và test.

### B11.2 `005-availability-calendar` (khoảng 1 phút)

> Feature **005** cung cấp lịch trống theo mô hình tám status, align half-open với 004, tôn trọng RoomStatusBlock. Màn chính: SCR-09.
>
> **Khi demo:** một câu — “calendar và booking dùng cùng quy ước ngày, không một bên inclusive một bên exclusive.”

### B11.3 `010-contract-management` (khoảng 1 phút)

> Feature **010**: sau khi cọc confirmed, sinh hợp đồng PDF và gửi email; hướng Outbox cho tác vụ bất đồng bộ. Màn: SCR-21, SCR-38.
>
> **Khi demo:** nhắc ADR trong CLAUDE — contract gắn sau CONFIRMED, không sinh trước khi có cọc.

### B11.4 `012-payment-management` (khoảng 2 phút)

> Feature **012**: VNPay cho DEPOSIT, REMAINING và DAMAGE_FEE, xác nhận qua **Return URL**; cash remaining tại quầy check-in được giữ.
>
> **Không** tự làm trong edu trừ khi mentor reopen: TD-010 đối soát định kỳ và SCR-52; TD-011 chuyển khoản cộng biên lai; TD-012b IPN.
>
> Màn trọng tâm: SCR-20, 26, 36.
>
> **Khi demo:** nói rõ “online = VNPay Return URL; tại quầy = cash remaining; IPN và CK deferred.”

---

## B12. `sdd/sprints/backlog.md` (khoảng 1 phút)

> Backlog chia P0, P1, P2 và gắn với Spec FR. Nếu Status trên backlog lệch thực tế code đã Done, team ưu tiên đối chiếu **tech-debt**, **CLAUDE** và code; cập nhật backlog lại sau Spec Sync — không để backlog thành nguồn lệch thứ hai.

---

## B13. `sdd/sprints/tech-debt.md` (khoảng 2 phút)

> Đây là tracker nợ kỹ thuật. Nhiều khoản Sprint 2 đã **Done**: TD-001 đến TD-007 và các path scripts/coverage liên quan.
>
> **Deferred edu** cần nhớ thuộc lòng: TD-010 reconcile VNPay; TD-011 bank transfer và CK verify — trừ cash desk; TD-012b IPN.
>
> Luật vận hành: nợ phát sinh từ Agent Jam phải log cùng Friday Demo; Critical debt chặn release liên quan; đánh Done phải có PR và đổi Status.
>
> **Khi demo:** mở bảng TD — mentor thấy nhóm biết cắt scope có kiểm soát, không “quên” việc.

---

## B14. `sdd/sprints/pre-commit-checklist.md` (khoảng 1–2 phút)

> Checklist trước khi commit hoặc mở PR: đã đọc spec liên quan; build xanh; lint sạch; không commit secrets; dùng DTO; có validation; không bypass business rules; có unit test và coverage-critical nếu đụng Auth, Booking, Payment; PR khoảng tối đa 400 dòng; và quan trọng nhất — **giải thích được** thay đổi của mình.
>
> Extra cho booking/payment: đúng enum status; không double-book; refund đúng FR-010. Với agent: human phải review plan trước khi cho chạy.
>
> **Khi demo:** “Definition of Done không chỉ nằm AGENTS — còn có playbook pre-commit cụ thể.”

---

# PHẦN C — `docs/` PRODUCT UI & ENGINEERING NOTES

## C1. `docs/README.md` (khoảng 1 phút)

> `docs/README.md` giải thích **convention A**: thư mục `docs/` giữ SCR, design, UAT, setup, testing.  
> **Lưu ý cập nhật:** Specification_v2 **không còn** là canonical dưới `docs/`; xem `sdd/services/homestay/Specification_v2.md`.  
> Bảng sync ngày 23 tháng 7 vẫn đúng hướng: cancel tiers, hold 10 phút, VNPay only, Return URL, SCR-19 / 20 / 37. README cũng trỏ sang `deployment/` và các thư mục testing/prompts nếu có.

---

## C2. `docs/screen.md` (khoảng 3 phút)

> **screen.md** là master catalog khoảng **69 mã SCR**, chia theo role: Guest 01–09, Shared 10–14, Customer 15–26 cộng một số màn mở rộng, Manager 27–44 cộng 66 và 69, Admin 45–58, Employee 59–65.
>
> Điểm nhấn khi demo: SCR-16 và SCR-20 — đặt hàng và VNPay kèm hold 10 phút; SCR-19 — hủy theo tier; SCR-37 — check-in/out và thu remaining; SCR-69 — Manager hủy hoàn 100% cọc; **SCR-52 deferred** theo TD-010; SCR-59 — Employee Dashboard.
>
> Nếu Figma hoặc prompt sinh UI lệch số màn, **luôn theo `screen.md`**.

---

## C3. `docs/screendesign.md` (khoảng 45 giây)

> Ghi chú layout từng màn — Modal, Dedicated page, Drawer… Hỗ trợ thiết kế UI, **không thay** Acceptance Criteria trong Spec.

---

## C4. `docs/entity-ui-mapping.md` (khoảng 1 phút)

> Map từng entity sang field hiển thị trên UI. Mục tiêu: Backend và Frontend không lệch tên field hay thiếu cột quan trọng khi làm form và bảng.

---

## C5. `docs/api-spec-by-screen.md` (khoảng 1 phút)

> Map API theo từng SCR — dùng khi viết Postman hoặc UAT theo màn hình. Nên đối chiếu thêm `SecurityConfig` để biết endpoint nào cần role gì.

---

## C6. `docs/uat-checklist.md` (khoảng 1 phút)

> Checklist UAT thủ công, đã align cancel tier và các edu cuts. Dùng cho Friday Demo và buổi nghiệm thu với mentor.

---

## C7. `docs/environment-setup.md` (khoảng 2 phút)

> Hướng dẫn dựng máy DEV: Java 17, Maven Wrapper, Node 20, SQL Server qua Docker. Workspace canonical: `Backend/` và `Frontend/`. Copy `.env` từ template — **không commit secrets**. Thứ tự: Compose SQL → chạy Backend → Frontend `npm ci` rồi `npm run dev`.

---

## C8. `docs/testing.md` (khoảng 2 phút)

> Chiến lược test: unit, API, E2E, UAT. Critical path hướng ≥80% bằng lệnh `mvn -Pcoverage-critical verify`. Soft floor tổng thể khoảng 15% với `mvn verify`. Test Backend mặc định dùng H2. Frontend bắt buộc `npm run build` trước khi coi là xanh. Chi tiết CI xem `deployment/ci-cd.md`.

---

## C9. `docs/DESIGN.md` + `component-library.md` (khoảng 1 phút)

> Design system và thư viện component — tokens typography/color/spacing, atoms như Button Input Badge, organisms như RoomCard, DataTable, AvailabilityCalendar. Code UI kit nằm `Frontend/src/components/ui`.

---

## C10. `docs/figma-generation-prompt.md` (khoảng 30 giây)

> Prompt dài để sinh UI trên Figma theo entity-driven rules. Số SCR trong prompt có thể legacy — luôn đối chiếu lại `screen.md`.

---

## C11. `docs/presentation-script-vi.md` (file này)

> Đây là **lời trình bày** — hỗ trợ nói khi bảo vệ. File **không** thay Spec. Mỗi lần Spec Sync đổi ADR, Deferred scope, hoặc chuyển path tài liệu, cần cập nhật script này.

---

# PHẦN D — DEPLOYMENT & CI

## D1. `deployment/README.md` (khoảng 30 giây)

> Index của thư mục deployment: trỏ tới environments, docker, ci-cd. Setup máy DEV chuẩn vẫn ưu tiên `docs/environment-setup.md`. Compose SQL nằm `Backend/compose.yml`.

---

## D2–D3. `environments.md` + `docker.md` (khoảng 1–2 phút)

> **environments.md** mô tả ma trận Local và CI, phiên bản chuẩn, biến môi trường Backend.  
> **docker.md** hướng dẫn SQL Server Docker thống nhất cho DEV: start, stop, healthcheck, troubleshooting — API và Frontend **chưa** bắt buộc dockerize trong phạm vi edu.

---

## D4. `deployment/ci-cd.md` + workflows (khoảng 2 phút)

> CI: Backend và Frontend có remote/workflow riêng, cộng workflow `tests.yml` ở root khi dùng. Trigger trên nhánh `main`, `DEV`, `dev` và pull request.
>
> Backend: Java 17, chạy coverage-critical kèm JaCoCo. Frontend: Node 20, `npm ci`, build. E2E Playwright hiện disabled đến khi team sẵn sàng.
>
> Critical set tiêu biểu: AuthService, BookingService, PaymentService, VNPayService, DamageFeeSettlementService.
>
> CI **hiện không** deploy cloud, không dựng Docker API đầy đủ, không SQL Server thật trên runner, không integration test VNPay sandbox. Trước PR, local parity: `mvnw test` và Frontend build.
>
> **Khi demo:** mở badge CI hoặc log workflow — chứng minh chất lượng có gate tự động, không chỉ “chạy được trên máy em”.

---

# PHẦN E — WORKSPACE CODE

## E1. `Backend/` (khoảng 1 phút)

> Entry point `com.homestay.HomestayApplication`. Package theo lớp: controller, service, repository, entity, dto, exception, job, security. Có job hold timeout và no-show. SecurityConfig cấu hình JWT. Coverage critical gắn JaCoCo profile.
>
> **Khi demo:** nếu còn thời gian, chạy một API booking hoặc mở class `BookingService`.

## E2. `Frontend/` (khoảng 1 phút)

> Vite + React + TypeScript. Pages chia theo role; gọi API qua `src/api/*Api.ts`; TanStack Query cho server state, Zustand cho client state; Vite proxy về cổng 8080. Lệnh ngày thường: `npm run dev`, `build`, `lint`.
>
> **Khi demo:** mở một page Customer booking hoặc Manager check-in khớp SCR đã nêu.

---

# PHẦN F — KẾT LUẬN (1 phút)

> Em xin tóm tắt lại hệ thống tài liệu:
>
> 1. **Root** — luật team và AI memory: Constitution, AGENTS, CLAUDE, CONTRIBUTING.  
> 2. **`sdd/global`** — architecture và policy kỹ thuật.  
> 3. **`homestay/CONTEXT.md`** — pain và domain **toàn dự án**; **`module.md`** catalog 23 feature; **`Specification_v2.md`** SRS narrative thứ cấp.  
> 4. **`features/*/spec.md`** — nguồn sự thật từng FR; Sprint 2 trọng tâm 004, 005, 010, 012.  
> 5. **`docs/`** — SCR, UAT, setup; **`deployment/` và CI** — lặp lại được chất lượng.
>
> Hierarchy **feature spec > AGENTS > CLAUDE > Spec_v2** giúp team tránh tranh cãi “doc nào đúng” và tránh agent tự mở scope Deferred.
>
> Em xin hết phần trình bày tài liệu. Nhóm sẵn sàng demo luồng booking hoặc trả lời câu hỏi của thầy/cô.

---

# PHỤ LỤC

## A. Phiên bản rút gọn ~3 phút

> Homestay Booking vận hành SDD cộng ADD.  
> Root có README, CONTRIBUTING bốn pha, CONSTITUTION bảy điều, AGENTS khóa tám status booking, CLAUDE ghi ADR và lesson.  
> Trong `sdd/`: global policy → CONTEXT toàn dự án cộng module cộng Spec_v2 narrative → 23 feature; Sprint 2 làm 004, 005, 010, 012; thanh toán VNPay Return URL; cash remaining tại quầy; IPN, CK và reconcile deferred.  
> Trong `docs/`: khoảng 69 SCR, UAT và setup. Deploy và CI có coverage-critical.  
> Khi xung đột: **feature spec trước Specification_v2**.

## B. Gợi ý chia người trình bày

| Thành viên | Phần | Thời lượng gợi ý |
|------------|------|------------------|
| A | Mở đầu + README + CONTRIBUTING + CONSTITUTION | ~8–10 phút |
| B | AGENTS + CLAUDE | ~6–7 phút |
| C | sdd/README + global (architecture, security, data, api) | ~10–12 phút |
| D | CONTEXT toàn dự án + module + Spec_v2 + feature 004/005/010/012 + sprints | ~12–15 phút |
| E | docs (screen, setup, testing) + deployment/CI + kết luận | ~8–10 phút |

## C. FAQ mentor — một câu

| Câu hỏi | Trả lời ngắn |
|---------|----------------|
| Source of truth? | Feature `spec.md` trong `sdd/.../features/` |
| CONTEXT toàn dự án ở đâu? | `sdd/services/homestay/CONTEXT.md` |
| Spec_v2 ở đâu? | `sdd/services/homestay/Specification_v2.md` (secondary) |
| Sao không microservice? | Modular monolith — Constitution + `system-architecture.md` |
| Thanh toán? | VNPay Return URL; cash remaining quầy; IPN/CK deferred |
| Cancel hoàn? | ≥7d 100% / 3–7d 50% / &lt;3d 0% cọc; Manager 100% cọc |
| Hold? | 10 phút mặc định |
| AI được phép? | Bám AGENTS + Spec; không invent status; human review |
| Overbooking chống thế nào? | Pessimistic Room lock + overlap half-open + RoomStatusBlock |
| Damage > 5 triệu? | Cần Admin co-approval |

## D. Gợi ý mở IDE khi nói (cheat sheet)

| Đang nói mục | Mở file / folder |
|--------------|------------------|
| A1 | `README.md` |
| A3 | `CONSTITUTION.md` → Article 1 |
| A4 | `AGENTS.md` → Forbidden Patterns / statuses |
| B3 | `sdd/global/system-architecture.md` |
| B7 | `sdd/services/homestay/CONTEXT.md` §1 Problem |
| B8 | cây `sdd/services/homestay/features/` |
| B11.1 | `features/004-booking-inventory/spec.md` Clarifications |
| B13 | `sdd/sprints/tech-debt.md` |
| C2 | `docs/screen.md` SCR-19 / 20 / 37 |
| D4 | `.github/workflows/tests.yml` |

## E. Changelog script

| Ngày | Ver | Thay đổi |
|------|-----|----------|
| 24/07/2026 | 1.0 | Bản đầu — lời trình bày toàn bộ docs |
| 24/07/2026 | 1.1 | Thêm B7 CONTEXT module; Spec_v2 → `sdd/services/homestay/`; cập nhật hierarchy, mở đầu, kết luận, FAQ, chia người |
| 24/07/2026 | 1.2 | Viết lại lời nói đầy đủ (câu đọc được); bổ sung cheat sheet IDE; FAQ overbooking/damage; thời lượng chia người |

---

*File này bổ sung lời nói; không thay thế Spec. Cập nhật khi Spec Sync đổi ADR / Deferred scope / chuyển path tài liệu.*
