# Đồng nhất môi trường phát triển

## Phiên bản chuẩn

- Java 17
- Maven 3.9.9 (được khóa bằng Maven Wrapper)
- Node.js 20.x
- npm 10 trở lên
- SQL Server 2022 Developer chạy bằng Docker

Không cần cài Maven toàn cục. Frontend dùng `package-lock.json` và phải cài
dependency bằng `npm ci`.

## Workspaces (canonical)

| Vai trò | Thư mục |
|---------|---------|
| Backend (Spring Boot) | `Backend/` |
| Frontend (React + Vite) | `Frontend/` |

Không dùng thư mục root tên `backend/` hay `frontend/` (đã gỡ). Source canonical là hai workspace ở bảng trên.

## Thiết lập lần đầu trên Windows

### 1. Chuẩn bị biến môi trường

Từ thư mục gốc của repository:

```powershell
Copy-Item Backend/dev-environment.template Backend/.env
```

Mở `Backend/.env` và thay toàn bộ giá trị `<required>`. `DB_PASSWORD` phải có
ít nhất 8 ký tự và gồm chữ hoa, chữ thường, số và ký tự đặc biệt để đáp ứng
chính sách mật khẩu của SQL Server.

Không commit file `.env`.

### 2. Khởi động SQL Server

Đảm bảo Docker Desktop đang chạy, sau đó:

```powershell
docker compose --env-file Backend/.env -f Backend/compose.yml up -d
docker compose --env-file Backend/.env -f Backend/compose.yml ps
```

Compose tạo SQL Server 2022 trên `localhost:1433`, volume
`homestay_sql_data`, và database `HomestayManagement`.

Nếu port `1433` đã được SQL Server cài trực tiếp sử dụng, hãy dừng dịch vụ đó
trước khi chạy Compose. Không chạy đồng thời hai SQL Server trên cùng port.

### 3. Khởi động backend

```powershell
Set-Location Backend
.\mvnw.cmd spring-boot:run
```

Backend tự đọc `.env` trong thư mục này và chạy tại `http://localhost:8080`.

### 4. Khởi động frontend

Mở terminal khác:

```powershell
Set-Location Frontend
nvm use
npm ci
npm run dev
```

Nếu không dùng NVM, hãy cài Node.js 20.x trước khi chạy `npm ci`.

## Lệnh hằng ngày

Khởi động database:

```powershell
docker compose --env-file Backend/.env -f Backend/compose.yml up -d
```

Dừng database nhưng giữ dữ liệu:

```powershell
docker compose --env-file Backend/.env -f Backend/compose.yml stop
```

Dừng và xóa container nhưng giữ volume:

```powershell
docker compose --env-file Backend/.env -f Backend/compose.yml down
```

Chỉ xóa cả dữ liệu khi thực sự muốn tạo database mới:

```powershell
docker compose --env-file Backend/.env -f Backend/compose.yml down -v
```

## Kiểm tra trước khi gửi code

```powershell
Set-Location Backend
.\mvnw.cmd test

Set-Location ../Frontend
npm ci
npm run lint
npm run test:run
npm run build
```

CI sử dụng cùng Java 17 và Node.js 20.
