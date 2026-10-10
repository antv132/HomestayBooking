# SWP391 — Project AI Usage Report

## Project Information

| Field | Value |
|---|---|
| Subject Code | SWP391 |
| Subject Name | Software Project |
| Class Code | _(điền mã lớp)_ |
| Semester | _(điền kỳ học, vd. Summer 2026)_ |
| Lecturer Name | _(điền tên GV)_ |
| Group Code | G6 |
| Project Title | Homestay Booking System |

## List of Students

| No | StudentCode | StudentName | Role In Group | AI Tool Usaged |
|---|---|---|---|---|
| 1 | | | | Cursor, ChatGPT |
| 2 | | | | Cursor, ChatGPT |
| 3 | | | | Cursor, ChatGPT |
| 4 | | | | Cursor, ChatGPT |
| 5 | | | | Cursor, ChatGPT |

> Điền MSSV, họ tên, role (Leader / Backend / Frontend / Tester / BA…).  
> Evidence screenshots: Google Drive folder chung, đặt tên `Group6_Wn_Sx_Activity.png`.

---

## Column Guide

| Column | Description & How to Fill |
|---|---|
| No. | Sequential number of the log entry (1, 2, 3…). |
| SDLC Phase | Which phase of the project this AI usage belongs to (e.g., Requirement, Design, Implementation, Testing, Reporting). |
| Task / Activity | Specific task you used AI for (e.g., User Story Development, ERD Design, Workflow Coding, Test Case Generation). |
| AI Tool Used | Name of the AI tool (e.g., ChatGPT, Cursor, GitHub Copilot). |
| AI Output | The actual output AI produced (summary only). |
| Student’s Validation / Modification | How you validated or modified the AI output. |
| Evidence / Link | Link to shared Google Drive folder; naming `GroupX_SessionY_Activity`. Screenshot must show prompt, AI response, and student follow-up. |
| Quantitative Measure | Numerical evidence (user stories, entities, classes/methods, LOC, test cases…). |
| Value Added (1-5) | Self-assessment of AI’s usefulness (1 = not useful, 5 = very useful). |
| Risks / Limitations Observed | Limitations or risks observed when using AI. |

---

## Week 1 — Requirement / Kickoff

| No. | SDLC Phase | Task / Activity | AI Tool Used | AI Output | Student’s Validation / Modification | Evidence / Link | Quantitative Measure | Value Added (1-5) | Risks / Limitations Observed |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Requirement | Phân tích đề bài SWP391, xác định scope Homestay Booking | ChatGPT | Danh sách module đề xuất (auth, booking, payment, CMS…) | Giữ 6 module core; loại bank transfer/IPN (defer); ghi ADR JWT + React | `Group6_W1_S1_Scope.png` | 6 modules in-scope | 4 | AI đề xuất quá rộng so với Sprint |
| 2 | Requirement | Draft user stories theo persona Customer/Manager/Admin | ChatGPT | ~18 user stories + priority | Giữ 10 stories; bỏ 8 trùng/ngoài scope; chỉnh wording VN/EN | `Group6_W1_S2_UserStory.png` | 10 user stories | 4 | Story generic, thiếu deposit 40/60 |
| 3 | Requirement | Xác định non-functional (security, performance, upload) | ChatGPT | NFR list (JWT, bcrypt, 10MB upload…) | Align constitution 10MB; bỏ NFR không đo được; thêm timezone Asia/HCM | `Group6_W1_S3_NFR.png` | 8 NFR kept | 3 | NFR mơ hồ, thiếu metric |
| 4 | Reporting | Setup repo structure Backend/Frontend + README outline | Cursor | README + folder tree gợi ý | Sửa tên package `com.homestay`; bỏ Prisma/TypeORM | `Group6_W1_S4_Repo.png` | 2 workspaces, 1 README | 4 | AI lẫn stack sai (Node ORM) |

---

## Week 2 — Requirement (Use case / Spec)

