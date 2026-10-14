-- FR-22 Employee Dashboard: optional index for SCR-59 inspection KPI/preview
-- SQL Server + idempotent (Hibernate ddl-auto may already have related indexes)

IF OBJECT_ID('dbo.room_inspections', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1 FROM sys.indexes
       WHERE name = 'idx_room_inspections_employee_status'
         AND object_id = OBJECT_ID('dbo.room_inspections')
   )
BEGIN
    CREATE INDEX idx_room_inspections_employee_status
        ON dbo.room_inspections (inspected_by, status);
END
GO
