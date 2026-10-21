/* ============================================================
   PROYECTO FINAL - PROGRAMACION II
   MODELO ASIGNADO: CONTROL DE TICKETS (Estructura FIFO / Colas)
   Motor: SQL Server
   Capa: Base de Datos (Capa 1 - Aislada, acceso solo vía Middleware SOAP)
   ============================================================ */

-- ============================================================
-- 0. CREACION DE BASE DE DATOS
-- ============================================================
IF DB_ID('DB_ControlTickets') IS NOT NULL
BEGIN
    ALTER DATABASE DB_ControlTickets SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE DB_ControlTickets;
END
GO

CREATE DATABASE DB_ControlTickets;
GO

USE DB_ControlTickets;
GO

-- ============================================================
-- 1. NUCLEO GENERICO (ADN de Datos requerido por la arquitectura)
--    Empresa -> Usuario -> Rol
-- ============================================================

CREATE TABLE Empresa (
    IdEmpresa       INT IDENTITY(1,1) PRIMARY KEY,
    NombreEmpresa   VARCHAR(150)  NOT NULL,
    Direccion       VARCHAR(200)  NULL,
    Telefono        VARCHAR(20)   NULL,
    Activo          BIT           NOT NULL DEFAULT 1,
    FechaRegistro   DATETIME      NOT NULL DEFAULT GETDATE()
);
GO

CREATE TABLE Rol (
    IdRol       INT IDENTITY(1,1) PRIMARY KEY,
    NombreRol   VARCHAR(50) NOT NULL UNIQUE
    -- Valores esperados: 'Solicitante', 'Especialista', 'Administrador'
);
GO

CREATE TABLE Usuario (
    IdUsuario       INT IDENTITY(1,1) PRIMARY KEY,
    IdEmpresa       INT NOT NULL,
    IdRol           INT NOT NULL,
    NombreCompleto  VARCHAR(150) NOT NULL,
    NombreUsuario   VARCHAR(50)  NOT NULL UNIQUE,
    Contrasena      VARBINARY(256) NOT NULL,   -- Hash (nunca texto plano)
    Correo          VARCHAR(150) NOT NULL UNIQUE,
    Activo          BIT NOT NULL DEFAULT 1,
    FechaCreacion   DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Usuario_Empresa FOREIGN KEY (IdEmpresa) REFERENCES Empresa(IdEmpresa),
    CONSTRAINT FK_Usuario_Rol     FOREIGN KEY (IdRol)     REFERENCES Rol(IdRol)
);
GO

-- ============================================================
-- 2. CATALOGOS DE ESTADO Y CLASIFICACION (FIFO)
-- ============================================================

CREATE TABLE EstadoTicket (
    IdEstado    INT IDENTITY(1,1) PRIMARY KEY,
    NombreEstado VARCHAR(30) NOT NULL UNIQUE,
    Orden        INT NOT NULL UNIQUE   -- 1=Solicitado, 2=En Proceso, 3=Finalizado
);
GO

CREATE TABLE PrioridadTicket (
    IdPrioridad     INT IDENTITY(1,1) PRIMARY KEY,
    NombrePrioridad VARCHAR(20) NOT NULL UNIQUE,   -- Alta, Media, Baja
    NivelPeso       INT NOT NULL UNIQUE            -- 1=Alta ... usado para desempate en la cola
);
GO

CREATE TABLE CategoriaTicket (
    IdCategoria        INT IDENTITY(1,1) PRIMARY KEY,
    NombreCategoria    VARCHAR(60) NOT NULL UNIQUE,   -- Hardware, Software, Red, Citas, etc.
    Prefijo            VARCHAR(5)  NOT NULL UNIQUE,   -- HD, SW, NW...
    UltimoConsecutivo  INT NOT NULL DEFAULT 0         -- usado por el trigger de folio
);
GO

-- ============================================================
-- 3. TABLA TRANSACCIONAL PRINCIPAL: TICKET (Cola FIFO)
-- ============================================================

