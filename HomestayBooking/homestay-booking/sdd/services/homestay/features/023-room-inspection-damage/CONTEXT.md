# CONTEXT.md — FR-23 Room Inspection & Damage Resolution
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/023-room-inspection-damage/spec.md`

## 1. PROBLEM STATEMENT

Check-out có thể xảy ra khi phòng chưa kiểm tra hoặc còn hư hại chưa thu phí; thiếu phân tách quyền (Employee ghi nhận / Manager duyệt / Admin co-approve / Customer dispute).

Pain cụ thể:
- Booking Checked-in sắp check-out không có checklist Pass/Fail → damage bị bỏ sót.
- Fail không có DamageReport + DamageItem + ảnh → không phê duyệt phí.
- Check-out không bị chặn khi inspection Pending hoặc Damage Fee chưa trả.
- Phí > 5.000.000 VND thiếu Admin co-approval; Customer thiếu cửa Dispute 24h.
- Manager/Employee vượt Property; Customer bị tính phí không biết / không phản đối được.

Không mô tả giải pháp — chỉ pain: thiếu gate inspection + damage fee SoD trước Checked-out (precondition FR-21 HK).

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa trong hệ thống này |
|-----------|-------------------------|
| RoomInspection | Checklist trước check-out: Pending → Passed / Failed With Damage |
| DamageReport / DamageItem | Báo cáo hư hại + dòng chi phí ước tính + ảnh |
| Pending Approval → Approved / Escalated / Disputed | Lifecycle phê duyệt phí |
| Threshold 5M VND | Manager approve ≤5M; >5M → Escalated cần Admin |
| Damage Fee | Cộng Remaining Balance; thanh toán qua FR-12 |
| Dispute window | Customer phản đối trong 24h sau Approved |
| Checkout gate | Cần Passed (hoặc damage approved + paid) + balance settled |

Ranh giới: FR-04 owns check-out UI nhưng gate thuộc FR-23; Contract addendum damage = P2; FR-21 sau checkout thành công.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Employee | Inspect + tạo DamageReport/Items (Property scope) |
| Manager | Xem inspection; approve ≤5M (SCR-42/43) |
| Admin | Co-approve Escalated (SCR-53); xử lý Dispute |
| Customer | Thanh toán Damage Fee; Dispute 24h |
| Spec Architect / Agent Pilot | Spec + implement `023-room-inspection-damage` |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- Property-scoped RBAC; validate ảnh trước ghi disk

**Business**
- Segregation of Duties: Employee create ≠ Manager approve ≠ Admin escalate
- Damage > 5.000.000 VND cần Admin co-approval (AGENTS.md)
- Không check-out khi inspection/damage payment chưa xong
- Notify damage qua FR-15

**Time / Process**
- Artifacts: `sdd/services/homestay/features/023-room-inspection-damage/`

## 5. ASSUMPTIONS

1. Passed là terminal cho Employee (Manager override out of scope v1).
2. Threshold mặc định 5M, configurable.
3. Dispute chặn thanh toán đến khi Admin xử lý (resolve UI P2).
4. v1 cập nhật Remaining Balance; PDF addendum P2.
5. Chỉ Employee tạo DamageReport (không Manager/Customer create).

## 6. OPEN QUESTIONS

1. Checklist items cố định (TV/Minibar/Bed/Bathroom) hay cấu hình được?
2. Admin resolve Dispute: adjust fee / reject / waive — UI v1?
3. Threshold 5M lấy từ SystemSetting (FR-17) hay constant?
4. Nhiều DamageReport cho một inspection có cho phép?
5. Đồng hồ 24h Dispute theo timezone nào?
