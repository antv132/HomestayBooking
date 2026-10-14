-- Chuyển cột text sang NVARCHAR (Unicode) và sửa dữ liệu demo bị lỗi dấu (?)
-- Chạy trong SSMS / Azure Data Studio, sau đó restart backend.

USE HomestayManagement;
GO

IF OBJECT_ID('properties', 'U') IS NOT NULL
BEGIN
    ALTER TABLE properties ALTER COLUMN name NVARCHAR(200) NOT NULL;
    ALTER TABLE properties ALTER COLUMN address NVARCHAR(500) NOT NULL;
    ALTER TABLE properties ALTER COLUMN description NVARCHAR(MAX) NULL;
END
GO

IF OBJECT_ID('floors', 'U') IS NOT NULL
BEGIN
    ALTER TABLE floors ALTER COLUMN description NVARCHAR(500) NULL;
END
GO

IF OBJECT_ID('rooms', 'U') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM sys.objects WHERE name = 'uq_room_number_property' AND type = 'UQ')
        ALTER TABLE rooms DROP CONSTRAINT uq_room_number_property;

    ALTER TABLE rooms ALTER COLUMN room_number NVARCHAR(50) NOT NULL;
    ALTER TABLE rooms ALTER COLUMN room_type NVARCHAR(100) NULL;
    ALTER TABLE rooms ALTER COLUMN description NVARCHAR(MAX) NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'uq_room_number_property' AND type = 'UQ')
        ALTER TABLE rooms ADD CONSTRAINT uq_room_number_property UNIQUE (room_number, property_id);
END
GO

IF OBJECT_ID('users', 'U') IS NOT NULL
BEGIN
    ALTER TABLE users ALTER COLUMN full_name NVARCHAR(200) NOT NULL;
END
GO

-- Sửa dữ liệu demo (dùng N'' prefix cho Unicode literal)
UPDATE properties SET
    name = N'Hà Nội Old Quarter Inn',
    address = N'25 Hàng Bạc, Hà Nội',
    description = N'Homestay phố cổ Hà Nội, đi bộ tới Hồ Hoàn Kiếm.'
WHERE name LIKE N'%Old Quarter Inn%';
GO

UPDATE properties SET
    name = N'Sunset Resort Đà Nẵng',
    address = N'123 Nguyễn Tất Thành, Đà Nẵng'
WHERE name LIKE N'%Sunset Resort%';
GO

UPDATE properties SET
    name = N'Hội An Garden Villa',
    address = N'78 Phan Bội Châu, Hội An'
WHERE name LIKE N'%Garden Villa%' OR name LIKE N'%H?i An%';
GO

UPDATE properties SET
    name = N'Phú Quốc Beach House',
    address = N'12 Trần Hưng Đạo, Phú Quốc'
WHERE name LIKE N'%Ph%Qu%E%Beach House%';
GO

UPDATE properties SET
    address = N'456 Trần Phú, Đà Lạt'
WHERE name LIKE N'%Mountain View%';
GO

UPDATE properties SET
    address = N'88 Trần Phú, Nha Trang'
WHERE name LIKE N'%Nha Trang%';
GO

UPDATE users SET full_name = N'Nguyễn Văn An' WHERE email = 'an.nguyen@demo.com';
GO

IF OBJECT_ID('promotions', 'U') IS NOT NULL
BEGIN
    ALTER TABLE promotions ALTER COLUMN subtitle NVARCHAR(100) NOT NULL;
    ALTER TABLE promotions ALTER COLUMN title NVARCHAR(200) NOT NULL;
    ALTER TABLE promotions ALTER COLUMN description NVARCHAR(400) NULL;
    ALTER TABLE promotions ALTER COLUMN cta_text NVARCHAR(80) NOT NULL;
    ALTER TABLE promotions ALTER COLUMN cta_url NVARCHAR(300) NOT NULL;
    ALTER TABLE promotions ALTER COLUMN image_url NVARCHAR(500) NULL;
    ALTER TABLE promotions ALTER COLUMN color_theme NVARCHAR(20) NOT NULL;

    UPDATE promotions SET
        subtitle = N'Ưu đãi cuối tuần',
        title = N'Giảm 20%
thứ 6 – chủ nhật',
        description = N'Áp dụng cho phòng trống cuối tuần tại tất cả homestay.',
        cta_text = N'Đặt ngay →'
    WHERE sort_order = 0;

    UPDATE promotions SET
        subtitle = N'Đặt sớm hè 2026',
        title = N'Combo 3 đêm
+ bữa sáng miễn phí',
        description = N'Ưu đãi có hạn — đặt trước 31/08/2026.',
        cta_text = N'Khám phá →'
    WHERE sort_order = 1;

    UPDATE promotions SET
        subtitle = N'Lưu trú dài hạn',
        title = N'Giảm thêm 15%
cho booking từ 5 đêm',
        description = N'Lý tưởng cho kỳ nghỉ dài ngày hoặc công tác.',
        cta_text = N'Xem phòng →'
    WHERE sort_order = 2;
END
GO

UPDATE room_images SET image_url = REPLACE(image_url,
    'photo-1590071246406-4351809a5779', 'photo-1566073771259-6a8506099945')
WHERE image_url LIKE '%photo-1590071246406%';
GO

UPDATE room_images SET image_url = REPLACE(image_url,
    'photo-1512918728675-ed5a9ecdebfb', 'photo-1631049307264-da0ec9d70304')
WHERE image_url LIKE '%photo-1512918728675%';
GO

PRINT 'Unicode columns updated. Restart HomestayApplication.';