CREATE TABLE Ticket (
    IdTicket         INT IDENTITY(1,1) PRIMARY KEY,
    Folio            VARCHAR(15)  NULL UNIQUE,     -- Generado por trigger, ej: HD-042
    Titulo           VARCHAR(150) NOT NULL,
    Descripcion      VARCHAR(1000) NULL,
    IdCategoria      INT NOT NULL,
    IdPrioridad      INT NOT NULL,
    IdEstado         INT NOT NULL,
    IdSolicitante    INT NOT NULL,                 -- Usuario que reporta
    IdEspecialista   INT NULL,                     -- Usuario que reclama (NULL = en cola)
    PosicionCola     INT NOT NULL,                 -- Orden estricto de llegada (FIFO)
    FechaCreacion    DATETIME NOT NULL DEFAULT GETDATE(),
    FechaAsignacion  DATETIME NULL,
    FechaFinalizacion DATETIME NULL,
    Eliminado        BIT NOT NULL DEFAULT 0,        -- Borrado logico
    CONSTRAINT FK_Ticket_Categoria     FOREIGN KEY (IdCategoria)   REFERENCES CategoriaTicket(IdCategoria),
    CONSTRAINT FK_Ticket_Prioridad     FOREIGN KEY (IdPrioridad)   REFERENCES PrioridadTicket(IdPrioridad),
    CONSTRAINT FK_Ticket_Estado        FOREIGN KEY (IdEstado)      REFERENCES EstadoTicket(IdEstado),
    CONSTRAINT FK_Ticket_Solicitante   FOREIGN KEY (IdSolicitante) REFERENCES Usuario(IdUsuario),
    CONSTRAINT FK_Ticket_Especialista  FOREIGN KEY (IdEspecialista) REFERENCES Usuario(IdUsuario)
);
GO

CREATE INDEX IX_Ticket_Estado_Posicion ON Ticket(IdEstado, PosicionCola);
CREATE INDEX IX_Ticket_Especialista ON Ticket(IdEspecialista);
GO

-- ============================================================
-- 4. TRAZABILIDAD (Auditoria requerida por IReports)
-- ============================================================

CREATE TABLE HistorialTicket (
    IdHistorial      INT IDENTITY(1,1) PRIMARY KEY,
    IdTicket         INT NOT NULL,
    IdEstadoAnterior INT NULL,
    IdEstadoNuevo    INT NOT NULL,
    IdUsuarioAccion  INT NOT NULL,       -- quien ejecuto el cambio
    Comentario       VARCHAR(300) NULL,
    FechaCambio      DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Historial_Ticket   FOREIGN KEY (IdTicket) REFERENCES Ticket(IdTicket),
    CONSTRAINT FK_Historial_EstAnt   FOREIGN KEY (IdEstadoAnterior) REFERENCES EstadoTicket(IdEstado),
    CONSTRAINT FK_Historial_EstNue   FOREIGN KEY (IdEstadoNuevo) REFERENCES EstadoTicket(IdEstado),
    CONSTRAINT FK_Historial_Usuario  FOREIGN KEY (IdUsuarioAccion) REFERENCES Usuario(IdUsuario)
);
GO

-- ============================================================
-- 5. NOTIFICACIONES (Alertas no intrusivas al reclamar un ticket)
-- ============================================================

CREATE TABLE NotificacionTicket (
    IdNotificacion  INT IDENTITY(1,1) PRIMARY KEY,
    IdTicket        INT NOT NULL,
    IdEspecialista  INT NOT NULL,
    Mensaje         VARCHAR(200) NOT NULL,
    Leida           BIT NOT NULL DEFAULT 0,
    FechaNotificacion DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Notif_Ticket FOREIGN KEY (IdTicket) REFERENCES Ticket(IdTicket),
    CONSTRAINT FK_Notif_Especialista FOREIGN KEY (IdEspecialista) REFERENCES Usuario(IdUsuario)
);
GO

-- ============================================================
-- 6. DATOS INICIALES DE CATALOGO
-- ============================================================

INSERT INTO Rol (NombreRol) VALUES ('Solicitante'), ('Especialista'), ('Administrador');

INSERT INTO EstadoTicket (NombreEstado, Orden) VALUES
    ('Solicitado', 1),
    ('En Proceso', 2),
    ('Finalizado', 3);

INSERT INTO PrioridadTicket (NombrePrioridad, NivelPeso) VALUES
    ('Alta', 1), ('Media', 2), ('Baja', 3);

INSERT INTO CategoriaTicket (NombreCategoria, Prefijo) VALUES
    ('Hardware', 'HD'),
    ('Software', 'SW'),
    ('Redes',    'NW');
GO

-- ============================================================
-- 7. TRIGGERS
-- ============================================================

