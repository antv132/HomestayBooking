# CONTEXT.md — FR-10 Contract Management
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/010-contract-management/spec.md`

## 1. PROBLEM STATEMENT

Sau khi cọc thành công, khách/Manager không có hợp đồng PDF bất biến → thiếu bằng chứng pháp lý và phải gửi tay.

Pain cụ thể:
- Deposit confirmed nhưng không ai nhận PDF / email hợp đồng đúng lúc.
- Tạo trùng contract khi retry/IPN replay → rối lưu trữ.
- Sửa PDF gốc khi đổi giá phòng sau → mất tính snapshot.
- Customer không tự xem/tải/in; Manager không resend khi email fail.
- Damage fee nếu sửa vào hợp đồng gốc → phá immutability (cần Addendum riêng).

Pain: phát hành & phân phối contract sau cọc; không phải cổng thanh toán (FR-12) hay invoice (FR-11).

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa |
|-----------|--------|
| Accommodation Contract | PDF snapshot booking/room/customer/amounts/dates |
| Outbox | `CONTRACT_GENERATE` / `EMAIL_SEND` / `RESEND` — idempotent |
| Immutable snapshot | Không regenerate PDF gốc khi data phòng đổi |
| Active / Completed / Cancelled | Lifecycle contract theo booking |
| Addendum | Phụ lục Damage Fee — P2; không sửa PDF gốc |
| SCR-21 / SCR-38 | Customer My Contracts / Manager Contract Mgmt |

Trigger: deposit success (VNPay hoặc Manager duyệt CK) → booking Confirmed (FR-04).

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Customer | Xem, tải, in hợp đồng của mình |
| Manager | Xem scope property; resend email |
| System / Outbox worker | Generate + gửi |
| Admin | Read global optional; UI riêng không bắt buộc v1 |
| Agent Pilot | PDF + Outbox + list APIs |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- Outbox pattern (ADR-005); secrets email/SMTP qua env
- Một contract gốc / booking (unique bookingId)

**Business**
- Resend không regenerate PDF gốc
- Scope: Customer = own; Manager = assigned property
- Employee không quyền Contract

**Time**
- Sprint 2 active spec; Addendum có thể stub nếu FR-23 chưa sẵn

## 5. ASSUMPTIONS

1. Generate fail → retry Outbox; chưa Active đến khi PDF sẵn sàng.
2. Booking Cancelled → contract Cancelled; PDF gốc giữ read-only.
3. Checked-out → contract Completed (v1).
4. Admin dùng báo cáo/future screen; không bắt UI FR-10 v1.
5. Damage Addendum P2 khi damage approval sẵn sàng.

## 6. OPEN QUESTIONS

1. PDF engine chọn cái gì (OpenPDF/iText/HTML→PDF) đã chốt chưa?
2. Email: attachment PDF hay link signed URL?
3. Lưu file: local `/uploads` vs object storage?
4. Addendum có ship Sprint 2 hay chỉ interface stub?
5. ActivityLog event names đã đủ cho audit compliance chưa?
