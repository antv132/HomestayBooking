# Manager_Ops_Workflow

| Field | Value |
|---|---|
| **Workflow** | Manager_Ops_Workflow |
| **Test requirement** | Kiểm tra vận hành Manager theo property scope: dashboard KPI, structure (floor), room CRUD/list/filter, availability calendar, room status block, cô lập dữ liệu property, validation form, phân quyền. |
| **Number of TCs** | 32 |

| Testing Round | Passed | Failed | Pending | N/A |
|---|---:|---:|---:|---:|
| Round 1 | 0 | 0 | 32 | 0 |
| Round 2 | 0 | 0 | 32 | 0 |
| Round 3 | 0 | 0 | 32 | 0 |

### Cách điền Excel

- Hàng `Scenario …`: chỉ tên ở cột Test Case ID; cột khác trống.
- Hàng `MGR_xxx`: điền đủ; Round = Pending.
- Number of TCs = **32**.

---

### Excel copy — paste theo đúng thứ tự hàng

| Test Case ID | Test Case Description | Test Case Procedure | Expected Results | Pre-conditions | Round 1 | Test date | Tester | Round 2 | Test date | Tester | Round 3 | Test date | Tester | Note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Scenario A — Dashboard & structure | | | | | | | | | | | | | | |
| MGR_001 | Test login Manager vào Dashboard SCR-27 | 1. Login Manager. | Vào SCR-27; KPI load không 500. | Manager ACTIVE + property | Pending | | | Pending | | | Pending | | | |
| MGR_002 | Test KPI dashboard chỉ theo property được gán | 1. So KPI với data Property A vs B. | Không lẫn số liệu property ngoài scope. | Data A ≠ B | Pending | | | Pending | | | Pending | | | |
| MGR_003 | Test mở Structure Management SCR-28 | 1. Mở SCR-28. | Tree Property → Floor hiển thị. | Có floor | Pending | | | Pending | | | Pending | | | |
| MGR_004 | Test thêm Floor mới thành công | 1. Add floor (modal). 2. Nhập tên hợp lệ. 3. Save. | Floor xuất hiện trên tree. | Quyền Manager | Pending | | | Pending | | | Pending | | | |
| MGR_005 | Test sửa Floor thành công | 1. Edit tên floor. 2. Save. | Tên cập nhật đúng. | Có floor | Pending | | | Pending | | | Pending | | | |
| MGR_006 | Test thêm Floor thiếu tên → validation | 1. Add floor để trống tên. 2. Save. | Lỗi required; không tạo. | Form add floor | Pending | | | Pending | | | Pending | | | |
| MGR_007 | Test xóa/ẩn Floor khi còn phòng | 1. Thử xóa floor còn room. | Chặn hoặc cascade theo rule; không orphan sai. | Floor có room | Pending | | | Pending | | | Pending | | | Align UI |
| Scenario B — Room management | | | | | | | | | | | | | | |
| MGR_008 | Test Room Management List SCR-29 load | 1. Mở SCR-29. | List phòng property; có search/filter. | Có rooms | Pending | | | Pending | | | Pending | | | |
| MGR_009 | Test search phòng theo tên/mã | 1. Gõ keyword trùng 1 phòng. | Kết quả lọc đúng. | Nhiều phòng | Pending | | | Pending | | | Pending | | | |
| MGR_010 | Test filter phòng theo status | 1. Filter ACTIVE/MAINTENANCE… | List khớp filter. | Status đa dạng | Pending | | | Pending | | | Pending | | | N/A nếu thiếu filter |
| MGR_011 | Test mở Add Room SCR-30 | 1. Mở form tạo phòng. | Form đủ field bắt buộc. | Manager login | Pending | | | Pending | | | Pending | | | |
| MGR_012 | Test tạo phòng mới thành công | 1. Nhập tên, floor, giá, capacity, mô tả. 2. Save. | Phòng có trong list; bookable theo status. | Có floor | Pending | | | Pending | | | Pending | | | |
| MGR_013 | Test tạo phòng thiếu field bắt buộc | 1. Bỏ trống giá/tên/floor. 2. Save. | Validation lỗi. | Form Add Room | Pending | | | Pending | | | Pending | | | |
| MGR_014 | Test tạo phòng giá ≤ 0 bị từ chối | 1. Nhập price = 0 hoặc âm. 2. Save. | Lỗi validation giá. | Form Add Room | Pending | | | Pending | | | Pending | | | |
| MGR_015 | Test tạo phòng capacity ≤ 0 bị từ chối | 1. Capacity 0/âm. 2. Save. | Lỗi validation. | Form Add Room | Pending | | | Pending | | | Pending | | | |
| MGR_016 | Test edit phòng cập nhật giá/mô tả | 1. Sửa room. 2. Save. 3. Xem lại detail/list. | Dữ liệu mới được lưu. | Room tồn tại | Pending | | | Pending | | | Pending | | | |
| MGR_017 | Test upload ảnh phòng hợp lệ ≤10MB | 1. Upload jpg/png ≤10MB. | Ảnh hiển thị trên detail. | File hợp lệ | Pending | | | Pending | | | Pending | | | |
| MGR_018 | Test upload ảnh phòng >10MB bị từ chối | 1. Upload >10MB. | Lỗi size. | File lớn | Pending | | | Pending | | | Pending | | | |
| Scenario C — Calendar & blocks | | | | | | | | | | | | | | |
| MGR_019 | Test calendar ops phản ánh booking CONFIRMED | 1. Có CONFIRMED phòng X. 2. Xem calendar. | Ngày overlap occupied. | Booking thuộc property | Pending | | | Pending | | | Pending | | | |
| MGR_020 | Test calendar phản ánh hold PENDING_DEPOSIT | 1. Có hold còn hạn. 2. Xem calendar. | Ngày hold không hiện trống sai. | Hold active | Pending | | | Pending | | | Pending | | | |
| MGR_021 | Test tạo RoomStatusBlock khoảng ngày | 1. Block phòng X từ D1–D2 (maintenance). 2. Save. | Block lưu; Guest không book overlap. | Feature block có | Pending | | | Pending | | | Pending | | | |
| MGR_022 | Test xóa/hết block → phòng bookable lại | 1. Gỡ block. 2. Customer search overlap. | Phòng bookable. | Đã có block | Pending | | | Pending | | | Pending | | | |
| MGR_023 | Test block overlap booking hiện hữu xử lý đúng rule | 1. Thử block đè ngày đã CONFIRMED. | Chặn hoặc cảnh báo theo spec; không corruption data. | Có booking | Pending | | | Pending | | | Pending | | | Align spec |
| Scenario D — Isolation & authz | | | | | | | | | | | | | | |
| MGR_024 | Test Manager booking list chỉ property mình | 1. Mở list booking Manager. | Không thấy booking property khác. | ≥2 property | Pending | | | Pending | | | Pending | | | |
| MGR_025 | Test mở roomId property khác → 403/404 | 1. Đổi URL room Property B. | 403/404. | Biết id B | Pending | | | Pending | | | Pending | | | |
| MGR_026 | Test API Manager tạo room không token → 401 | 1. POST room không Bearer. | 401. | Endpoint manager room | Pending | | | Pending | | | Pending | | | |
| MGR_027 | Test Customer không truy cập SCR-28/29 | 1. Login Customer. 2. Mở URL Manager structure/rooms. | Redirect/403. | URL Manager | Pending | | | Pending | | | Pending | | | |
| MGR_028 | Test Employee không tạo room Manager | 1. Employee gọi API tạo room. | 403. | Token Employee | Pending | | | Pending | | | Pending | | | |
| MGR_029 | Test Manager nhiều property chuyển context (nếu có) | 1. Switch property A/B trên UI. 2. List đổi theo context. | Data theo property đang chọn. | Multi-property assignment | Pending | | | Pending | | | Pending | | | N/A nếu 1 property |
| MGR_030 | Test không tạo trùng mã phòng trong cùng property | 1. Tạo room trùng code. | Lỗi unique. | Rule unique code | Pending | | | Pending | | | Pending | | | N/A nếu không có code |
| MGR_031 | Test sau tạo phòng, Guest search thấy phòng (nếu ACTIVE) | 1. Tạo room ACTIVE. 2. Search Guest ngày trống. | Phòng xuất hiện kết quả. | Room ACTIVE | Pending | | | Pending | | | Pending | | | |
| MGR_032 | Test ẩn/deactivate phòng → không còn bookable | 1. Đặt room inactive/hidden. 2. Guest search. | Không hiện trong bookable list. | Có action deactivate | Pending | | | Pending | | | Pending | | | |
