# CheckInOut_Workflow

| Field | Value |
|---|---|
| **Workflow** | CheckInOut_Workflow |
| **Test requirement** | Kiểm tra thu remaining 60% (cash quầy / VNPay), check-in kèm CMND đúng ngày & property scope, check-out → inspection pass/fail, damage report + ảnh ≤10MB, chặn checkout khi chưa trả damage, trigger housekeeping sau checkout pass. |
| **Number of TCs** | 32 |

| Testing Round | Passed | Failed | Pending | N/A |
|---|---:|---:|---:|---:|
| Round 1 | 0 | 0 | 32 | 0 |
| Round 2 | 0 | 0 | 32 | 0 |
| Round 3 | 0 | 0 | 32 | 0 |

### Cách điền Excel

- Hàng `Scenario …`: chỉ tên ở cột Test Case ID; cột khác trống.
- Hàng `CIO_xxx`: điền đủ; Round = Pending.
- Number of TCs = **32**.

---

### Excel copy — paste theo đúng thứ tự hàng

| Test Case ID | Test Case Description | Test Case Procedure | Expected Results | Pre-conditions | Round 1 | Test date | Tester | Round 2 | Test date | Tester | Round 3 | Test date | Tester | Note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Scenario A — Remaining payment | | | | | | | | | | | | | | |
| CIO_001 | Test chặn check-in khi remaining chưa PAID | 1. Staff mở check-in booking CONFIRMED đúng ngày. 2. Remaining chưa thu. 3. Submit CI. | Check-in bị chặn; yêu cầu thu remaining. | DEPOSIT PAID; remaining unpaid | Pending | | | Pending | | | Pending | | | |
| CIO_002 | Test thu remaining bằng cash tại quầy (remainingCollected) | 1. Manager/Employee thu cash remaining. 2. Xác nhận. | Payment REMAINING = PAID method CASH; đủ điều kiện CI. | Quyền staff; booking đúng property | Pending | | | Pending | | | Pending | | | |
| CIO_003 | Test thu remaining bằng VNPay success | 1. Khách/staff initiate VNPay REMAINING. 2. Return URL success. | REMAINING PAID method VNPay. | VNPay sandbox | Pending | | | Pending | | | Pending | | | |
| CIO_004 | Test VNPay remaining fail không đổi thành PAID | 1. VNPay remaining cancel/fail. | Remaining vẫn unpaid; CI vẫn bị chặn. | CONFIRMED | Pending | | | Pending | | | Pending | | | |
| CIO_005 | Test số tiền remaining = 60% total | 1. Đối chiếu remaining vs total booking. | Remaining ≈ 60% (làm tròn VND). | Biết total | Pending | | | Pending | | | Pending | | | |
| Scenario B — Check-in rules | | | | | | | | | | | | | | |
| CIO_006 | Test check-in thành công khi remaining PAID + CMND hợp lệ | 1. Nhập CMND. 2. Confirm check-in. | Status CHECKED_IN; ghi nhận thời điểm CI. | Remaining PAID; đúng ngày CI | Pending | | | Pending | | | Pending | | | |
| CIO_007 | Test check-in thiếu CMND / CMND invalid | 1. Bỏ trống hoặc sai format CMND. 2. Submit. | Validation lỗi; chưa CHECKED_IN. | Remaining PAID | Pending | | | Pending | | | Pending | | | |
| CIO_008 | Test chặn check-in trước ngày check-in | 1. Hôm nay < check-in date. 2. Thử CI (remaining đã PAID). | Bị chặn; thông báo chưa đến ngày. | Timezone Asia/Ho_Chi_Minh | Pending | | | Pending | | | Pending | | | |
| CIO_009 | Test check-in đúng ngày check-in thành công | 1. Hôm nay = check-in date. 2. CI. | CHECKED_IN OK. | Remaining + CMND OK | Pending | | | Pending | | | Pending | | | |
| CIO_010 | Test check-in muộn sau ngày CI (trước no-show) | 1. Quá ngày CI nhưng chưa NO_SHOW. 2. Thử CI theo policy. | Theo spec/code; không crash. Ghi Note. | Align FR check-in / no-show | Pending | | | Pending | | | Pending | | | |
| CIO_011 | Test không check-in booking CANCELLED | 1. Booking CANCELLED. 2. Thử CI. | Bị chặn. | CANCELLED | Pending | | | Pending | | | Pending | | | |
| CIO_012 | Test không check-in booking PENDING_DEPOSIT | 1. Chưa trả cọc. 2. Thử CI. | Bị chặn. | PENDING_DEPOSIT | Pending | | | Pending | | | Pending | | | |
| CIO_013 | Test không check-in 2 lần cùng booking | 1. Đã CHECKED_IN. 2. CI lại. | Lỗi / không đổi; idempotent an toàn. | CHECKED_IN | Pending | | | Pending | | | Pending | | | |
| CIO_014 | Test Manager/Employee sai property không CI được | 1. Staff Property A CI booking Property B. | 403; booking không đổi. | ≥2 property | Pending | | | Pending | | | Pending | | | |
| CIO_015 | Test Customer không gọi API check-in Manager | 1. Customer gọi endpoint manager check-in. | 403. | Token Customer | Pending | | | Pending | | | Pending | | | |
| CIO_016 | Test legacy check-in/out API bị block (TD-004) | 1. Gọi `/api/bookings/.../check-in` legacy. | Bị block; phải dùng `/api/v1/...` manager. | Biết legacy path | Pending | | | Pending | | | Pending | | | |
| Scenario C — Check-out & inspection | | | | | | | | | | | | | | |
| CIO_017 | Test bắt đầu check-out từ CHECKED_IN → PENDING_INSPECTION | 1. Staff request checkout/inspection. | Status PENDING_INSPECTION. | CHECKED_IN | Pending | | | Pending | | | Pending | | | |
| CIO_018 | Test không checkout khi chưa CHECKED_IN | 1. Booking CONFIRMED. 2. Thử checkout. | Bị chặn. | CONFIRMED | Pending | | | Pending | | | Pending | | | |
| CIO_019 | Test inspection PASS → CHECKED_OUT | 1. Inspection PASS. 2. Complete. | Booking CHECKED_OUT. | PENDING_INSPECTION | Pending | | | Pending | | | Pending | | | |
| CIO_020 | Test inspection PASS tạo Housekeeping task | 1. Sau CIO_019 kiểm tra HK list/dashboard. | Có HK task đúng phòng. | HK module bật | Pending | | | Pending | | | Pending | | | |
| CIO_021 | Test inspection FAIL yêu cầu damage report | 1. Chọn FAIL. 2. Không nhập report → submit. | Bắt buộc mô tả/ảnh; chưa complete checkout. | PENDING_INSPECTION | Pending | | | Pending | | | Pending | | | |
| CIO_022 | Test tạo damage report kèm ảnh hợp lệ | 1. FAIL + mô tả + upload ảnh jpg/png ≤10MB. 2. Submit. | Report lưu OK; vào nhánh duyệt damage. | File hợp lệ | Pending | | | Pending | | | Pending | | | |
| CIO_023 | Test upload ảnh damage >10MB bị từ chối | 1. Upload file >10MB. | Lỗi size; không lưu file. | File >10MB | Pending | | | Pending | | | Pending | | | |
| CIO_024 | Test upload ảnh damage sai MIME bị từ chối | 1. Upload .exe/.txt giả ảnh. | Lỗi type; không lưu. | File sai type | Pending | | | Pending | | | Pending | | | |
| Scenario D — Damage gate & scope | | | | | | | | | | | | | | |
| CIO_025 | Test PENDING_DAMAGE_PAYMENT chặn complete checkout | 1. Fee đã approve, chưa trả. 2. Thử complete CO. | Bị chặn đến khi DAMAGE_FEE PAID. | PENDING_DAMAGE_PAYMENT | Pending | | | Pending | | | Pending | | | |
| CIO_026 | Test sau trả DAMAGE_FEE thì checkout hoàn tất | 1. Pay damage. 2. Complete CO. | CHECKED_OUT. | DAMAGE_FEE PAID | Pending | | | Pending | | | Pending | | | |
| CIO_027 | Test inspection notes bắt buộc khi FAIL (nếu UI yêu cầu) | 1. FAIL bỏ trống note. 2. Submit. | Validation lỗi. | Form inspection | Pending | | | Pending | | | Pending | | | |
| CIO_028 | Test Employee thực hiện inspection đúng property | 1. Employee Property A inspect booking A. | Cho phép thao tác. | Employee đúng property | Pending | | | Pending | | | Pending | | | |
| CIO_029 | Test Employee không inspect booking property khác | 1. Employee A inspect booking B. | 403. | Scope khác | Pending | | | Pending | | | Pending | | | |
| CIO_030 | Test Booking Detail phản ánh đúng status sau CI/CO | 1. Sau từng bước mở detail Customer/Manager. | Status đồng bộ CHECKED_IN / PENDING_INSPECTION / CHECKED_OUT. | Có booking flow | Pending | | | Pending | | | Pending | | | |
| CIO_031 | Test API check-in không token → 401 | 1. POST check-in không Bearer. | 401. | Endpoint v1 manager | Pending | | | Pending | | | Pending | | | |
| CIO_032 | Test không dùng nhầm booking khác khi CI (đúng bookingId) | 1. Mở CI booking X. 2. Confirm. 3. Kiểm tra booking Y không đổi. | Chỉ X chuyển CHECKED_IN. | ≥2 booking | Pending | | | Pending | | | Pending | | | |