-- 7.1 Al insertar un ticket: generar Folio consecutivo por categoria
--     y calcular PosicionCola (estrictamente por orden de llegada).
CREATE TRIGGER TR_Ticket_Insertar
ON Ticket
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    -- Actualiza el consecutivo de la categoria de forma atomica
    UPDATE C
        SET C.UltimoConsecutivo = C.UltimoConsecutivo + 1
    FROM CategoriaTicket C
    INNER JOIN INSERTED I ON I.IdCategoria = C.IdCategoria;

    -- Asigna Folio y PosicionCola (basada en el correlativo global de la tabla)
    UPDATE T
        SET T.Folio = C.Prefijo + '-' + RIGHT('000' + CAST(C.UltimoConsecutivo AS VARCHAR(10)), 3),
            T.PosicionCola = T.IdTicket
    FROM Ticket T
    INNER JOIN INSERTED I ON I.IdTicket = T.IdTicket
    INNER JOIN CategoriaTicket C ON C.IdCategoria = I.IdCategoria;

    -- Registra el estado inicial en el historial
    INSERT INTO HistorialTicket (IdTicket, IdEstadoAnterior, IdEstadoNuevo, IdUsuarioAccion, Comentario)
    SELECT I.IdTicket, NULL, I.IdEstado, I.IdSolicitante, 'Ticket creado e ingresado a la cola FIFO'
    FROM INSERTED I;
END;
GO

