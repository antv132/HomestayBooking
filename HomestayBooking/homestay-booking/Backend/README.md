# Homestay Backend

Yêu cầu: Java 17 và Docker Desktop.

```powershell
# Chạy từ thư mục gốc của repository
Copy-Item backend/dev-environment.template backend/.env
# Điền các giá trị <required> trong backend/.env

docker compose --env-file backend/.env -f backend/compose.yml up -d

Set-Location backend
.\mvnw.cmd spring-boot:run
```

Backend chạy tại `http://localhost:8080`.

Hướng dẫn đầy đủ cho cả backend và frontend:
[`docs/environment-setup.md`](../docs/environment-setup.md).