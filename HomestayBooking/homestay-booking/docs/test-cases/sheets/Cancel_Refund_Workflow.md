# Cancel_Refund_Workflow

| Field | Value |
|---|---|
| **Workflow** | Cancel_Refund_Workflow |
| **Test requirement** | Kiểm tra hủy booking trước check-in: preview + thực thi tier hoàn cọc Customer (≥7d 100% / 3–7d 50% / <3d 0%); Manager hủy 100% bắt buộc cancelReason; scope property; chặn sau check-in; nhả phòng + cập nhật payment/refund/notify. |
| **Number of TCs** | 32 |

| Testing Round | Passed | Failed | Pending | N/A |
|---|---:|---:|---:|---:|
| Round 1 | 0 | 0 | 32 | 0 |
| Round 2 | 0 | 0 | 32 | 0 |
| Round 3 | 0 | 0 | 32 | 0 |

### Cách điền Excel

- Hàng `Scenario …`: chỉ tên scenario ở cột Test Case ID; cột khác trống.
- Hàng `CANC_xxx`: điền đủ; Round = Pending.
- Number of TCs = **32** (không đếm Scenario).

---

### Excel copy — paste theo đúng thứ tự hàng

| Test Case ID | Test Case Description | Test Case Procedure | Expected Results | Pre-conditions | Round 1 | Test date | Tester | Round 2 | Test date | Tester | Round 3 | Test date | Tester | Note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Scenario A — Customer preview & tiers | | | | | | | | | | | | | | |
| CANC_001 | Test mở màn Customer Cancel (SCR-19) từ booking CONFIRMED | 1. Login Customer. 2. Từ SCR-18 vào Cancel. | SCR-19 mở; hiện thông tin booking + policy tier. | Booking CONFIRMED của user | Pending | | | Pending | | | Pending | | | |
| CANC_002 | Test preview refund tier ≥7 ngày = 100% cọc | 1. Booking check-in ≥7d. 2. Xem preview trên SCR-19 / GET preview. | refundAmount preview = 100% DEPOSIT đã trả. | DEPOSIT PAID; ≥7d (Asia/Ho_Chi_Minh) | Pending | | | Pending | | | Pending | | | |
| CANC_003 | Test preview refund tier 3–7 ngày = 50% cọc | 1. Booking check-in còn 3–7d. 2. Preview. | Preview = 50% deposit. | DEPOSIT PAID; tier 3–7 | Pending | | | Pending | | | Pending | | | |
| CANC_004 | Test preview refund tier <3 ngày = 0% | 1. Booking check-in <3d. 2. Preview. | Preview = 0. | DEPOSIT PAID; <3d | Pending | | | Pending | | | Pending | | | |
| CANC_005 | Test biên đúng 7 ngày áp tier 100% | 1. Check-in đúng mốc ≥7d theo rule ngày. 2. Preview. | Áp 100% (không nhầm sang 50%). | Tính đúng timezone VN | Pending | | | Pending | | | Pending | | | |
| CANC_006 | Test biên đúng 3 ngày áp đúng tier (50% hoặc rule spec) | 1. Check-in đúng mốc 3d. 2. Preview. | Khớp spec (≥7 / 3–7 / <3); ghi Note nếu inclusive. | Spec FR-010 | Pending | | | Pending | | | Pending | | | |
| Scenario B — Customer cancel execution | | | | | | | | | | | | | | |
| CANC_007 | Test Customer confirm cancel tier ≥7d → CANCELLED + refund 100% | 1. Confirm cancel case ≥7d. 2. Kiểm tra booking + payment. | Status CANCELLED; refund 100% deposit; payment refunded theo policy. | CANC_002 data | Pending | | | Pending | | | Pending | | | |
| CANC_008 | Test Customer cancel tier 3–7d → refund 50% | 1. Confirm cancel. | CANCELLED; refundAmount = 50% deposit. | Tier 3–7 | Pending | | | Pending | | | Pending | | | |
| CANC_009 | Test Customer cancel tier <3d → refund 0% | 1. Confirm cancel. | CANCELLED; refund 0; cọc giữ. | Tier <3d | Pending | | | Pending | | | Pending | | | |
| CANC_010 | Test sau cancel phòng AVAILABLE lại trên search | 1. Cancel xong. 2. Guest/Customer search overlap ngày đó. | Phòng xuất hiện bookable. | Cancel success | Pending | | | Pending | | | Pending | | | |
| CANC_011 | Test Customer nhận notification sau cancel (nếu bật) | 1. Cancel. 2. Mở Notification Center. | Có thông báo hủy + (nếu có) số tiền hoàn. | Notification engine bật | Pending | | | Pending | | | Pending | | | N/A nếu tắt mail/noti |
| CANC_012 | Test cancel booking PENDING_DEPOSIT theo rule hệ thống | 1. Booking PENDING_DEPOSIT. 2. Thử cancel theo UI Customer. | Khớp code/spec (hủy hoặc hold expire); không crash. | PENDING_DEPOSIT | Pending | | | Pending | | | Pending | | | Align code |
| CANC_013 | Test Customer không cancel booking của người khác | 1. Customer A gọi cancel id của B. | 403/404; booking B không đổi. | 2 customer | Pending | | | Pending | | | Pending | | | |
| CANC_014 | Test cancel khi đã CANCELLED rồi → lỗi | 1. Booking đã CANCELLED. 2. Cancel lại. | Báo đã hủy / 409; không double refund. | Đã cancel | Pending | | | Pending | | | Pending | | | |
| Scenario C — Manager cancel | | | | | | | | | | | | | | |
| CANC_015 | Test Manager mở SCR-69 Cancel từ booking list | 1. Login Manager. 2. Từ SCR-35 vào cancel. | Form cancel hiện; yêu cầu cancelReason. | Manager đúng property | Pending | | | Pending | | | Pending | | | |
| CANC_016 | Test Manager cancel + reason → refund 100% cọc | 1. Nhập cancelReason. 2. Confirm. | CANCELLED; refund = 100% deposit (không theo tier Customer). | CONFIRMED + DEPOSIT PAID | Pending | | | Pending | | | Pending | | | |
| CANC_017 | Test Manager cancel thiếu cancelReason → 400 | 1. Submit không reason (UI/API). | 400 validation; status không đổi. | Booking còn hủy được | Pending | | | Pending | | | Pending | | | |
| CANC_018 | Test Manager cancel reason chỉ khoảng trắng → 400 | 1. Nhập reason toàn space. 2. Submit. | Validation lỗi; không hủy. | Form SCR-69 | Pending | | | Pending | | | Pending | | | |
| CANC_019 | Test Manager cancel booking property khác → 403 | 1. Manager Property A hủy booking Property B. | 403; booking không đổi. | ≥2 property | Pending | | | Pending | | | Pending | | | |
| CANC_020 | Test Employee không được Manager-cancel API | 1. Login Employee. 2. Gọi API manager cancel. | 403 Forbidden. | Biết endpoint manager cancel | Pending | | | Pending | | | Pending | | | |
| CANC_021 | Test Customer không gọi được API manager cancel | 1. Customer gọi manager cancel. | 403. | Token Customer | Pending | | | Pending | | | Pending | | | |
| Scenario D — Blocks, release room, isolation | | | | | | | | | | | | | | |
| CANC_022 | Test chặn cancel khi booking CHECKED_IN | 1. Booking CHECKED_IN. 2. Customer/Manager thử cancel. | Bị chặn; status giữ CHECKED_IN. | Đã check-in | Pending | | | Pending | | | Pending | | | |
| CANC_023 | Test chặn cancel khi PENDING_INSPECTION / sau check-in started | 1. Status sau check-in. 2. Thử cancel. | Bị chặn. | Post check-in status | Pending | | | Pending | | | Pending | | | |
| CANC_024 | Test chặn cancel booking CHECKED_OUT | 1. Booking CHECKED_OUT. 2. Thử cancel. | Bị chặn / không áp dụng. | CHECKED_OUT | Pending | | | Pending | | | Pending | | | |
| CANC_025 | Test Manager cancel vẫn nhả phòng dù gần ngày check-in | 1. Booking <3d còn CONFIRMED. 2. Manager cancel + reason. | CANCELLED; refund 100%; phòng AVAILABLE (khác tier Customer 0%). | <3d CONFIRMED | Pending | | | Pending | | | Pending | | | |
| CANC_026 | Test refund chỉ tính trên DEPOSIT đã trả, không hoàn remaining chưa thu | 1. Chỉ có DEPOSIT PAID, remaining chưa thu. 2. Cancel theo tier. | refundAmount dựa trên deposit; không bịa remaining. | Chưa thu remaining | Pending | | | Pending | | | Pending | | | |
| CANC_027 | Test sau cancel, Booking Detail hiện CANCELLED + refundAmount | 1. Mở SCR-18 sau cancel. | Status CANCELLED; hiển thị số hoàn (nếu UI có). | Cancel success | Pending | | | Pending | | | Pending | | | |
| CANC_028 | Test cancel không ảnh hưởng booking khác cùng Customer | 1. Customer có 2 booking. 2. Cancel 1 booking. | Booking còn lại nguyên status. | ≥2 booking | Pending | | | Pending | | | Pending | | | |
| CANC_029 | Test No-Show không nhầm với Customer cancel | 1. Quan sát booking NO_SHOW (job). 2. So với CANCELLED do user. | Status NO_SHOW khác CANCELLED; inventory released theo no-show policy. | Có case NO_SHOW hoặc N/A | Pending | | | Pending | | | Pending | | | Có thể N/A nếu chưa seed |
| CANC_030 | Test preview trước và sau khi sang mốc tier khác | 1. Dùng 2 booking khác mốc ngày. 2. So preview. | Mỗi booking đúng tier riêng; không cache sai. | 2 booking khác tier | Pending | | | Pending | | | Pending | | | |
| CANC_031 | Test API cancel không auth → 401 | 1. POST cancel không token. | 401. | Endpoint cancel | Pending | | | Pending | | | Pending | | | |
| CANC_032 | Test UI xác nhận hủy (confirm dialog) tránh bấm nhầm | 1. Bấm Cancel. 2. Ở dialog chọn No/Back. | Booking không hủy khi chưa confirm. | SCR-19 có confirm | Pending | | | Pending | | | Pending | | | N/A nếu không có dialog |
