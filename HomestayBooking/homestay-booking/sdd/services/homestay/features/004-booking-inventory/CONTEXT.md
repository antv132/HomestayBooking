# CONTEXT.md — FR-04 Booking & Inventory
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/004-booking-inventory/spec.md`
# Sprint: 2 (primary)

## 1. PROBLEM STATEMENT

Homestay mất tiền và mất chỗ trống khi vòng đời booking không khép kín.

Pain cụ thể:
- Khách giữ phòng (`PENDING_DEPOSIT`) rồi không trả cọc — phòng vẫn bị “treo”.
- Khách đã cọc (`CONFIRMED`) nhưng không đến check-in — phòng vẫn khóa cả kỳ, chỗ chỉ cầm được ~40% cọc, mất cơ hội bán lại.
- Khách hủy gần ngày nhận phòng nhưng chính sách hoàn cọc không rõ / code đang hoàn cố định — chủ nhà hoặc khách bị thiệt.
- Hai người đặt trùng phòng cùng đêm → overbooking.
- Manager không sửa được ngày/phòng booking đã Confirmed khi khách xin đổi — phải hủy/tạo lại thủ công.

Tránh nhảy vào giải pháp; đây là pain vận hành + doanh thu + trải nghiệm.

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa |
|-----------|--------|
| PENDING_DEPOSIT | Đã tạo booking, chờ cọc; hold có hạn |
| CONFIRMED | Đã cọc; khóa tồn kho theo khoảng ngày |
| NO_SHOW | Không check-in trước deadline `checkInDate.atStartOfDay() + 24h`; cọc không hoàn; nhả phòng |
| Hold timeout | Hết hạn giữ chỗ chưa cọc → CANCELLED + nhả phòng (spec mặc định 10 phút) |
| Deposit / Remaining | 40% / 60% tổng (snapshot lúc tạo booking); damage fee tách `damageFeeAmount` |
| Inventory lock | Pessimistic lock Room + half-open overlap `[checkIn, checkOut)` + RoomStatusBlock |
| Check-in date | `LocalDate` — không lưu giờ check-in; no-show neo 00:00 |
| Cancel tiers | ≥7d 100%; `3≤d<7` 50%; `<3` 0% (Customer). Manager cancel: 100% |

Canonical statuses: xem `AGENTS.md` / enum `Booking.Status`.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Customer | Book, trả cọc, hủy theo policy, check-in |
| Manager | Verify CK, check-in/out, hủy 100%, (FR-015) sửa booking |
| Property owner / Admin | Doanh thu, chống overbooking, no-show |
| Spec Architect / Domain Owner | Chốt convention 24h no-show với date-only |
| Agent Pilot | Implement jobs/tiers trong Sprint 2 |

## 4. CONSTRAINTS

**Tech**
- Java 17 / Spring / SQL Server / JPA — `CONSTITUTION.md`
- Dates = `LocalDate` (không đổi model datetime trừ khi amend + migration)
- Jobs: `@Scheduled` (đã có `BookingHoldTimeoutJob`)

**Business**
- Không overbooking; snapshot giá không bị đổi `PricePerNight` sau
- No-show: giữ cọc; Cancelled/No-show nhả tồn kho
- Spec `004` thắng narrative docs cũ nếu conflict

**Time**
- Sprint 2: B-001…B-005 trong `sdd/sprints/backlog.md`

## 5. ASSUMPTIONS

1. No-show với date-only: dùng `checkInDate + 1 day` (sau hết ngày nhận phòng) hoặc `checkInDate` @ 00:00 + 24h — **cần Spec Sync chốt một**.
2. Hold timeout mặc định 10 phút — khớp `application.yml` (`BOOKING_HOLD_TIMEOUT_MINUTES`).
3. Refund tier tính trên **deposit đã trả**, không trên total.
4. Manager modify (FR-015) chỉ khi `CONFIRMED`.
5. Legacy check-in/out API không phải path chuẩn — ưu tiên `/api/v1/manager/...`.

## 6. OPEN QUESTIONS

1. Convention no-show chính thức với `LocalDate` only: midnight+24h hay end-of-check-in-day?
2. Job no-show chạy mỗi bao nhiêu phút? (research gợi ý 15 phút)
3. Cancel tier: ngày lịch (calendar days) hay 24h periods?
4. FR-015 có tạo payment line khi `priceDelta > 0` trong Sprint 2 không, hay chỉ đổi ngày/phòng?
5. Có thông báo in-app/email khi NO_SHOW trong Sprint 2 không?

---

**Next:** Spec Sync chốt OQ → cập nhật `spec.md` / research nếu cần → Agent Jam implement theo `sprint-2-plan.md`.