| No. | SDLC Phase | Task / Activity | AI Tool Used | AI Output | Student’s Validation / Modification | Evidence / Link | Quantitative Measure | Value Added (1-5) | Risks / Limitations Observed |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Requirement | Use case booking inventory (create, hold, confirm) | ChatGPT | UC diagram + main/alternate flows | Map UC theo spec 004; thêm hold timeout 10’; bỏ flow thừa | `Group6_W2_S1_UC_Booking.png` | 5 use cases | 4 | Thiếu room lock / overlap |
| 2 | Requirement | Acceptance criteria cancel + no-show | ChatGPT | Draft AC cancel refund + no-show 24h | Align tier ≥7d/3–7d/<3d; Manager 100%; sửa AC sai | `Group6_W2_S2_AC_Cancel.png` | 12 AC | 5 | AI dùng flat 50% refund |
| 3 | Requirement | Screen list / SCR mapping (entity-ui) | ChatGPT / Cursor | Danh sách ~40 màn hình | Giữ SCR chính; loại SCR ngoài sprint; map role | `Group6_W2_S3_ScreenList.png` | ~25 screens mapped | 3 | Trùng tên màn; thiếu employee dashboard |
| 4 | Requirement | Clarify payment: VNPay vs cash desk | ChatGPT | So sánh cổng thanh toán | Chọn VNPay Return URL only; giữ cash remaining; defer IPN | `Group6_W2_S4_PaymentScope.png` | 3 payment decisions (ADR) | 4 | AI đẩy IPN/reconcile không cần |
| 5 | Design | Draft domain glossary (status enum, deposit…) | Cursor | Glossary 20+ terms | Chuẩn hóa 8 booking status; bỏ synonym gây confuse | `Group6_W2_S5_Glossary.png` | 8 statuses documented | 5 | AI invent status ngoài enum |

---

## Week 3 — Design (Data / ERD)

| No. | SDLC Phase | Task / Activity | AI Tool Used | AI Output | Student’s Validation / Modification | Evidence / Link | Quantitative Measure | Value Added (1-5) | Risks / Limitations Observed |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Design | ERD core: User, Property, Room, Booking | ChatGPT | ERD ~12 entities | Giữ 9 entity; sửa FK; snake_case; bỏ bảng thừa | `Group6_W3_S1_ERD_Core.png` | 9 entities | 4 | Cardinality sai |
| 2 | Design | ERD Payment + Contract + Outbox | ChatGPT | Thêm Payment, Contract, OutboxEvent | Giữ Outbox email; bỏ transfer verify (defer); sửa amount fields | `Group6_W3_S2_ERD_Payment.png` | +3 entities | 4 | Thiếu index gợi ý |
| 3 | Design | Index / query plan cho booking list | Cursor | Gợi ý index property_id, status, dates | Giữ index theo LESSON-008; bỏ index thừa | `Group6_W3_S3_Indexes.png` | 6 indexes | 3 | Over-index; thiếu composite |
| 4 | Design | RoomStatusBlock date-scoped design | Cursor | Model block theo ngày | Align half-open; sửa overlap với booking | `Group6_W3_S4_RoomBlock.png` | 1 entity design | 4 | AI treat block như soft-delete room |
| 5 | Reporting | Xuất ERD mô tả cho tài liệu | ChatGPT | Mô tả bảng tiếng Việt | Rút gọn; đúng tên cột thật trong code | `Group6_W3_S5_ERD_Doc.png` | 1 ERD doc section | 3 | Mô tả lệch schema code |

---

## Week 4 — Design (API / Workflow)

| No. | SDLC Phase | Task / Activity | AI Tool Used | AI Output | Student’s Validation / Modification | Evidence / Link | Quantitative Measure | Value Added (1-5) | Risks / Limitations Observed |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Design | Booking status state machine | Cursor | Diagram 6–8 status | Align 8 status chuẩn; thêm NO_SHOW, PENDING_DAMAGE_PAYMENT | `Group6_W4_S1_StateMachine.png` | 8 statuses, 12 transitions | 5 | Thiếu nhánh damage fee |
| 2 | Design | REST API outline `/api/v1/manager\|customer` | Cursor | ~25 endpoint stubs | Version `/api/v1`; tách manager; block legacy CI/CO | `Group6_W4_S2_API_Outline.png` | ~20 endpoints | 4 | Path không nhất quán |
| 3 | Design | Sequence: create booking → deposit VNPay | ChatGPT | Sequence diagram PlantUML | Sửa return URL; bỏ IPN; thêm holdExpiresAt | `Group6_W4_S3_Seq_Deposit.png` | 1 sequence diagram | 5 | Nhầm confirm qua IPN |
| 4 | Design | Sequence: check-in (CMND + remaining cash) | Cursor | Sequence check-in desk | Giữ remainingCollected CASH; yêu cầu CMND; sửa preconditions | `Group6_W4_S4_Seq_CheckIn.png` | 1 sequence diagram | 4 | Cho check-in trước ngày |
| 5 | Design | Error model (Business/Conflict/NotFound) | Cursor | Exception hierarchy draft | Map GlobalExceptionHandler; HTTP status chuẩn | `Group6_W4_S5_ErrorModel.png` | 4 exception types | 4 | Status code không đồng nhất |

