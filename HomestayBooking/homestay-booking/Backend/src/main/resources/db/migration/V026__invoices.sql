-- FR-11 invoices (reference SQL; project uses JPA ddl-auto + SchemaColumnPatcher)
IF OBJECT_ID('dbo.invoices', 'U') IS NULL
BEGIN
    CREATE TABLE invoices (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        booking_id UNIQUEIDENTIFIER NOT NULL,
        customer_id UNIQUEIDENTIFIER NOT NULL,
        property_id UNIQUEIDENTIFIER NOT NULL,
        type NVARCHAR(20) NOT NULL,
        amount DECIMAL(15,2) NOT NULL,
        status NVARCHAR(20) NOT NULL CONSTRAINT DF_invoices_status DEFAULT 'UNPAID',
        due_date DATE NOT NULL,
        paid_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL,
        CONSTRAINT uk_invoices_booking_type UNIQUE (booking_id, type)
    );
    CREATE INDEX idx_invoices_customer_status ON invoices (customer_id, status, created_at);
    CREATE INDEX idx_invoices_property_status ON invoices (property_id, status, due_date);
    CREATE INDEX idx_invoices_booking ON invoices (booking_id);
END
GO
