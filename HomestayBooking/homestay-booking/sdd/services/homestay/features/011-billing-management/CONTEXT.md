# CONTEXT.md — FR-11 Billing Management
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/011-billing-management/spec.md`

## 1. PROBLEM STATEMENT

Không có hóa đơn cọc/phần còn lại rõ ràng → khách không biết nợ gì; Manager không biết ai chưa trả trước check-in.

Pain cụ thể:
- Booking tạo xong nhưng không có cặp invoice 40%/60% theo snapshot → nghĩa vụ thanh toán mơ hồ.
- Invoice trùng khi retry tạo booking → lệch sổ.
- Giá phòng đổi sau làm “đè” amount đã phát hành (trừ remaining khi modify Confirmed — cần rule rõ).
- Booking Cancelled vẫn để Unpaid “treo” trên UI.
- Customer/Manager thiếu breakdown trên booking detail / payment history theo đúng scope.

Pain: phát hành & theo dõi Invoice; **không** thực thi cổng VNPay/biên lai (FR-12).

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa |
|-----------|--------|
| Invoice Deposit | 40% TotalAmount snapshot lúc booking Pending Deposit |
| Invoice Remaining | 60% RemainingAmount snapshot; có thể adjust khi modify Confirmed |
| Status | Unpaid → Pending Payment → Paid \| Cancelled; Overdue display optional |
| Idempotent pair | Đúng 1 deposit + 1 remaining / booking |
| SCR-18 / 26 | Customer booking breakdown / payment history |
| SCR-35 / 36 | Manager breakdown / pending billing visibility |

Ranh giới: Payment records & verify = FR-12; Contract PDF = FR-10; Damage invoice = out of FR-11 v1.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Customer | Xem breakdown & lịch sử billing của mình |
| Manager | Theo dõi Unpaid/Pending trong property scope |
| System | Issue invoices on booking create/cancel/modify |
| Spec Architect | Chốt adjust remaining vs immutable deposit |
| Agent Pilot | Invoice entity + sync events từ FR-04/12 |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- Amount từ snapshot booking FR-04 (40/60) — không tính lại từ PricePerNight live
- FR-11 không gọi VNPay / upload receipt

**Business**
- Paid invoice giữ Paid khi cancel; refund thuộc FR-12
- Scope Customer own / Manager assigned property
- Partial payment không hỗ trợ v1

**Time**
- Sprint 2 cùng payment/booking; damage billing sau

## 5. ASSUMPTIONS

1. DueDate remaining ≈ check-in date; Overdue badge không auto-cancel booking v1.
2. Deposit Paid amount không đổi khi modify booking.
3. Pending Payment khi FR-12 bắt đầu pay; Paid khi confirm.
4. Admin read-only global; không UI invoice Admin riêng v1.
5. ActivityLog: ISSUED / PAID / CANCELLED / ADJUSTED.

## 6. OPEN QUESTIONS

1. Modify tăng giá: update in-place remaining invoice hay invoice adjustment mới?
2. Modify giảm giá khi remaining chưa Paid: có cho âm / hoàn không?
3. Entity tên `Invoice` đã có trong DB hay map từ Payment lines hiện tại?
4. Overdue có job nhắc (FR-15) trong Sprint 2 không?
5. CHECKIN_DENIED_UNPAID enforce ở FR-04 hay đọc status invoice FR-11?