---

## Week 5 — Implementation (Auth / Profile)

| No. | SDLC Phase | Task / Activity | AI Tool Used | AI Output | Student’s Validation / Modification | Evidence / Link | Quantitative Measure | Value Added (1-5) | Risks / Limitations Observed |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Implementation | JWT access + refresh authentication | Cursor | SecurityFilterChain, JwtService, AuthController | Refresh TTL 7d; không hardcode secret; validate claims | `Group6_W5_S1_JWT.png` | ~4 classes, ~250 LOC | 4 | Pattern lộ secret trong sample yml |
| 2 | Implementation | Role-based access (Customer/Manager/Admin/Employee) | Cursor / Copilot | `@PreAuthorize` + route guards FE | Sửa role name; chặn SUSPENDED/INACTIVE ở refresh | `Group6_W5_S2_RBAC.png` | 4 roles, ~10 secured endpoints | 4 | Guard FE thiếu edge case |
| 3 | Implementation | Google OAuth login (FR-006) | Cursor | OAuth2 login flow draft | Align OTP/Google rule; sửa redirect; test fail path | `Group6_W5_S3_GoogleOAuth.png` | 1 OAuth flow, ~150 LOC | 3 | Config mismatch local/prod |
| 4 | Implementation | Forgot-password anti-enumeration | Cursor | Reset password API | Response thống nhất (không lộ email tồn tại); TTL OTP | `Group6_W5_S4_ForgotPassword.png` | 2 endpoints | 5 | AI trả message khác nhau theo email |
| 5 | Implementation | FE Login/Register pages + auth store | Cursor | React pages + Zustand auth | Bỏ `any`; gắn TanStack Query; sửa redirect theo role | `Group6_W5_S5_FE_Auth.png` | 3 pages, 1 store | 4 | Double export / white screen risk |

---

## Week 6 — Implementation (Booking core)

| No. | SDLC Phase | Task / Activity | AI Tool Used | AI Output | Student’s Validation / Modification | Evidence / Link | Quantitative Measure | Value Added (1-5) | Risks / Limitations Observed |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Implementation | Create booking + PENDING_DEPOSIT hold | Cursor | BookingService.create + DTO | Hold 10’; holdExpiresAt; validate date half-open | `Group6_W6_S1_CreateBooking.png` | 1 service method, ~200 LOC | 5 | Timezone / inclusive date sai |
| 2 | Implementation | Pessimistic room lock + overlap check | Cursor | Lock query + conflict handling | Giữ room lock; ConflictException; bỏ race path | `Group6_W6_S2_RoomLock.png` | 1 lock path, 3 conflict cases | 5 | Deadlock nếu lock order sai |
| 3 | Implementation | BookingHoldTimeoutJob | Cursor | `@Scheduled` cancel unpaid hold | Align config `BOOKING_HOLD_TIMEOUT_MINUTES`; release inventory | `Group6_W6_S3_HoldJob.png` | 1 job class | 4 | Job chạy sai TZ |
| 4 | Implementation | Availability calendar days[] (FE + BE) | Cursor | Calendar API + FE grid | Half-open; 8-status color; RoomStatusBlock scoped | `Group6_W6_S4_Calendar.png` | 1 API + 1 FE page, ~300 LOC | 4 | N+1 query; status color lệch |
| 5 | Implementation | FE booking form + hold countdown UI | Cursor | React booking wizard | Countdown holdExpiresAt; disable submit khi hết hạn | `Group6_W6_S5_FE_HoldUI.png` | 1 wizard, countdown component | 4 | Timer drift client/server |

---

## Week 7 — Implementation (Payment)

