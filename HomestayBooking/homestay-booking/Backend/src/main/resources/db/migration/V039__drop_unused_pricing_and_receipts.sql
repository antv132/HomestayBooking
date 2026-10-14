-- Edu DB slim-down: drop deferred tables (PricingRule / PaymentReceipt TD-011).
-- Project primarily uses JPA ddl-auto + SchemaColumnPatcher; this is reference SQL.
-- Idempotent for SQL Server.

IF OBJECT_ID(N'dbo.payment_receipts', N'U') IS NOT NULL
    DROP TABLE dbo.payment_receipts;
GO

IF OBJECT_ID(N'dbo.pricing_rules', N'U') IS NOT NULL
    DROP TABLE dbo.pricing_rules;
GO
