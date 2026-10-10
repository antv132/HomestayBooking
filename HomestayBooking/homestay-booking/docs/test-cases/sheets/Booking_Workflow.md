# Booking_Workflow

| Field | Value |
|---|---|
| **Workflow** | Booking_Workflow |
| **Test requirement** | Kiểm tra discovery phòng (search/filter/detail/calendar), tạo booking hold PENDING_DEPOSIT (10 phút), chống double-book, thanh toán cọc 40% VNPay → CONFIRMED + contract, validation ngày/số khách/phòng không khả dụng. |
| **Number of TCs** | 32 |

| Testing Round | Passed | Failed | Pending | N/A |
|---|---:|---:|---:|---:|
| Round 1 | 0 | 0 | 32 | 0 |
| Round 2 | 0 | 0 | 32 | 0 |
| Round 3 | 0 | 0 | 32 | 0 |

### Cách điền Excel

- Hàng `Scenario …`: chỉ ghi tên ở cột Test Case ID (merge hàng nếu template yêu cầu); cột khác để trống.
- Hàng `BOOK_xxx`: điền đủ; Round = Pending.
- Number of TCs = **32** (không đếm hàng Scenario).

---

### Excel copy — paste theo đúng thứ tự hàng

| Test Case ID | Test Case Description | Test Case Procedure | Expected Results | Pre-conditions | Round 1 | Test date | Tester | Round 2 | Test date | Tester | Round 3 | Test date | Tester | Note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Scenario A — Search & discovery | | | | | | | | | | | | | | |
| BOOK_001 | Test search phòng với ngày hợp lệ trả về list | 1. Mở SCR-07. 2. Chọn check-in < check-out hợp lệ. 3. Search. | Hiện danh sách phòng AVAILABLE trong khoảng ngày. | Có room ACTIVE trống | Pending | | | Pending | | | Pending | | | |
| BOOK_002 | Test search không hiện phòng đã booked overlap | 1. Có booking CONFIRMED phòng X ngày D1–D2. 2. Search overlap D1–D2. | Phòng X không nằm trong kết quả bookable. | Booking CONFIRMED tồn tại | Pending | | | Pending | | | Pending | | | |
| BOOK_003 | Test search không hiện phòng đang hold PENDING_DEPOSIT | 1. Có hold còn hạn phòng Y. 2. User khác search overlap. | Phòng Y không bookable trong lúc hold. | Hold còn hiệu lực | Pending | | | Pending | | | Pending | | | |
| BOOK_004 | Test search check-out ≤ check-in bị validate | 1. Chọn check-out ≤ check-in. 2. Search/submit. | Lỗi validation ngày; không trả list sai. | SCR-07 mở | Pending | | | Pending | | | Pending | | | |
| BOOK_005 | Test search ngày quá khứ bị chặn | 1. Chọn check-in trong quá khứ. 2. Submit. | Báo lỗi không cho chọn ngày quá khứ. | Đồng hồ máy đúng | Pending | | | Pending | | | Pending | | | |
| BOOK_006 | Test filter theo số khách / capacity | 1. Search với số khách > capacity phòng nhỏ. | Chỉ hiện phòng đủ capacity (hoặc filter đúng rule). | Có phòng capacity khác nhau | Pending | | | Pending | | | Pending | | | |
| BOOK_007 | Test filter theo giá / loại phòng (nếu có) | 1. Áp filter giá min-max hoặc room type. 2. Search. | Kết quả khớp filter. | Có data đa dạng | Pending | | | Pending | | | Pending | | | N/A nếu chưa có filter |
| BOOK_008 | Test search không có phòng trống → empty state | 1. Chọn khoảng ngày tất cả phòng đều bận. 2. Search. | Empty state / thông báo không có phòng. | Data full booked | Pending | | | Pending | | | Pending | | | |
| Scenario B — Detail & calendar | | | | | | | | | | | | | | |
| BOOK_009 | Test Guest xem Room Detail không cần login | 1. Chưa login. 2. Mở SCR-08 một phòng. | Xem được ảnh, mô tả, tiện ích, giá; CTA book yêu cầu login nếu cần. | Room có content | Pending | | | Pending | | | Pending | | | |
| BOOK_010 | Test Room Detail hiển thị đủ thông tin | 1. Mở SCR-08. 2. Kiểm tra gallery, mô tả, amenities, giá/đêm. | Đủ field theo thiết kế; không broken image chính. | Room seed đầy đủ | Pending | | | Pending | | | Pending | | | |
| BOOK_011 | Test Availability Calendar đánh dấu đúng ngày bận | 1. Mở SCR-09 / calendar trên detail. 2. Đối chiếu booking thật. | Ngày overlap booked/hold hiển thị bận; ngày trống available (half-open). | Có booking mẫu | Pending | | | Pending | | | Pending | | | |
| BOOK_012 | Test chọn ngày trên calendar đưa vào checkout | 1. Chọn range trống trên calendar. 2. Tiếp tục book. | Checkout nhận đúng check-in/out đã chọn. | Customer có thể login | Pending | | | Pending | | | Pending | | | |
| BOOK_013 | Test Customer chưa login bị yêu cầu login khi book | 1. Guest bấm Book Now. | Redirect Login/Register; sau login quay lại flow (nếu có). | Session trống | Pending | | | Pending | | | Pending | | | |
| Scenario C — Create booking & hold | | | | | | | | | | | | | | |
| BOOK_014 | Test tạo booking thành công → PENDING_DEPOSIT | 1. Login Customer. 2. SCR-16 nhập số khách, xác nhận ngày. 3. Create. | Status PENDING_DEPOSIT; có holdExpiresAt ~10 phút; vào SCR-20. | Phòng trống; Customer ACTIVE | Pending | | | Pending | | | Pending | | | |
| BOOK_015 | Test hiển thị countdown hold trên SCR-20 | 1. Sau BOOK_014 mở Order Review. 2. Quan sát countdown. | Countdown giảm theo holdExpiresAt; cảnh báo sắp hết hạn. | Booking PENDING_DEPOSIT | Pending | | | Pending | | | Pending | | | |
| BOOK_016 | Test số tiền cọc hiển thị = 40% total | 1. Trên SCR-20 xem breakdown. | Deposit = 40% tổng; Remaining = 60%. | Giá phòng rõ ràng | Pending | | | Pending | | | Pending | | | |
| BOOK_017 | Test tạo booking số khách vượt capacity | 1. Nhập guests > max capacity phòng. 2. Submit. | Validation lỗi; không tạo booking. | Biết capacity phòng | Pending | | | Pending | | | Pending | | | |
| BOOK_018 | Test tạo booking thiếu ngày / số khách | 1. Bỏ trống field bắt buộc. 2. Submit. | Lỗi required; không tạo. | Form checkout mở | Pending | | | Pending | | | Pending | | | |
| BOOK_019 | Test double-book cùng phòng overlap bị conflict | 1. User A hold phòng X. 2. User B book overlap X. | B bị conflict/409; chỉ 1 hold hợp lệ. | Hold A còn hạn | Pending | | | Pending | | | Pending | | | |
| BOOK_020 | Test book phòng bị RoomStatusBlock overlap | 1. Manager block phòng X ngày D. 2. Customer book overlap D. | Bị từ chối; phòng không bookable. | Có RoomStatusBlock | Pending | | | Pending | | | Pending | | | |
| BOOK_021 | Test hold timeout hủy PENDING_DEPOSIT và nhả phòng | 1. Tạo PENDING_DEPOSIT. 2. Không trả đến hết hold (hoặc hạ timeout test). 3. Kiểm tra status + availability. | Booking cancelled theo hold job; phòng AVAILABLE lại. | Job hold timeout chạy | Pending | | | Pending | | | Pending | | | Có thể hạ timeout |
| BOOK_022 | Test không thanh toán được sau khi hold hết hạn | 1. Booking đã hết hold. 2. Thử mở VNPay deposit. | Bị chặn; thông báo hết hạn hold. | Hold expired | Pending | | | Pending | | | Pending | | | |
| Scenario D — Deposit VNPay & post-confirm | | | | | | | | | | | | | | |
| BOOK_023 | Test thanh toán cọc VNPay success → CONFIRMED | 1. PENDING_DEPOSIT còn hold. 2. Pay VNPay sandbox. 3. Return URL success. | DEPOSIT PAID; booking CONFIRMED; contract sinh/queue. | VNPay sandbox OK | Pending | | | Pending | | | Pending | | | |
| BOOK_024 | Test VNPay cancel/fail giữ PENDING_DEPOSIT | 1. Bắt đầu VNPay. 2. Cancel/fail. 3. Return. | Vẫn PENDING_DEPOSIT nếu còn hold; chưa CONFIRMED. | Còn hold | Pending | | | Pending | | | Pending | | | |
| BOOK_025 | Test thanh toán cọc chỉ cho phép VNPay (không CK) | 1. Trên SCR-20 kiểm tra phương thức. | Chỉ VNPay cho cọc; không có bank transfer deposit. | Theo ADR-007 | Pending | | | Pending | | | Pending | | | |
| BOOK_026 | Test Customer xem booking CONFIRMED trên list | 1. Sau BOOK_023 mở SCR-17. | Booking hiện status CONFIRMED đúng phòng/ngày. | BOOK_023 pass | Pending | | | Pending | | | Pending | | | |
| BOOK_027 | Test Booking Detail hiển thị cọc đã trả + remaining | 1. Mở SCR-18 booking CONFIRMED. | Thấy deposit paid, remaining 60% chưa trả (hoặc đúng trạng thái). | CONFIRMED | Pending | | | Pending | | | Pending | | | |
| BOOK_028 | Test Customer không xem booking của user khác | 1. Customer A mở detail id booking của B. | 403/404; không lộ data. | 2 customer có booking | Pending | | | Pending | | | Pending | | | |
| BOOK_029 | Test tính tổng tiền theo số đêm đúng | 1. Book N đêm giá P/đêm. 2. Xem total trên checkout. | Total = P × N (half-open check-out). | Biết công thức giá | Pending | | | Pending | | | Pending | | | |
| BOOK_030 | Test book nhiều đêm liên tiếp phòng trống | 1. Chọn range dài còn trống. 2. Create + pay deposit. | Booking CONFIRMED cho đúng range; calendar bận đúng range. | Phòng trống dài ngày | Pending | | | Pending | | | Pending | | | |
| BOOK_031 | Test promotion/code giảm giá (nếu có) hoặc N/A | 1. Nhập mã KM hợp lệ trên checkout. 2. Xem lại giá. | Giá giảm đúng rule; deposit 40% trên giá sau giảm. | Có promotion ACTIVE | Pending | | | Pending | | | Pending | | | N/A nếu chưa làm KM |
| BOOK_032 | Test API tạo booking không token → 401 | 1. Postman POST booking không Bearer. | 401 Unauthorized. | Biết endpoint create booking | Pending | | | Pending | | | Pending | | | |
