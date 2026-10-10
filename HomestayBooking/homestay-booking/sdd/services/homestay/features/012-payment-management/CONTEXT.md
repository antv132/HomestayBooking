# CONTEXT.md — FR-12 Payment Management (VNPay-first)
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/012-payment-management/spec.md`

## 1. PROBLEM STATEMENT

Khách trả cọc/remaining nhưng hệ thống không xác nhận VNPay → mất Confirmed, mất contract, mất niềm tin.

Pain cụ thể (edu in-scope):
- Không thanh toán được cọc 40% qua **VNPay** → booking kẹt Pending Deposit.
- **Return URL** không verify / không idempotent → Confirm sai hoặc mất Confirm.
- Remaining 60% chưa Paid vẫn cố check-in → phá rule CHECKIN_DENIED_UNPAID.
- Customer pay booking người khác / Manager xem ngoài property scope.

**Edu cuts:**
- TD-010 — cron đối soát + SCR-52
- TD-011 — chuyển khoản + biên lai + Manager verify CK
- TD-012 — VNPay **IPN** (sandbox/edu = Return URL only)

**Kept exception:** thu remaining **tiền mặt tại quầy** khi check-in (SCR-37) — không thuộc cut TD-011.

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa |
|-----------|--------|
| Payment | Giao dịch; **method = VNPay** (edu) |
| Pending → Paid / Failed | Redirect + **Return URL** (IPN deferred TD-012) |
| orderRef | Khóa idempotent với cổng VNPay |
| SCR-16/26/36 | Pay + history (không SCR-20/37/52) |

Side-effects deposit Paid: booking Confirmed (FR-04); invoice/contract khi module sẵn.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Customer | Pay deposit/remaining qua VNPay; xem history |
| Manager | Xem payment history scoped — **không** Approve CK |
| Admin | Read payments; *(SCR-52 deferred)* |
| Agent Pilot | VNPay create-url + **Return URL** — **không** build IPN / bank transfer / reconcile trừ khi reopen TD |

## 4. CONSTRAINTS

- Stack + secrets theo `CONSTITUTION.md` / `AGENTS.md`
- Idempotent **Return URL**
- Edu: **không** implement SCR-20/37/52 / cron reconcile / IPN (TD-010/011/012)

## 5. ASSUMPTIONS

1. **Online = VNPay-only** (TD-011) — accepted Spec Sync 2026-07-23. Cash remaining at desk (check-in) **kept**.
2. Pending VNPay fail → Customer retry / hold timeout (FR-04) hủy booking.
3. US6 Damage Fee → VNPay khi FR-23 sẵn.
4. Reconciliation (TD-010) vẫn deferred.
5. **VNPay IPN (TD-012)** deferred — Return URL đủ sandbox/edu.

## 6. OPEN QUESTIONS

1. Sandbox VNPay + Return URL DEV sẵn chưa?
2. Refund sau cancel: ghi Refunded thủ công hay gọi hoàn VNPay?
3. Mentor có bắt buộc CK/SCR-20 trên rubric không? (→ reopen TD-011)
4. Mentor có bắt buộc IPN trên rubric không? (→ reopen TD-012)
