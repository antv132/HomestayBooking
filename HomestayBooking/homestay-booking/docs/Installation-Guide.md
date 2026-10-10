# Hướng Dẫn Cài Đặt (Installation Guide)

Tài liệu này hướng dẫn chi tiết cách cài đặt và khởi chạy hệ thống Homestay Booking System trên môi trường phát triển nội bộ (Local Development).

## 1. Yêu Cầu Hệ Thống (Prerequisites)

Trước khi tiến hành cài đặt, máy tính của bạn cần được cài đặt sẵn các phần mềm sau:

- **Java 17**: Môi trường chạy cho Backend Spring Boot.
- **Node.js 20.x** & **npm 10+**: Môi trường chạy cho Frontend React. (Khuyến nghị dùng NVM).
- **Docker Desktop**: Dùng để chạy SQL Server Container.
- **Git**: Để clone repository.

> **Lưu ý:** Bạn không cần cài đặt Maven toàn cục trên máy, hệ thống sẽ sử dụng Maven Wrapper (`mvnw`) được đính kèm sẵn trong source code.

---

## 2. Chuẩn Bị Môi Trường

Clone repository về máy và di chuyển vào thư mục gốc của dự án:
```powershell
git clone <repository_url>
cd homestay-booking
```

### Thiết lập biến môi trường cho Backend
Từ thư mục gốc của repository, copy file template để tạo file `.env`:

```powershell
Copy-Item Backend/dev-environment.template Backend/.env
```

Mở file `Backend/.env` và thay đổi các giá trị `<required>`. 
**Đặc biệt lưu ý:** `DB_PASSWORD` phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt (chính sách bảo mật mặc định của SQL Server).

*(Lưu ý: Không bao giờ commit file `.env` lên Git).*

---

## 3. Cài Đặt Cơ Sở Dữ Liệu (SQL Server qua Docker)

Hệ thống sử dụng SQL Server 2022 Developer được đóng gói sẵn trong Docker.

Đảm bảo **Docker Desktop đang mở** và port `1433` trên máy bạn chưa bị chiếm dụng (nếu bạn có cài SQL Server trực tiếp trên Windows, hãy Stop service đó lại).

Chạy lệnh sau từ thư mục gốc để khởi chạy CSDL:
```powershell
docker compose --env-file Backend/.env -f Backend/compose.yml up -d
```

**Kiểm tra trạng thái:**
```powershell
docker compose --env-file Backend/.env -f Backend/compose.yml ps
```
*Hệ thống sẽ tự động tạo một volume `homestay_sql_data` để lưu trữ dữ liệu và khởi tạo database có tên `HomestayManagement`.*

---

## 4. Cài Đặt và Khởi Chạy Backend (Spring Boot)

Backend chạy độc lập trên máy host ở port `8080`.

1. Di chuyển vào thư mục Backend:
   ```powershell
   Set-Location Backend
   ```
2. Khởi chạy ứng dụng:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
*Backend sẽ tự động đọc cấu hình từ file `.env` và kết nối với SQL Server. Sau khi khởi động thành công, API sẽ khả dụng tại `http://localhost:8080`.*

---

## 5. Cài Đặt và Khởi Chạy Frontend (React + Vite)

Frontend chạy độc lập trên máy host. Hãy mở một cửa sổ Terminal mới.

1. Di chuyển vào thư mục Frontend:
   ```powershell
   Set-Location Frontend
   ```
2. Cài đặt các thư viện (sử dụng `npm ci` để đảm bảo đồng nhất phiên bản qua `package-lock.json`):
   ```powershell
   nvm use 20    # (Nếu bạn dùng NVM)
   npm ci
   ```
3. Khởi chạy server phát triển:
   ```powershell
   npm run dev
   ```
*Giao diện người dùng sẽ khả dụng tại địa chỉ được log trên terminal (thường là `http://localhost:5173`).*

---

## 6. Các Lệnh Quản Trị Hằng Ngày

Dưới đây là các lệnh Docker hữu ích để quản lý CSDL (chạy từ thư mục gốc):

- **Dừng CSDL (Giữ lại dữ liệu):**
  ```powershell
  docker compose --env-file Backend/.env -f Backend/compose.yml stop
  ```
- **Xóa container CSDL (Giữ lại dữ liệu):**
  ```powershell
  docker compose --env-file Backend/.env -f Backend/compose.yml down
  ```
- **Reset CSDL (Xóa hoàn toàn dữ liệu - Cẩn thận):**
  ```powershell
  docker compose --env-file Backend/.env -f Backend/compose.yml down -v
  ```

---

## 7. Khắc Phục Sự Cố Thường Gặp (Troubleshooting)

| Vấn đề | Nguyên nhân & Cách khắc phục |
|--------|------------------------------|
| **Port 1433 in use** | Port 1433 đã bị chiếm dụng. Hãy vào `services.msc` trên Windows và Stop dịch vụ `SQL Server (MSSQLSERVER)` hoặc đổi port trong file `compose.yml`. |
| **Login failed for user 'sa'** | Mật khẩu `DB_PASSWORD` trong file `.env` không khớp hoặc không đủ độ phức tạp. Hãy xóa container và volume (`down -v`), sửa lại `.env` và chạy `up -d` lại. |
| **Backend không kết nối được DB** | Đảm bảo biến `DB_URL` trong `.env` đang trỏ đúng về `localhost:1433` và tên DB là `HomestayManagement`. Kiểm tra container SQL Server đã thực sự "healthy" chưa. |
| **Lỗi khi `npm ci`** | Đảm bảo bạn đang dùng Node.js 20.x. Xóa thư mục `node_modules` và chạy lại `npm ci`. |
