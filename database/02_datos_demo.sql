/* ============================================================
   Datos de demostración (opcional) para presentar el sistema.
   Ejecutar en SSMS DESPUÉS de control_tickets_db.sql.
   Contraseña de todos los usuarios demo: Demo2026!
   ============================================================ */
USE DB_ControlTickets;
GO
SET NOCOUNT ON;

INSERT INTO Empresa (NombreEmpresa, Direccion, Telefono) VALUES
    ('Universidad Mariano Gálvez', '3a. Avenida 9-00 zona 2, Guatemala', '2411-1800'),
    ('Soporte Técnico Demo, S.A.', 'Zona 10, Guatemala', '2222-0000');

-- Roles del script base: 1 = Solicitante, 2 = Especialista, 3 = Administrador
INSERT INTO Usuario (IdEmpresa, IdRol, NombreCompleto, NombreUsuario, Contrasena, Correo) VALUES
    (1, 3, 'Administrador del Sistema', 'admin',      HASHBYTES('SHA2_256', 'Demo2026!'), 'admin@demo.local'),
    (1, 1, 'Ana López',                 'alopez',     HASHBYTES('SHA2_256', 'Demo2026!'), 'alopez@demo.local'),
    (1, 1, 'Carlos Pérez',              'cperez',     HASHBYTES('SHA2_256', 'Demo2026!'), 'cperez@demo.local'),
    (2, 2, 'María García',              'mgarcia',    HASHBYTES('SHA2_256', 'Demo2026!'), 'mgarcia@demo.local'),
    (2, 2, 'José Hernández',            'jhernandez', HASHBYTES('SHA2_256', 'Demo2026!'), 'jhernandez@demo.local');

-- Tickets en estado Solicitado (IdEstado = 1). El trigger asigna Folio, posición FIFO e historial.
-- Se insertan uno por uno para respetar el orden de llegada.
INSERT INTO Ticket (Titulo, Descripcion, IdCategoria, IdPrioridad, IdEstado, IdSolicitante, PosicionCola)
VALUES ('La impresora del laboratorio no imprime', 'Marca error de papel atascado aunque está vacía.', 1, 2, 1, 2, 0);
INSERT INTO Ticket (Titulo, Descripcion, IdCategoria, IdPrioridad, IdEstado, IdSolicitante, PosicionCola)
VALUES ('No abre NetBeans en la PC 12', 'Se cierra al cargar el splash.', 2, 1, 1, 3, 0);
INSERT INTO Ticket (Titulo, Descripcion, IdCategoria, IdPrioridad, IdEstado, IdSolicitante, PosicionCola)
VALUES ('Sin internet en el aula 204', 'El Wi-Fi conecta pero no navega.', 3, 1, 1, 2, 0);
INSERT INTO Ticket (Titulo, Descripcion, IdCategoria, IdPrioridad, IdEstado, IdSolicitante, PosicionCola)
VALUES ('Instalar SQL Server Management Studio', 'Para el curso de Bases de Datos.', 2, 3, 1, 3, 0);

-- Un ticket reclamado y en proceso, y otro finalizado, para que el tablero tenga las 3 columnas.
UPDATE Ticket SET IdEspecialista = 4 WHERE IdTicket = 2;
UPDATE Ticket SET IdEstado = 2      WHERE IdTicket = 2;

UPDATE Ticket SET IdEspecialista = 5 WHERE IdTicket = 3;
UPDATE Ticket SET IdEstado = 2      WHERE IdTicket = 3;
UPDATE Ticket SET IdEstado = 3      WHERE IdTicket = 3;
GO
PRINT 'Datos de demostración cargados.';
