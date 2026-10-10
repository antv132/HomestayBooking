# TEST REPORT — Homestay Booking System

## Document control

| Field | Value |
|---|---|
| **Project Name** | Homestay Booking System |
| **Project Code** | HomestayBooking |
| **Document Code** | HomestayBooking_Test Report_v1.0 |
| **Creator** | |
| **Reviewer/Approver** | |
| **Issue Date** | 2026-07-26 |
| **Notes** | Release 1 includes 2 modules: Auth_Workflow and Booking_Workflow |

Nguồn số liệu: `docs/test-cases/README.md` + sheets `Auth_Workflow` / `Booking_Workflow` (mỗi sheet 32 TC; Round còn Pending).

---

## TEST STATISTICS

| No | Module code | Passed | Failed | Pending | N/A | Number of test cases |
|---:|---|---:|---:|---:|---:|---:|
| 1 | Auth_Workflow | 0 | 0 | 32 | 0 | 32 |
| 2 | Booking_Workflow | 0 | 0 | 32 | 0 | 32 |
| | **Sub total** | **0** | **0** | **64** | **0** | **64** |
| | Test coverage | | **0.00%** | | | |
| | Test successful coverage | | **0.00%** | | | |

### Formulas

| Metric | Formula |
|---|---|
| Number of test cases | Passed + Failed + Pending + N/A |
| Test coverage | (Passed + Failed + N/A) / Total × 100 |
| Test successful coverage | Passed / Total × 100 |

### Correction log

| Item | Before (incorrect) | After |
|---|---|---|
| Booking_Workflow — Number of test cases | 38 | **32** |
| Sub total — Number of test cases | 70 | **64** |
| Sub total — Pending | 64 (ok) | **64** |
| Test coverage | 0.00% | **0.00%** (unchanged — chưa execute) |
| Test successful coverage | 0.00% | **0.00%** (unchanged) |

---

## Excel copy (TEST STATISTICS sheet)

Paste vào Excel theo cột:

```
No	Module code	Passed	Failed	Pending	N/A	Number of test cases
1	Auth_Workflow	0	0	32	0	32
2	Booking_Workflow	0	0	32	0	32
	Sub total	0	0	64	0	64
	Test coverage		0.00	%
	Test successful coverage		0.00	%
```

Khi chạy test: cập nhật Passed / Failed / Pending / N/A trên từng sheet workflow, rồi tính lại Sub total và 2 dòng coverage.
