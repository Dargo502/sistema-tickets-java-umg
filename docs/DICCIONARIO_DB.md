# Diccionario de datos — DB_ControlTickets (SQL Server)

Generado a partir de `database/control_tickets_db.sql`. Descripciones de negocio tomadas de los comentarios del script.


## Empresa

Empresas a las que pertenecen los usuarios.

| Columna | Tipo | Nulo | Clave / restricción | Valor por defecto | Comentario |
|---|---|---|---|---|---|
| IdEmpresa | INT | No | PK (IDENTITY) |  |  |
| NombreEmpresa | VARCHAR(150) | No |  |  |  |
| Direccion | VARCHAR(200) | Sí |  |  |  |
| Telefono | VARCHAR(20) | Sí |  |  |  |
| Activo | BIT | No |  | 1 |  |
| FechaRegistro | DATETIME | No |  | GETDATE() |  |

## Rol

Roles del sistema (Solicitante, Especialista, Administrador).

| Columna | Tipo | Nulo | Clave / restricción | Valor por defecto | Comentario |
|---|---|---|---|---|---|
| IdRol | INT | No | PK (IDENTITY) |  |  |
| NombreRol | VARCHAR(50) | No | UNIQUE |  |  |

## Usuario

Personas que usan el sistema. La contraseña se guarda en VARBINARY.

| Columna | Tipo | Nulo | Clave / restricción | Valor por defecto | Comentario |
|---|---|---|---|---|---|
| IdUsuario | INT | No | PK (IDENTITY) |  |  |
| IdEmpresa | INT | No | FK → Empresa.IdEmpresa |  |  |
| IdRol | INT | No |  |  |  |
| NombreCompleto | VARCHAR(150) | No |  |  |  |
| NombreUsuario | VARCHAR(50) | No | UNIQUE |  |  |
| Contrasena | VARBINARY(256) | No |  |  | Hash (nunca texto plano) |
| Correo | VARCHAR(150) | No | UNIQUE |  |  |
| Activo | BIT | No |  | 1 |  |
| FechaCreacion | DATETIME | No |  | GETDATE() |  |

## EstadoTicket

Catálogo de estados de la cola: 1=Solicitado, 2=En Proceso, 3=Finalizado.

| Columna | Tipo | Nulo | Clave / restricción | Valor por defecto | Comentario |
|---|---|---|---|---|---|
| IdEstado | INT | No | PK (IDENTITY) |  |  |
| NombreEstado | VARCHAR(30) | No | UNIQUE |  |  |
| Orden | INT | No | UNIQUE |  | 1=Solicitado, 2=En Proceso, 3=Finalizado |

## PrioridadTicket

Catálogo de prioridades (Alta, Media, Baja); NivelPeso sirve para desempate.

| Columna | Tipo | Nulo | Clave / restricción | Valor por defecto | Comentario |
|---|---|---|---|---|---|
| IdPrioridad | INT | No | PK (IDENTITY) |  |  |
| NombrePrioridad | VARCHAR(20) | No | UNIQUE |  | Alta, Media, Baja |
| NivelPeso | INT | No | UNIQUE |  | 1=Alta ... usado para desempate en la cola |

## CategoriaTicket

Catálogo de categorías; el prefijo y el consecutivo generan el folio (ej. HD-001).

| Columna | Tipo | Nulo | Clave / restricción | Valor por defecto | Comentario |
|---|---|---|---|---|---|
| IdCategoria | INT | No | PK (IDENTITY) |  |  |
| NombreCategoria | VARCHAR(60) | No | UNIQUE |  | Hardware, Software, Red, Citas, etc. |
| Prefijo | VARCHAR(5) | No | UNIQUE |  | HD, SW, NW... |
| UltimoConsecutivo | INT | No |  | 0 | usado por el trigger de folio |

## Ticket

Tabla principal: cada fila es un elemento de la cola FIFO. Borrado lógico con Eliminado.

| Columna | Tipo | Nulo | Clave / restricción | Valor por defecto | Comentario |
|---|---|---|---|---|---|
| IdTicket | INT | No | PK (IDENTITY) |  |  |
| Folio | VARCHAR(15) | Sí | UNIQUE |  | Generado por trigger, ej: HD-042 |
| Titulo | VARCHAR(150) | No |  |  |  |
| Descripcion | VARCHAR(1000) | Sí |  |  |  |
| IdCategoria | INT | No |  |  |  |
| IdPrioridad | INT | No |  |  |  |
| IdEstado | INT | No |  |  |  |
| IdSolicitante | INT | No |  |  | Usuario que reporta |
| IdEspecialista | INT | Sí |  |  | Usuario que reclama (NULL = en cola) |
| PosicionCola | INT | No |  |  | Orden estricto de llegada (FIFO) |
| FechaCreacion | DATETIME | No |  | GETDATE() |  |
| FechaAsignacion | DATETIME | Sí |  |  |  |
| FechaFinalizacion | DATETIME | Sí |  |  |  |
| Eliminado | BIT | No |  | 0 | Borrado logico |

## HistorialTicket

Auditoría: cada cambio de estado de un ticket (la llenan los triggers).

| Columna | Tipo | Nulo | Clave / restricción | Valor por defecto | Comentario |
|---|---|---|---|---|---|
| IdHistorial | INT | No | PK (IDENTITY) |  |  |
| IdTicket | INT | No |  |  |  |
| IdEstadoAnterior | INT | Sí |  |  |  |
| IdEstadoNuevo | INT | No |  |  |  |
| IdUsuarioAccion | INT | No |  |  | quien ejecuto el cambio |
| Comentario | VARCHAR(300) | Sí |  |  |  |
| FechaCambio | DATETIME | No |  | GETDATE() |  |

## NotificacionTicket

Alertas generadas al reclamar un ticket (las crea un trigger).

| Columna | Tipo | Nulo | Clave / restricción | Valor por defecto | Comentario |
|---|---|---|---|---|---|
| IdNotificacion | INT | No | PK (IDENTITY) |  |  |
| IdTicket | INT | No | FK → Ticket.IdTicket |  |  |
| IdEspecialista | INT | No | FK → Usuario.IdUsuario |  |  |
| Mensaje | VARCHAR(200) | No |  |  |  |
| Leida | BIT | No |  | 0 |  |
| FechaNotificacion | DATETIME | No |  | GETDATE() |  |

## Triggers

| Trigger | Tabla | Evento | Función |
|---|---|---|---|
| TR_Ticket_Insertar | Ticket | AFTER INSERT | Genera el folio por categoría, fija PosicionCola (orden de llegada FIFO) y registra el estado inicial en HistorialTicket. |
| TR_Ticket_ValidarEstado | Ticket | AFTER UPDATE | Solo permite avanzar un estado a la vez (Solicitado → En Proceso → Finalizado); registra el cambio y la fecha de finalización. |
| TR_Ticket_Reclamo | Ticket | AFTER UPDATE | Impide que otro especialista reclame un ticket ya asignado, registra FechaAsignacion y crea la notificación. |

## Procedimientos almacenados

`sp_Ticket_Insertar`, `sp_Ticket_ConsultarPorId`, `sp_Ticket_ListarPorEstado`, `sp_Ticket_Actualizar`, `sp_Ticket_Reclamar`, `sp_Ticket_CambiarEstado`, `sp_Ticket_Eliminar`, `sp_Usuario_Insertar`, `sp_Usuario_ConsultarPorId`, `sp_Usuario_Actualizar`, `sp_Usuario_Eliminar`, `sp_Reporte_HistorialTicket`.