| No. | SDLC Phase | Task / Activity | AI Tool Used | AI Output | Student’s Validation / Modification | Evidence / Link | Quantitative Measure | Value Added (1-5) | Risks / Limitations Observed |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Implementation | VNPay create payment URL (DEPOSIT) | Cursor | VNPay helper + controller | Hash/sign đúng; amount VND; bỏ log secret | `Group6_W7_S1_VNPayCreate.png` | 1 integration, ~180 LOC | 4 | Sample key trong code |
| 2 | Implementation | VNPay Return URL confirm (DEPOSIT/REMAINING/DAMAGE) | Cursor | Return handler multi-type | Chỉ Return URL; map 3 loại; idempotent confirm | `Group6_W7_S2_VNPayReturn.png` | 3 payment types handled | 5 | AI thêm IPN không cần |
| 3 | Implementation | Cash remaining at check-in desk | Cursor | `remainingCollected` → Payment CASH PAID | Giữ ADR-007; không mở lại CK; validate status | `Group6_W7_S3_CashRemaining.png` | 1 cash path | 5 | Nhầm với bank transfer |
| 4 | Implementation | Damage fee VNPay + unlock checkout | Cursor | PENDING_DAMAGE_PAYMENT flow | Fee >5tr cần Admin co-approve; unlock sau PAID | `Group6_W7_S4_DamageFee.png` | 1 flow, ~150 LOC | 4 | Thiếu co-approval gate |
| 5 | Implementation | FE payment result + retry UX | Cursor | Result page success/fail | Clear message; không double-charge; query status | `Group6_W7_S5_FE_Payment.png` | 1–2 pages | 3 | Polling thừa; race refresh |

---

## Week 8 — Implementation (Cancel / No-show / CI-CO)

| No. | SDLC Phase | Task / Activity | AI Tool Used | AI Output | Student’s Validation / Modification | Evidence / Link | Quantitative Measure | Value Added (1-5) | Risks / Limitations Observed |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Implementation | Customer cancel deposit tiers (FR-010) | Cursor | Cancel service + refund % | ≥7d 100% / 3–7d 50% / <3d 0%; bỏ flat 50% | `Group6_W8_S1_CancelTiers.png` | 1 method, 3 tiers | 5 | Business rule sai |
| 2 | Implementation | Manager cancel (100% refund policy) | Cursor | Manager cancel endpoint | Manager 100%; audit reason; scope property | `Group6_W8_S2_ManagerCancel.png` | 1 endpoint | 4 | Thiếu authorization check |
| 3 | Implementation | BookingNoShowJob (FR-008) | Cursor | Job CONFIRMED → NO_SHOW sau 24h | Giữ cọc; release inventory; TZ Asia/HCM | `Group6_W8_S3_NoShowJob.png` | 1 job, 3 tests | 5 | Mốc 24h tính sai |
| 4 | Implementation | Manager check-in (CMND + remaining) | Cursor | Check-in API v1 | Block legacy; yêu cầu CMND; ngày ≥ check-in date | `Group6_W8_S4_CheckIn.png` | 1 API flow | 4 | Cho CI sớm |
| 5 | Implementation | Check-out + inspection gate | Cursor | Checkout + inspection passed rule | Bắt buộc inspection; damage → PENDING_DAMAGE_PAYMENT | `Group6_W8_S5_CheckOut.png` | 1 API flow | 4 | Bỏ qua inspection |
| 6 | Implementation | FE Manager booking ops screens | Cursor | CI/CO/cancel UI | Disable action theo status; error toast rõ | `Group6_W8_S6_FE_ManagerOps.png` | 2–3 pages | 3 | Button state lệch BE |

---

## Week 9 — Testing / QA

