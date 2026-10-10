# Package Descriptions — Frontend

**Project:** Homestay Booking System  
**Root:** `Frontend/src`  
**Stack:** React 18 · TypeScript · Vite · TanStack Query · Zustand · Axios · Tailwind  

| No | Package | Description |
|----|---------|-------------|
| 01 | Pages | Màn hình / route theo role (`pages/admin`, `customer`, `manager`, `employee`, `public`). Orchestrate UI: gọi Api, bọc Layout, dùng Components / Store / Hooks / Utils. Ví dụ: `AdminDashboardPage`, `BookingListPage`, `LandingPage` (~89 `*Page.tsx`). |
| 02 | Api | Client HTTP tới Backend REST. Mỗi module bọc một domain (`bookingApi`, `paymentApi`, `authApi`…); dùng `axiosInstance` + TanStack Query. Đây là **biên duy nhất** Frontend → `Backend.Controller`. (~31 modules). |
| 03 | Layouts | Khung shell theo role: nav, sidebar, notification bell. Bọc Pages; «use» Store (auth), Api (notification), Hooks (WebSocket). Ví dụ: `AdminLayout`, `ManagerLayout`, `CustomerLayout`, `EmployeeLayout`, `PublicLayout` (5 layouts). |
| 04 | Components | UI dùng chung (shadcn-style + Tailwind): `ui/` (RoomCard, Button, DataTable…), `rooms/`, `booking/`, route guards (`ProtectedRoute`, `GuestRoute`). Pages / App «use» Components; một số component «use» Api. |
| 05 | Store | Client state Zustand. Hiện một slice `authStore` (session / role). Server state giữ trong React Query cache — không nhét data list vào Store. |
| 06 | Hooks | Custom React hooks bọc logic phức tạp (API + state). Ví dụ: `useRoomSearch`, `useNotificationWebSocket`. «use» Api và Store; được Pages / Layouts gọi. |
| 07 | Utils | Hàm thuần (pure), không side-effect UI. Format / calendar / media / filter / deep-link. Visibility nội bộ (−). Ví dụ: `roomCalendar`, `mediaUrl`, `amenities`, `filterInput`, `notificationDeepLink`. |

---

### Quan hệ gói (tóm tắt)

```
Pages ──«use»──► Api ──«use»──► Backend.Controller «external»
  │                 ▲
  ├──«use»──► Layouts ──«use»──► Store · Api · Hooks
  ├──«use»──► Components ──«use»──► Api (một số)
  ├──«use»──► Store ──«use»──► Api
  ├──«use»──► Hooks ──«use»──► Api · Store
  └──«use»──► Utils
```

### Ghi chú đặt tên

| Package | Quy tắc |
|---------|---------|
| Pages | `*Page.tsx` · thư mục theo role |
| Api | `*Api.ts` · camelCase |
| Layouts | `*Layout.tsx` |
| Components | PascalCase (vd. `RoomCard.tsx`) |
| Store | `*Store.ts` |
| Hooks | `use*` |
| Utils | camelCase mô tả chức năng |

### Ngoài phạm vi package diagram nghiệp vụ

| Thư mục | Ghi chú |
|---------|--------|
| `assets/` | Tài nguyên tĩnh (ảnh, icon) — không phải logic package |
| `test/` | Setup / smoke test — không thuộc kiến trúc runtime |
