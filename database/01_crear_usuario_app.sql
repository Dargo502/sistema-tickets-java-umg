/* ============================================================
   Usuario de SQL Server exclusivo para el backend (en lugar de "sa").
   Ejecutar en SSMS DESPUÉS de control_tickets_db.sql.
   Cambie la contraseña por una propia y use LA MISMA en
   backend/BackendTickets/src/java/db.properties (db.password).
   ============================================================ */
USE master;
GO
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'tickets_app')
    CREATE LOGIN tickets_app WITH PASSWORD = 'Cambiar#Esta2026', CHECK_POLICY = ON, DEFAULT_DATABASE = DB_ControlTickets;
GO
USE DB_ControlTickets;
GO
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'tickets_app')
    CREATE USER tickets_app FOR LOGIN tickets_app;
GO
-- Lectura, escritura y ejecución de procedimientos (no puede borrar tablas ni crear objetos).
ALTER ROLE db_datareader ADD MEMBER tickets_app;
ALTER ROLE db_datawriter ADD MEMBER tickets_app;
GRANT EXECUTE TO tickets_app;
GO
PRINT 'Usuario tickets_app listo.';
