# Manual Técnico — Control de Tickets

## 1. Modelo de negocio
Sistema de **Control de Tickets / Help Desk** basado en una **cola FIFO**: prioridad estricta de llegada (`PosicionCola`) y estados mutables Solicitado → En Proceso → Finalizado. La base de datos garantiza las reglas con triggers.

## 2. Arquitectura (3 capas, 3 servidores)
```
Navegador móvil → FrontendTickets (Servlet/JSP) → BackendTickets (JAX-WS/SOAP) → SQL Server
                       Capa 3: presentación          Capa 2: servicios            Capa 1: datos
```
La capa web **nunca** usa JDBC: solo consume los servicios SOAP.

## 3. Base de datos
9 tablas, 3 triggers y 12 procedimientos almacenados. Ver `DICCIONARIO_DB.md` y el script `database/control_tickets_db.sql`.
Triggers: `TR_Ticket_Insertar` (folio + posición FIFO + historial), `TR_Ticket_ValidarEstado` (transición secuencial), `TR_Ticket_Reclamo` (sin doble reclamo + notificación).

## 4. Servicios SOAP (BackendTickets)
Un `@WebService` por tabla/área, namespace `http://servicios/`, publicados en `/{BackendTickets}/{WSNombre}`. Usan `PreparedStatement` (previene inyección SQL) y devuelven texto `Clave: valor | Clave: valor`. Los errores se informan como texto que empieza con `Error`.

| Servicio | Operaciones |
|---|---|
| WSEmpresas | insertar, actualizar, desactivar, seleccionarEmpresas, seleccionarEmpresaPorId |
| WSRoles | insertar, actualizar, eliminar, seleccionarTodos, seleccionarPorId |
| WSUsuarios | insertar, actualizar, desactivar, seleccionarUsuarios, seleccionarUsuarioPorId |
| WSCategorias / WSEstados / WSPrioridades | insertar, actualizar, eliminar, seleccionar (todos) |
| WSTickets | insertar, reclamar, cambiarEstado, eliminar (lógico), seleccionarTickets (+ parche: actualizar y por ID) |
| WSHistorial | seleccionarHistorialPorTicket |
| WSNotificaciones | seleccionarNotificacionesPendientes, marcarNotificacionLeida |

## 5. Aplicación web (FrontendTickets) — patrón MVC
| Capa | Paquete / carpeta | Responsabilidad |
|---|---|---|
| Modelo | `tickets.modelo` | `Entidad`, `Campo`, `Entidades` (catálogo declarativo de módulos y operaciones SOAP), `Tabla`, `FilaVista`, `Opcion` |
| Servicio | `tickets.servicio.CrudService` | Validación, armado de parámetros, lectura/escritura vía SOAP |
| Acceso a servicios | `tickets.soap.SoapClient` | Cliente SOAP 1.1 (HTTP + XML), manejo de Fault y tiempos de espera |
| Controlador | `tickets.controlador` | `CrudServlet` (genérico), `TableroServlet`, `HistorialServlet`, `NotificacionesServlet`, `ConfigServlet`, `InicioServlet`, `EncodingFilter` |
| Vista | `WEB-INF/jsp` | JSP + JSTL, sin lógica de negocio; CSS responsive propio (sin CDN) |

Flujo de una petición: `GET /crud/usuarios` → `CrudServlet.lista` → `CrudService.listar` → `SoapClient.invocar("WSUsuarios","seleccionarUsuarios")` → respuesta XML → `Filas.parsear` → `Tabla` → `lista.jsp`.
Las escrituras usan **POST + redirect** (patrón PRG) con mensajes *flash* en sesión. Toda salida se escapa con `<c:out>` (previene XSS).

## 6. Estructura de datos FIFO
`PosicionCola` se fija al insertar (llegada). El tablero lista cada estado en ese orden. El avance de estado es estrictamente secuencial (validado por trigger y reflejado en la interfaz).

## 7. Consideraciones de seguridad y mejoras
Credenciales y URL JDBC duplicadas en cada servicio (centralizar), contraseñas con hash, autenticación/roles en la web (hoy no hay login), protección CSRF.
