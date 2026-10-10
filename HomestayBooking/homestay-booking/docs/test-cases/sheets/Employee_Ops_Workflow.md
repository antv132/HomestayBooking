# Employee_Ops_Workflow

| Field | Value |
|---|---|
| **Workflow** | Employee_Ops_Workflow |
| **Test requirement** | Kiểm tra Employee Dashboard composite (KPI, HK/MT/RI preview, todayTasks/awaiting/completedToday theo Asia/Ho_Chi_Minh), housekeeping sau checkout, maintenance ticket + upload, inspection trong scope property; RBAC/isolation; dashboard chủ yếu read-only theo FR-22. |
| **Number of TCs** | 32 |

| Testing Round | Passed | Failed | Pending | N/A |
|---|---:|---:|---:|---:|
| Round 1 | 0 | 0 | 32 | 0 |
| Round 2 | 0 | 0 | 32 | 0 |
| Round 3 | 0 | 0 | 32 | 0 |

### Cách điền Excel

- Hàng `Scenario …`: chỉ tên ở cột Test Case ID; cột khác trống.
- Hàng `EMP_xxx`: điền đủ; Round = Pending.
- Number of TCs = **32**.

---

### Excel copy — paste theo đúng thứ tự hàng

| Test Case ID | Test Case Description | Test Case Procedure | Expected Results | Pre-conditions | Round 1 | Test date | Tester | Round 2 | Test date | Tester | Round 3 | Test date | Tester | Note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Scenario A — Dashboard composite | | | | | | | | | | | | | | |
| EMP_001 | Test login Employee vào SCR-59 | 1. Login Employee. | Vào Employee Dashboard; không vào Manager/Admin. | Employee ACTIVE | Pending | | | Pending | | | Pending | | | |
| EMP_002 | Test GET employee dashboard API 200 | 1. Mở SCR-59 / gọi `GET /api/v1/employee/dashboard`. | 200; payload KPI + previews + task groups. | BE FR-22 | Pending | | | Pending | | | Pending | | | |
| EMP_003 | Test KPI cards hiển thị số không âm / hợp lý | 1. Quan sát KPI trên SCR-59. | Số ≥0; khớp property scope (không NaN). | Có/không có data đều OK | Pending | | | Pending | | | Pending | | | |
| EMP_004 | Test preview Housekeeping trên dashboard | 1. Có HK task. 2. Xem preview HK. | Preview list/count phản ánh task property. | Có HK task | Pending | | | Pending | | | Pending | | | |
| EMP_005 | Test preview Maintenance trên dashboard | 1. Có MT ticket. 2. Xem preview MT. | Preview đúng ticket scope. | Có MT | Pending | | | Pending | | | Pending | | | |
| EMP_006 | Test preview Room Inspection trên dashboard | 1. Có RI pending. 2. Xem preview RI. | Preview đúng case inspection. | Có RI | Pending | | | Pending | | | Pending | | | |
| EMP_007 | Test nhóm todayTasks chỉ task hôm nay (VN time) | 1. Seed task today + yesterday. 2. Refresh dashboard. | todayTasks chỉ hôm nay Asia/Ho_Chi_Minh. | Seed 2 ngày | Pending | | | Pending | | | Pending | | | |
| EMP_008 | Test nhóm awaiting chứa task chờ xử lý | 1. Có task awaiting. 2. Xem nhóm awaiting. | Task đúng trạng thái awaiting. | Có awaiting | Pending | | | Pending | | | Pending | | | |
| EMP_009 | Test nhóm completedToday sau khi hoàn thành trong ngày | 1. Complete 1 task hôm nay. 2. Xem completedToday. | Task xuất hiện completedToday. | Có thể complete task | Pending | | | Pending | | | Pending | | | |
| EMP_010 | Test dashboard empty state khi không có task | 1. Employee property không có task hôm nay. | Empty state rõ; không crash. | Property sạch task | Pending | | | Pending | | | Pending | | | |
| EMP_011 | Test refresh dashboard cập nhật số liệu | 1. Tạo thêm HK task (sau CO). 2. Refresh SCR-59. | Số/preview tăng đúng. | Có quyền tạo gián tiếp qua CO | Pending | | | Pending | | | Pending | | | |
| Scenario B — Housekeeping | | | | | | | | | | | | | | |
| EMP_012 | Test HK task tạo sau inspection PASS / CHECKED_OUT | 1. Hoàn tất CIO pass. 2. Employee xem HK. | Có task đúng room vừa CO. | CIO_019/020 | Pending | | | Pending | | | Pending | | | |
| EMP_013 | Test xem chi tiết HK task | 1. Mở 1 HK task. | Thấy phòng, trạng thái, thời gian tạo. | Có HK | Pending | | | Pending | | | Pending | | | |
| EMP_014 | Test cập nhật HK IN_PROGRESS (nếu UI cho) | 1. Đổi status → IN_PROGRESS. | Lưu thành công; dashboard cập nhật. | Mutation HK có | Pending | | | Pending | | | Pending | | | N/A nếu read-only |
| EMP_015 | Test cập nhật HK DONE → vào completedToday | 1. Đổi → DONE. | completedToday có task; awaiting giảm. | EMP_014 path | Pending | | | Pending | | | Pending | | | N/A nếu read-only |
| EMP_016 | Test không complete HK của property khác | 1. Gọi update HK task Property B. | 403. | Task B | Pending | | | Pending | | | Pending | | | |
| Scenario C — Maintenance & inspection | | | | | | | | | | | | | | |
| EMP_017 | Test tạo Maintenance ticket | 1. Tạo ticket gắn phòng property. 2. Save. | Ticket lưu; hiện trong list/preview. | Quyền tạo/xem | Pending | | | Pending | | | Pending | | | |
| EMP_018 | Test Maintenance ticket bắt buộc mô tả | 1. Tạo ticket bỏ trống description. | Validation lỗi. | Form MT | Pending | | | Pending | | | Pending | | | |
| EMP_019 | Test upload ảnh MT ≤10MB thành công | 1. Upload ảnh hợp lệ. | Ảnh đính kèm OK. | File ≤10MB | Pending | | | Pending | | | Pending | | | |
| EMP_020 | Test upload ảnh MT >10MB fail | 1. Upload >10MB. | Từ chối size. | File lớn | Pending | | | Pending | | | Pending | | | |
| EMP_021 | Test Employee xem list MT chỉ trong property | 1. Có ticket A và B khác property. 2. Emp A xem list. | Chỉ thấy ticket A. | ≥2 property | Pending | | | Pending | | | Pending | | | |
| EMP_022 | Test Employee thực hiện room inspection trong scope | 1. Mở case PENDING_INSPECTION property mình. 2. PASS/FAIL. | Cho phép thao tác. | Có RI | Pending | | | Pending | | | Pending | | | |
| EMP_023 | Test Employee không inspect property khác | 1. Mở RI Property B. | 403/404. | RI thuộc B | Pending | | | Pending | | | Pending | | | |
| EMP_024 | Test điều hướng từ dashboard preview sang màn HK/MT/RI | 1. Click item preview. | Sang đúng màn ops tương ứng. | UI có link | Pending | | | Pending | | | Pending | | | |
| Scenario D — Scope, RBAC, negative | | | | | | | | | | | | | | |
| EMP_025 | Test dashboard read-only: không mutate booking từ SCR-59 | 1. Trên SCR-59 tìm action sửa booking/payment. | Không có / không cho mutate booking; chỉ xem + navigate. | Spec FR-22 | Pending | | | Pending | | | Pending | | | |
| EMP_026 | Test Customer không vào SCR-59 | 1. Login Customer. 2. Mở URL employee dashboard. | 403/redirect. | URL SCR-59 | Pending | | | Pending | | | Pending | | | |
| EMP_027 | Test Manager gọi employee dashboard API | 1. Manager gọi `/api/v1/employee/dashboard`. | 403 hoặc khớp security config. | Token Manager | Pending | | | Pending | | | Pending | | | Align security |
| EMP_028 | Test API dashboard không token → 401 | 1. GET dashboard không Bearer. | 401. | Endpoint employee dashboard | Pending | | | Pending | | | Pending | | | |
| EMP_029 | Test timezone: task gần nửa đêm VN tính đúng ngày VN | 1. Seed task gần 23:30 VN. 2. Xem todayTasks. | Không lệch sang ngày UTC nhầm. | Kiểm soát giờ server/test | Pending | | | Pending | | | Pending | | | |
| EMP_030 | Test Employee Suspended không login dashboard | 1. Login Employee SUSPENDED. | Bị chặn login. | User SUSPENDED | Pending | | | Pending | | | Pending | | | |
| EMP_031 | Test nhiều task cùng phòng vẫn phân nhóm đúng | 1. 1 phòng có HK + MT. 2. Xem dashboard. | Mỗi loại đúng preview/group; không gộp sai. | Seed đa loại | Pending | | | Pending | | | Pending | | | |
| EMP_032 | Test lỗi mạng / API fail hiện error state trên SCR-59 | 1. Tắt BE hoặc mock 500. 2. Mở dashboard. | Error/retry UI; không trắng trang. | Có thể tắt BE tạm | Pending | | | Pending | | | Pending | | | |
