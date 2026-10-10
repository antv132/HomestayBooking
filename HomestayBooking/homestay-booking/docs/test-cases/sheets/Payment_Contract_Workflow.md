# Payment_Contract_Workflow

| Field | Value |
|---|---|
| **Workflow** | Payment_Contract_Workflow |
| **Test requirement** | Kiểm tra contract PDF sau cọc; payment history; tỷ lệ 40/60; DAMAGE_FEE approve ≤5M / escalate >5M; thanh toán DEPOSIT/REMAINING/DAMAGE qua VNPay Return URL; cô lập dữ liệu payment/contract theo user; không test reconcile/IPN/CK (deferred). |
| **Number of TCs** | 32 |

| Testing Round | Passed | Failed | Pending | N/A |
|---|---:|---:|---:|---:|
| Round 1 | 0 | 0 | 32 | 0 |
| Round 2 | 0 | 0 | 32 | 0 |
| Round 3 | 0 | 0 | 32 | 0 |

### Cách điền Excel

- Hàng `Scenario …`: chỉ tên ở cột Test Case ID; cột khác trống.
- Hàng `PAY_xxx`: điền đủ; Round = Pending.
- Number of TCs = **32**.

---

### Excel copy — paste theo đúng thứ tự hàng

| Test Case ID | Test Case Description | Test Case Procedure | Expected Results | Pre-conditions | Round 1 | Test date | Tester | Round 2 | Test date | Tester | Round 3 | Test date | Tester | Note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Scenario A — Contract | | | | | | | | | | | | | | |
| PAY_001 | Test contract xuất hiện sau DEPOSIT PAID → CONFIRMED | 1. Pay deposit success. 2. Mở SCR-21 My Contract List. | Có contract gắn booking. | Outbox/contract job chạy | Pending | | | Pending | | | Pending | | | |
| PAY_002 | Test xem PDF contract / drawer thành công | 1. Mở contract detail. 2. View/download PDF. | PDF mở được; có thông tin booking/khách/phòng. | PAY_001 | Pending | | | Pending | | | Pending | | | |
| PAY_003 | Test chưa trả cọc thì chưa có contract (hoặc chưa final) | 1. Booking PENDING_DEPOSIT. 2. Mở contract list. | Không có contract final cho booking đó. | Chưa DEPOSIT PAID | Pending | | | Pending | | | Pending | | | |
| PAY_004 | Test contract list chỉ hiện contract của Customer đang login | 1. Customer A xem list. | Không thấy contract Customer B. | 2 customer có contract | Pending | | | Pending | | | Pending | | | |
| PAY_005 | Test mở contractId của user khác → 403/404 | 1. A mở URL contract của B. | 403/404. | Biết id B | Pending | | | Pending | | | Pending | | | |
| Scenario B — Payment history & amounts | | | | | | | | | | | | | | |
| PAY_006 | Test Payment History (SCR-26) load danh sách | 1. Login Customer có ≥1 payment. 2. Mở SCR-26. | List hiển thị amount, type, status, method, thời gian. | Có payment | Pending | | | Pending | | | Pending | | | |
| PAY_007 | Test history hiện DEPOSIT sau trả cọc | 1. Sau VNPay deposit. 2. Xem history. | Dòng DEPOSIT PAID VNPay đúng số 40%. | Deposit success | Pending | | | Pending | | | Pending | | | |
| PAY_008 | Test history hiện REMAINING sau thu remaining | 1. Thu remaining cash/VNPay. 2. Xem history. | Dòng REMAINING PAID đúng method. | Remaining paid | Pending | | | Pending | | | Pending | | | |
| PAY_009 | Test history hiện DAMAGE_FEE sau trả damage | 1. Pay damage. 2. Xem history. | Dòng DAMAGE_FEE PAID. | Damage paid | Pending | | | Pending | | | Pending | | | |
| PAY_010 | Test tỷ lệ DEPOSIT 40% + REMAINING 60% = total | 1. Cộng 2 payment đã PAID. | Tổng khớp total booking (làm tròn VND). | Đã trả đủ 2 phần | Pending | | | Pending | | | Pending | | | |
| PAY_011 | Test filter/search payment history theo loại (nếu có) | 1. Filter DEPOSIT only. | Chỉ hiện DEPOSIT. | UI filter có | Pending | | | Pending | | | Pending | | | N/A nếu chưa có |
| PAY_012 | Test empty Payment History khi user chưa giao dịch | 1. Customer mới chưa book. 2. Mở SCR-26. | Empty state; không lỗi. | User chưa payment | Pending | | | Pending | | | Pending | | | |
| Scenario C — Damage fee | | | | | | | | | | | | | | |
| PAY_013 | Test Manager approve damage ≤ 5.000.000 VND | 1. Damage report ≤5M. 2. Manager approve. | PENDING_DAMAGE_PAYMENT; tạo DAMAGE_FEE pending. | Report submitted | Pending | | | Pending | | | Pending | | | |
| PAY_014 | Test damage = đúng 5.000.000 vẫn theo nhánh ≤5M (Manager) | 1. Fee = 5.000.000. 2. Approve. | Không escalate; Manager duyệt đủ. | Fee đúng biên 5M | Pending | | | Pending | | | Pending | | | |
| PAY_015 | Test damage > 5.000.000 phải escalate Admin | 1. Fee >5M. 2. Manager path. | Trạng thái chờ Admin co-approve; chưa final charge. | Fee >5M | Pending | | | Pending | | | Pending | | | Không full Admin sheet |
| PAY_016 | Test chưa approve damage thì chưa có DAMAGE_FEE PAID path | 1. Report mới submit chưa duyệt. 2. Customer thử pay damage. | Không cho trả / chưa có payment payable. | Chưa approve | Pending | | | Pending | | | Pending | | | |
| PAY_017 | Test thanh toán DAMAGE_FEE VNPay success | 1. PENDING_DAMAGE_PAYMENT. 2. VNPay success Return URL. | DAMAGE_FEE PAID. | VNPay sandbox | Pending | | | Pending | | | Pending | | | |
| PAY_018 | Test DAMAGE_FEE VNPay fail giữ chưa PAID | 1. VNPay fail/cancel. | Vẫn pending pay; checkout vẫn chặn. | Pending damage pay | Pending | | | Pending | | | Pending | | | |
| PAY_019 | Test sau DAMAGE_FEE PAID mở khóa checkout | 1. Pay xong. 2. Staff complete CO. | CHECKED_OUT OK. | PAY_017 | Pending | | | Pending | | | Pending | | | |
| Scenario D — Security / negative / deferred | | | | | | | | | | | | | | |
| PAY_020 | Test không double-charge DEPOSIT khi Return URL gọi lại | 1. Pay deposit success. 2. Replay Return URL / refresh. | Vẫn 1 payment DEPOSIT PAID; booking CONFIRMED 1 lần. | Có thể replay return | Pending | | | Pending | | | Pending | | | |
| PAY_021 | Test payment gắn đúng bookingId | 1. Tạo 2 booking. 2. Pay deposit booking 1. | Chỉ booking 1 CONFIRMED; payment.bookingId = 1. | 2 booking | Pending | | | Pending | | | Pending | | | |
| PAY_022 | Test Customer không xem payment của user khác | 1. A mở payment id của B. | 403/404. | 2 users | Pending | | | Pending | | | Pending | | | |
| PAY_023 | Test Manager xem payment trong property scope (nếu UI có) | 1. Manager mở payment/booking property mình. | Xem được trong scope. | Manager UI payment | Pending | | | Pending | | | Pending | | | N/A nếu không có màn |
| PAY_024 | Test refund sau cancel phản ánh trên history | 1. Cancel có refund >0. 2. Xem history/detail. | Có bản ghi/status refund khớp số tiền tier. | Cancel refund >0 | Pending | | | Pending | | | Pending | | | |
| PAY_025 | Test không có UI bank transfer deposit (TD-011 deferred) | 1. Kiểm tra SCR-20 phương thức cọc. | Không có CK/upload biên lai cho deposit; chỉ VNPay. | ADR-007 | Pending | | | Pending | | | Pending | | | Pass = không có CK |
| PAY_026 | Test không yêu cầu VNPay IPN để confirm (Return URL only) | 1. Pay VNPay chỉ qua Return URL (không IPN). | Booking vẫn CONFIRMED. | Edu TD-012 | Pending | | | Pending | | | Pending | | | |
| PAY_027 | Test contract vẫn xem được sau check-out | 1. Booking CHECKED_OUT. 2. Mở lại contract. | PDF vẫn xem được. | Có contract | Pending | | | Pending | | | Pending | | | |
| PAY_028 | Test số tiền hiển thị format VND | 1. Xem SCR-20/26/21. | Format tiền VND rõ; không hiện sai currency. | UI VN | Pending | | | Pending | | | Pending | | | |
| PAY_029 | Test API tạo payment URL không auth → 401 | 1. Gọi initiate VNPay không token. | 401. | Endpoint payment | Pending | | | Pending | | | Pending | | | |
| PAY_030 | Test không pay DEPOSIT cho booking đã CONFIRMED | 1. Booking CONFIRMED. 2. Thử pay deposit lại. | Bị chặn / không tạo DEPOSIT thứ 2. | Đã trả cọc | Pending | | | Pending | | | Pending | | | |
| PAY_031 | Test SCR-52 reconcile UI không bắt buộc (deferred N/A) | 1. Ghi nhận không test reconcile Admin. | Đánh N/A cho TD-010; không fail suite. | Deferred edu | Pending | | | Pending | | | Pending | | | N/A |
| PAY_032 | Test tổng quan 1 booking đủ DEPOSIT+REMAINING(+DAMAGE) trên detail | 1. Booking đã đủ các payment. 2. Mở detail/history. | Đủ dòng; status booking đúng giai đoạn. | Flow đủ tiền | Pending | | | Pending | | | Pending | | | |
