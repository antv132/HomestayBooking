# Manual Test Cases — Homestay Booking System

Bộ tài liệu test case thủ công theo template Excel (TEST CASE LIST + Workflow sheets).

## Files

| File | Sheet Excel tương ứng |
|---|---|
| [TEST_CASE_LIST.md](./TEST_CASE_LIST.md) | `TEST CASE LIST` |
| [TEST_REPORT.md](./TEST_REPORT.md) | `TEST STATISTICS` (Release 1) |
| [sheets/Auth_Workflow.md](./sheets/Auth_Workflow.md) | `Auth_Workflow` |
| [sheets/Booking_Workflow.md](./sheets/Booking_Workflow.md) | `Booking_Workflow` |
| [sheets/Cancel_Refund_Workflow.md](./sheets/Cancel_Refund_Workflow.md) | `Cancel_Refund_Workflow` |
| [sheets/CheckInOut_Workflow.md](./sheets/CheckInOut_Workflow.md) | `CheckInOut_Workflow` |
| [sheets/Payment_Contract_Workflow.md](./sheets/Payment_Contract_Workflow.md) | `Payment_Contract_Workflow` |
| [sheets/Manager_Ops_Workflow.md](./sheets/Manager_Ops_Workflow.md) | `Manager_Ops_Workflow` |
| [sheets/Employee_Ops_Workflow.md](./sheets/Employee_Ops_Workflow.md) | `Employee_Ops_Workflow` |

## Summary

| Workflow | # TCs | ID range |
|---|---:|---|
| Auth_Workflow | 32 | AUTH_001 – AUTH_032 |
| Booking_Workflow | 32 | BOOK_001 – BOOK_032 |
| Cancel_Refund_Workflow | 32 | CANC_001 – CANC_032 |
| CheckInOut_Workflow | 32 | CIO_001 – CIO_032 |
| Payment_Contract_Workflow | 32 | PAY_001 – PAY_032 |
| Manager_Ops_Workflow | 32 | MGR_001 – MGR_032 |
| Employee_Ops_Workflow | 32 | EMP_001 – EMP_032 |
| **Total** | **224** | |

Admin workflow: intentionally omitted.

## How to use in Excel

1. Tạo workbook với 8 tab: `TEST CASE LIST` + 7 workflow ở trên.
2. Copy bảng **Excel copy — paste theo đúng thứ tự hàng** trong mỗi file (đã xen hàng Scenario).
3. Điền header: Workflow, Test requirement, **Number of TCs = 32**, Round counts `Pending = 32`.
4. **Hàng Scenario** (vd. `Scenario A — Register validation`):
   - Chỉ điền tên scenario vào cột **Test Case ID** (hoặc merge cả hàng như template gốc).
   - Các cột Description / Procedure / Expected / Round… **để trống**.
   - **Không** tính vào Number of TCs.
5. **Hàng TC** (`AUTH_001`, `BOOK_001`…): điền đủ cột; Round = `Pending` ban đầu.
6. Khi chạy test: đổi `Pending` → `Passed`/`Failed`/`N/A`, điền Test date + Tester, cập nhật bảng Round ở header.
7. Một số TC ghi **N/A** trong Note khi feature chưa bật (OAuth, promotion, filter UI, HK mutation…).

## Out of scope (do not fail the suite)

- TD-010 Payment reconciliation UI  
- TD-011 Bank transfer + CK verify  
- TD-012 VNPay IPN (Return URL only)
