-- Reset demo data (HomestayManagement)
-- Chạy trong SQL Server Management Studio hoặc Azure Data Studio, rồi restart backend.

USE HomestayManagement;
GO

-- Xóa theo thứ tự phụ thuộc (bảng con trước)
IF OBJECT_ID('reviews', 'U') IS NOT NULL DELETE FROM reviews;
IF OBJECT_ID('bookings', 'U') IS NOT NULL DELETE FROM bookings;
IF OBJECT_ID('room_images', 'U') IS NOT NULL DELETE FROM room_images;
IF OBJECT_ID('rooms', 'U') IS NOT NULL DELETE FROM rooms;
IF OBJECT_ID('floors', 'U') IS NOT NULL DELETE FROM floors;
IF OBJECT_ID('properties', 'U') IS NOT NULL DELETE FROM properties;

-- (Tuỳ chọn) Xóa user demo
-- DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@homestay.demo' OR email LIKE '%@demo.com');
-- DELETE FROM users WHERE email LIKE '%@homestay.demo' OR email LIKE '%@demo.com';

PRINT 'Demo data cleared. Run scripts/fix-unicode.sql if Vietnamese text shows as ?, then restart HomestayApplication to re-seed.';