-- 7.2 Al cambiar de estado: valida transicion secuencial estricta
--     (Solicitado -> En Proceso -> Finalizado, sin saltos ni retrocesos)
--     y registra el cambio en el historial.
CREATE TRIGGER TR_Ticket_ValidarEstado
ON Ticket
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF UPDATE(IdEstado)
    BEGIN
        -- Rechaza transiciones invalidas (retroceso o salto de mas de 1 paso)
        IF EXISTS (
            SELECT 1
            FROM INSERTED I
            INNER JOIN DELETED D ON D.IdTicket = I.IdTicket
            INNER JOIN EstadoTicket EOld ON EOld.IdEstado = D.IdEstado
            INNER JOIN EstadoTicket ENew ON ENew.IdEstado = I.IdEstado
            WHERE ENew.Orden <> EOld.Orden + 1
        )
        BEGIN
            RAISERROR('Transicion de estado invalida: solo se permite avanzar un paso (Solicitado -> En Proceso -> Finalizado).', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END

        INSERT INTO HistorialTicket (IdTicket, IdEstadoAnterior, IdEstadoNuevo, IdUsuarioAccion, Comentario)
        SELECT I.IdTicket, D.IdEstado, I.IdEstado,
               ISNULL(I.IdEspecialista, I.IdSolicitante),
               'Cambio de estado de ticket'
        FROM INSERTED I
        INNER JOIN DELETED D ON D.IdTicket = I.IdTicket;

        -- Si paso a Finalizado, registra fecha de cierre
        UPDATE T
            SET T.FechaFinalizacion = GETDATE()
        FROM Ticket T
        INNER JOIN INSERTED I ON I.IdTicket = T.IdTicket
        INNER JOIN EstadoTicket E ON E.IdEstado = I.IdEstado
        WHERE E.NombreEstado = 'Finalizado';
    END
END;
GO

-- 7.3 Al reclamar un ticket (IdEspecialista pasa de NULL a un valor):
--     evita doble reclamo concurrente y genera la notificacion no intrusiva.
CREATE TRIGGER TR_Ticket_Reclamo
ON Ticket
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF UPDATE(IdEspecialista)
    BEGIN
        -- Bloquea el reclamo si el ticket ya tenia un especialista asignado
        IF EXISTS (
            SELECT 1
            FROM INSERTED I
            INNER JOIN DELETED D ON D.IdTicket = I.IdTicket
            WHERE D.IdEspecialista IS NOT NULL
              AND I.IdEspecialista IS NOT NULL
              AND D.IdEspecialista <> I.IdEspecialista
        )
        BEGIN
            RAISERROR('Este ticket ya fue reclamado por otro especialista.', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END

        UPDATE T SET T.FechaAsignacion = GETDATE()
        FROM Ticket T
        INNER JOIN INSERTED I ON I.IdTicket = T.IdTicket
        INNER JOIN DELETED D ON D.IdTicket = I.IdTicket
        WHERE D.IdEspecialista IS NULL AND I.IdEspecialista IS NOT NULL;

        INSERT INTO NotificacionTicket (IdTicket, IdEspecialista, Mensaje)
        SELECT I.IdTicket, I.IdEspecialista,
               'Incidente ' + I.Folio + ' reclamado por especialista'
        FROM INSERTED I
        INNER JOIN DELETED D ON D.IdTicket = I.IdTicket
        WHERE D.IdEspecialista IS NULL AND I.IdEspecialista IS NOT NULL;
    END
END;
GO

-- ============================================================
-- 8. PROCEDIMIENTOS ALMACENADOS (CRUD)
--    Estos son los unicos puntos de entrada a la BD.
--    El Middleware SOAP (JAX-WS) los invoca via JDBC; el Servlet/JSP
--    (Capa 3) nunca ejecuta SQL directamente (PROHIBITED_ACTION: DIRECT_JDBC_FROM_UI).
-- ============================================================

-- 8.1 CREAR ticket
CREATE PROCEDURE sp_Ticket_Insertar
    @Titulo         VARCHAR(150),
    @Descripcion    VARCHAR(1000),
    @IdCategoria    INT,
    @IdPrioridad    INT,
    @IdSolicitante  INT
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @IdEstadoInicial INT = (SELECT IdEstado FROM EstadoTicket WHERE NombreEstado = 'Solicitado');

    INSERT INTO Ticket (Titulo, Descripcion, IdCategoria, IdPrioridad, IdEstado, IdSolicitante, PosicionCola)
    VALUES (@Titulo, @Descripcion, @IdCategoria, @IdPrioridad, @IdEstadoInicial, @IdSolicitante, 0);

    SELECT * FROM Ticket WHERE IdTicket = SCOPE_IDENTITY();
END;
GO

-- 8.2 CONSULTAR ticket por Id
CREATE PROCEDURE sp_Ticket_ConsultarPorId
    @IdTicket INT
AS
BEGIN
    SET NOCOUNT ON;
    SELECT T.*, E.NombreEstado, P.NombrePrioridad, C.NombreCategoria,
           US.NombreCompleto AS Solicitante, UE.NombreCompleto AS Especialista
    FROM Ticket T
    INNER JOIN EstadoTicket E ON E.IdEstado = T.IdEstado
    INNER JOIN PrioridadTicket P ON P.IdPrioridad = T.IdPrioridad
    INNER JOIN CategoriaTicket C ON C.IdCategoria = T.IdCategoria
    INNER JOIN Usuario US ON US.IdUsuario = T.IdSolicitante
    LEFT JOIN Usuario UE ON UE.IdUsuario = T.IdEspecialista
    WHERE T.IdTicket = @IdTicket AND T.Eliminado = 0;
END;
GO

-- 8.3 LISTAR tickets agrupados por estado (para tablero Kanban),
--     respetando estrictamente el orden FIFO de llegada.
CREATE PROCEDURE sp_Ticket_ListarPorEstado
    @NombreEstado VARCHAR(30) = NULL   -- NULL = todos
AS
BEGIN
    SET NOCOUNT ON;
    SELECT T.IdTicket, T.Folio, T.Titulo, T.Descripcion, T.PosicionCola,
           E.NombreEstado, P.NombrePrioridad, C.NombreCategoria,
           US.NombreCompleto AS Solicitante, UE.NombreCompleto AS Especialista,
           T.FechaCreacion
    FROM Ticket T
    INNER JOIN EstadoTicket E ON E.IdEstado = T.IdEstado
    INNER JOIN PrioridadTicket P ON P.IdPrioridad = T.IdPrioridad
    INNER JOIN CategoriaTicket C ON C.IdCategoria = T.IdCategoria
    INNER JOIN Usuario US ON US.IdUsuario = T.IdSolicitante
    LEFT JOIN Usuario UE ON UE.IdUsuario = T.IdEspecialista
    WHERE T.Eliminado = 0
      AND (@NombreEstado IS NULL OR E.NombreEstado = @NombreEstado)
    ORDER BY P.NivelPeso ASC, T.PosicionCola ASC;
END;
GO

-- 8.4 ACTUALIZAR datos generales del ticket (titulo, descripcion, prioridad, categoria)
CREATE PROCEDURE sp_Ticket_Actualizar
    @IdTicket       INT,
    @Titulo         VARCHAR(150),
    @Descripcion    VARCHAR(1000),
    @IdCategoria    INT,
    @IdPrioridad    INT
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Ticket
        SET Titulo = @Titulo,
            Descripcion = @Descripcion,
            IdCategoria = @IdCategoria,
            IdPrioridad = @IdPrioridad
    WHERE IdTicket = @IdTicket AND Eliminado = 0;
END;
GO

-- 8.5 RECLAMAR ticket (asignacion de especialista) - protegido contra condicion de carrera
CREATE PROCEDURE sp_Ticket_Reclamar
    @IdTicket       INT,
    @IdEspecialista INT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;

    DECLARE @EspecialistaActual INT;
    SELECT @EspecialistaActual = IdEspecialista
    FROM Ticket WITH (UPDLOCK, ROWLOCK)
    WHERE IdTicket = @IdTicket;

    IF @EspecialistaActual IS NOT NULL
    BEGIN
        ROLLBACK TRANSACTION;
        RAISERROR('El ticket ya fue reclamado previamente.', 16, 1);
        RETURN;
    END

    UPDATE Ticket
        SET IdEspecialista = @IdEspecialista
    WHERE IdTicket = @IdTicket;

    COMMIT TRANSACTION;
END;
GO

-- 8.6 CAMBIAR ESTADO del ticket (avance en la cola)
CREATE PROCEDURE sp_Ticket_CambiarEstado
    @IdTicket       INT,
    @NombreEstado   VARCHAR(30),
    @IdUsuarioAccion INT
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @IdEstadoNuevo INT = (SELECT IdEstado FROM EstadoTicket WHERE NombreEstado = @NombreEstado);

    UPDATE Ticket
        SET IdEstado = @IdEstadoNuevo
    WHERE IdTicket = @IdTicket;
END;
GO

-- 8.7 ELIMINAR (logico) ticket
CREATE PROCEDURE sp_Ticket_Eliminar
    @IdTicket INT
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Ticket SET Eliminado = 1 WHERE IdTicket = @IdTicket;
END;
GO

-- 8.8 CRUD basico de Usuario (soporte para Solicitante/Especialista)
CREATE PROCEDURE sp_Usuario_Insertar
    @IdEmpresa      INT,
    @IdRol          INT,
    @NombreCompleto VARCHAR(150),
    @NombreUsuario  VARCHAR(50),
    @Contrasena     VARBINARY(256),
    @Correo         VARCHAR(150)
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO Usuario (IdEmpresa, IdRol, NombreCompleto, NombreUsuario, Contrasena, Correo)
    VALUES (@IdEmpresa, @IdRol, @NombreCompleto, @NombreUsuario, @Contrasena, @Correo);
    SELECT * FROM Usuario WHERE IdUsuario = SCOPE_IDENTITY();
END;
GO

CREATE PROCEDURE sp_Usuario_ConsultarPorId
    @IdUsuario INT
AS
BEGIN
    SET NOCOUNT ON;
    SELECT U.*, R.NombreRol, E.NombreEmpresa
    FROM Usuario U
    INNER JOIN Rol R ON R.IdRol = U.IdRol
    INNER JOIN Empresa E ON E.IdEmpresa = U.IdEmpresa
    WHERE U.IdUsuario = @IdUsuario;
END;
GO

CREATE PROCEDURE sp_Usuario_Actualizar
    @IdUsuario      INT,
    @NombreCompleto VARCHAR(150),
    @Correo         VARCHAR(150),
    @Activo         BIT
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Usuario
        SET NombreCompleto = @NombreCompleto,
            Correo = @Correo,
            Activo = @Activo
    WHERE IdUsuario = @IdUsuario;
END;
GO

CREATE PROCEDURE sp_Usuario_Eliminar
    @IdUsuario INT
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Usuario SET Activo = 0 WHERE IdUsuario = @IdUsuario;
END;
GO

-- 8.9 Reportes de soporte para IReports / Jasper Reports
CREATE PROCEDURE sp_Reporte_HistorialTicket
    @FechaInicio DATE,
    @FechaFin    DATE
AS
BEGIN
    SET NOCOUNT ON;
    SELECT H.IdHistorial, T.Folio, EA.NombreEstado AS EstadoAnterior,
           EN.NombreEstado AS EstadoNuevo, U.NombreCompleto AS UsuarioAccion,
           H.Comentario, H.FechaCambio
    FROM HistorialTicket H
    INNER JOIN Ticket T ON T.IdTicket = H.IdTicket
    LEFT JOIN EstadoTicket EA ON EA.IdEstado = H.IdEstadoAnterior
    INNER JOIN EstadoTicket EN ON EN.IdEstado = H.IdEstadoNuevo
    INNER JOIN Usuario U ON U.IdUsuario = H.IdUsuarioAccion
    WHERE CAST(H.FechaCambio AS DATE) BETWEEN @FechaInicio AND @FechaFin
    ORDER BY H.FechaCambio ASC;
END;
GO

PRINT 'Base de datos DB_ControlTickets creada correctamente: tablas, catalogos, triggers y procedimientos CRUD listos para consumo via JAX-WS.';
