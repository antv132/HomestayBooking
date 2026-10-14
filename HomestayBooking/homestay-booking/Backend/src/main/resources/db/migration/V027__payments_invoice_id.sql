-- FR-11 / FR-12: payments.invoice_id
IF COL_LENGTH('dbo.payments', 'invoice_id') IS NULL
BEGIN
    ALTER TABLE payments ADD invoice_id UNIQUEIDENTIFIER NULL;
END
GO
