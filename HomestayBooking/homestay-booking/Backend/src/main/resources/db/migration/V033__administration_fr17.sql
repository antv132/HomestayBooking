-- FR-17 Administration: complaints, system_settings, activity_logs + seed defaults
-- Idempotent for DBs that already have tables via Hibernate ddl-auto

-- complaints
IF OBJECT_ID('dbo.complaints', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.complaints (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        user_id UNIQUEIDENTIFIER NULL,
        subject NVARCHAR(200) NOT NULL,
        description NVARCHAR(MAX) NOT NULL,
        status NVARCHAR(20) NOT NULL CONSTRAINT DF_complaints_status DEFAULT 'OPEN',
        resolution_notes NVARCHAR(MAX) NULL,
        resolved_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL,
        CONSTRAINT FK_complaints_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_complaints_user_created' AND object_id = OBJECT_ID('dbo.complaints'))
    CREATE INDEX idx_complaints_user_created ON dbo.complaints (user_id, created_at DESC);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_complaints_status_created' AND object_id = OBJECT_ID('dbo.complaints'))
    CREATE INDEX idx_complaints_status_created ON dbo.complaints (status, created_at DESC);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_complaints_created' AND object_id = OBJECT_ID('dbo.complaints'))
    CREATE INDEX idx_complaints_created ON dbo.complaints (created_at DESC);
GO

-- system_settings
IF OBJECT_ID('dbo.system_settings', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.system_settings (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        setting_key NVARCHAR(100) NOT NULL,
        setting_value NVARCHAR(MAX) NULL,
        description NVARCHAR(500) NULL,
        updated_by UNIQUEIDENTIFIER NULL,
        updated_at DATETIME2 NOT NULL,
        CONSTRAINT UQ_system_settings_key UNIQUE (setting_key),
        CONSTRAINT FK_system_settings_updated_by FOREIGN KEY (updated_by) REFERENCES dbo.users(id)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_settings_key' AND object_id = OBJECT_ID('dbo.system_settings'))
    CREATE UNIQUE INDEX idx_settings_key ON dbo.system_settings (setting_key);
GO

-- activity_logs
IF OBJECT_ID('dbo.activity_logs', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.activity_logs (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        user_id UNIQUEIDENTIFIER NULL,
        action NVARCHAR(64) NOT NULL,
        entity_type NVARCHAR(64) NULL,
        entity_id UNIQUEIDENTIFIER NULL,
        details NVARCHAR(MAX) NULL,
        ip_address NVARCHAR(45) NULL,
        created_at DATETIME2 NOT NULL,
        CONSTRAINT FK_activity_logs_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
    );
END
GO

IF COL_LENGTH('dbo.activity_logs', 'ip_address') IS NULL
BEGIN
    ALTER TABLE dbo.activity_logs ADD ip_address NVARCHAR(45) NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_actlog_created' AND object_id = OBJECT_ID('dbo.activity_logs'))
    CREATE INDEX idx_actlog_created ON dbo.activity_logs (created_at DESC);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_actlog_action_created' AND object_id = OBJECT_ID('dbo.activity_logs'))
    CREATE INDEX idx_actlog_action_created ON dbo.activity_logs (action, created_at DESC);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_actlog_user_created' AND object_id = OBJECT_ID('dbo.activity_logs'))
    CREATE INDEX idx_actlog_user_created ON dbo.activity_logs (user_id, created_at DESC);
GO

-- Seed default settings (skip if key exists)
-- Dev smoke: use existing CUSTOMER/ADMIN users from FR-01 seed when present.
MERGE dbo.system_settings AS t
USING (VALUES
    (NEWID(), N'DEPOSIT_PERCENTAGE', N'40', N'Deposit percentage for new bookings (10-50)', SYSUTCDATETIME()),
    (NEWID(), N'SYSTEM_NAME', N'Homestay Booking', N'Display name of the platform', SYSUTCDATETIME()),
    (NEWID(), N'SUPPORT_EMAIL', N'support@example.com', N'Public support contact email', SYSUTCDATETIME()),
    (NEWID(), N'BANK_ACCOUNT_NUMBER', N'', N'Beneficiary bank account number', SYSUTCDATETIME()),
    (NEWID(), N'BANK_ACCOUNT_NAME', N'', N'Beneficiary account holder name', SYSUTCDATETIME()),
    (NEWID(), N'BANK_NAME', N'', N'Bank name', SYSUTCDATETIME()),
    (NEWID(), N'CANCEL_TIMEOUT_HOURS', N'24', N'Hours allowed to cancel after booking created (P2)', SYSUTCDATETIME())
) AS s (id, setting_key, setting_value, description, updated_at)
ON t.setting_key = s.setting_key
WHEN NOT MATCHED THEN
    INSERT (id, setting_key, setting_value, description, updated_by, updated_at)
    VALUES (s.id, s.setting_key, s.setting_value, s.description, NULL, s.updated_at);
GO