| No. | SDLC Phase | Task / Activity | AI Tool Used | AI Output | Student’s Validation / Modification | Evidence / Link | Quantitative Measure | Value Added (1-5) | Risks / Limitations Observed |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Testing | JUnit BookingService (create/hold/cancel) | Cursor | Draft unit tests | Sửa mock; thêm edge date; bỏ test không compile | `Group6_W9_S1_JUnit_Booking.png` | 12 unit tests | 3 | Assert yếu; thiếu concurrency |
| 2 | Testing | JUnit payment return + damage unlock | Cursor | Payment service tests | Idempotent case; fail checksum; map status | `Group6_W9_S2_JUnit_Payment.png` | 8 unit tests | 4 | Không cover chữ ký sai đủ |
| 3 | Testing | Manual test case list (Postman/checklist) | ChatGPT | ~30 manual cases | Giữ 20; bổ sung negative; map SCR/Test Report | `Group6_W9_S3_ManualTC.png` | 20 manual cases | 3 | Case trùng; thiếu expected |
| 4 | Testing | Regression: no-show + hold timeout | Cursor | Test plan + sample data | Chạy job manual; verify inventory release | `Group6_W9_S4_JobRegression.png` | 6 regression cases | 4 | Khó reproduce TZ |
| 5 | Testing | FE smoke: login → book → pay result | ChatGPT / Cursor | Smoke checklist | Rút 10 bước critical path; ghi bug UI | `Group6_W9_S5_FE_Smoke.png` | 10 smoke steps | 3 | Bỏ role Manager/Employee |
| 6 | Testing | Coverage critical path note | Cursor | Gợi ý package coverage | Align `mvn -Pcoverage-critical`; không claim ảo | `Group6_W9_S6_Coverage.png` | critical set documented | 3 | AI phóng đại % coverage |

---

## Week 10 — Reporting / Demo polish

| No. | SDLC Phase | Task / Activity | AI Tool Used | AI Output | Student’s Validation / Modification | Evidence / Link | Quantitative Measure | Value Added (1-5) | Risks / Limitations Observed |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Reporting | Outline báo cáo đồ án + mục lục | ChatGPT | TOC + section draft | Cắt generic; gắn Homestay G6 + spec refs | `Group6_W10_S1_ReportOutline.png` | 1 outline, ~10 sections | 4 | Nội dung chung chung |
| 2 | Reporting | Slide demo flow (book → pay → CI/CO) | ChatGPT | ~15 slide outline | Giữ 12; demo script thật; bỏ slide filler | `Group6_W10_S2_Slides.png` | 12 slides | 4 | Quá nhiều chữ trên slide |
| 3 | Reporting | Presentation script tiếng Việt | ChatGPT | Script dài ~20’ | Rút theo thời lượng; chia phần thành viên | `Group6_W10_S3_Script.png` | 1 script | 4 | Thuật ngữ lệch spec |
| 4 | Reporting | Booking workflow diagram cho tài liệu | Cursor | PlantUML/HTML workflow | Align 8 status; export diagram đã review | `Group6_W10_S4_WorkflowDoc.png` | 1 diagram updated | 5 | Nhánh thiếu nếu copy cũ |
| 5 | Reporting | AI Usage Report + evidence naming | ChatGPT / Cursor | Template bảng + naming convention | Điền 10 tuần; chuẩn `Group6_Wn_Sx_...`; link Drive | `Group6_W10_S5_AIUsage.png` | 10 week logs, 40+ evidences | 5 | Claim AI làm phần chưa có screenshot |
| 6 | Reporting | Tech debt / deferred items note (TD-010..012) | Cursor | List deferred VNPay IPN/reconcile | Ghi rõ deferred edu; không pretend đã làm | `Group6_W10_S6_TechDebt.png` | 3 deferred items documented | 4 | AI dễ “complete” nhầm debt |

---

## Evidence Notes

- Folder Drive: _(dán link Google Drive chung của nhóm)_
- Naming: `Group6_W{week}_S{session}_{Activity}.png` (hoặc `.mp4`)
- Mỗi evidence phải thấy rõ: **prompt → AI response → student follow-up**
- Tổng: **10 tuần · ~51 dòng log**

## Summary (optional)

| Week | Phase focus | # Log entries |
|---|---|---|
| 1 | Requirement / Kickoff | 4 |
| 2 | Requirement (UC / Spec) | 5 |
| 3 | Design (ERD) | 5 |
| 4 | Design (API / Workflow) | 5 |
| 5 | Implementation (Auth) | 5 |
| 6 | Implementation (Booking) | 5 |
| 7 | Implementation (Payment) | 5 |
| 8 | Implementation (Cancel / CI-CO) | 6 |
| 9 | Testing / QA | 6 |
| 10 | Reporting / Demo | 6 |
| **Total** | | **52** |
