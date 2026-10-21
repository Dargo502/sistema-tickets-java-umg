# Manual de Operación y Usuario — Sistema de Gestión de Tickets

Este documento describe la funcionalidad de la plataforma y sirve como guía operativa para la administración de incidencias bajo el esquema de colas FIFO en el entorno distribuido.

---

## 1. Propósito del Sistema

El sistema automatiza el registro, seguimiento y resolución de incidentes técnicos (*help desk*) garantizando un flujo secuencial estricto controlado por base de datos. Las incidencias transicionan de manera ordenada bajo los siguientes estados mutables:
**Solicitado → En Proceso → Finalizado**

---

## 2. Acceso a la Plataforma

Para iniciar la aplicación, abra el navegador web de su dispositivo e ingrese la URL correspondiente al nodo de presentación:
```http
http://[IP_NODO_FRONTEND]:8080/FrontendTickets/
```
* **Estado de la conexión:** si los servicios no responden, el tablero muestra una alerta roja con el motivo y el botón **Revisar la conexión**, que lleva a la pantalla de Configuración.
* **Adaptabilidad Móvil:** En dispositivos móviles, la interfaz se compacta automáticamente y habilita un menú colapsable accesible mediante el ícono de hamburguesa (botón de tres líneas).

---

## 3. Operación del Tablero FIFO

El módulo central opera mediante un modelo Kanban adaptado a la lógica FIFO (primero en entrar, primero en salir), organizando las tarjetas de modo que la incidencia más antigua se ubica siempre en la parte superior de cada columna.

### Elementos de la Tarjeta de Incidencia
Cada tarjeta muestra el folio, el título, la prioridad (el color del borde indica Alta, Media o Baja), la categoría, el solicitante y, cuando ya fue tomada, el especialista que la atiende.

### Flujo de Acciones de Control
* **Atender:** en la columna *Solicitado*, el botón **Atender** abre un diálogo para elegir al especialista; al confirmar, el ticket queda reclamado y pasa a *En Proceso*. Un mismo ticket no puede ser reclamado por dos especialistas.
* **Pasar a la siguiente etapa:** los tickets en *En Proceso* tienen el botón **Pasar a Finalizado**. El flujo es secuencial: no se puede saltar una etapa ni retroceder.
* **Arrastrar y soltar:** también puede arrastrar la tarjeta a la columna siguiente; es equivalente a usar el botón.
* **Procesando:** mientras el servidor confirma la acción aparece el aviso *Procesando…* y se bloquean los botones para evitar envíos duplicados.
* **Buscar y filtrar:** el cuadro de búsqueda filtra las tarjetas por texto, y los botones **Prioridad Alta** y **Mostrar Todo** filtran por prioridad.
* **Ocultar finalizados:** el ícono de papelera de la columna *Finalizado* solo oculta las tarjetas de la pantalla; los tickets siguen guardados.

---

## 4. Registro de Nuevas Incidencias

1. En el tablero, pulse el botón **Crear Ticket** (esquina superior derecha).
2. Complete el formulario *Registrar Incidencia*: *Título*, *Categoría*, *Nivel de Prioridad* y *Solicitante* son obligatorios; la *Descripción* es opcional.
3. Pulse **Guardar Ticket**. El sistema registra el ticket en el backend, le asigna automáticamente un folio según la categoría (por ejemplo `HD-001`, `SW-002`) y lo coloca al final de la cola en *Solicitado*.

---

## 5. Módulos de Mantenimiento y Catálogos

Los módulos de mantenimiento (*Tickets, Usuarios, Empresas, Roles, Categorías, Estados y Prioridades*) comparten un mismo patrón. Se abren con la dirección `/crud/{módulo}` (por ejemplo `/crud/usuarios` o `/crud/roles`):

* **Visualización de Datos:** Despliega los registros en tablas estructuradas para pantallas de escritorio, transformándose de forma automática en tarjetas independientes al procesarse desde pantallas móviles.
* **Búsqueda Dinámica por ID:** Permite filtrar de forma exacta ingresando el código del registro y presionando *Buscar*. El control *Ver todos* restablece los filtros de la grilla.
* **Inserción y Modificación:** Al presionar **+ Nuevo** o **Editar**, se despliega un formulario con validaciones en tiempo real en la capa del servidor. Los campos requeridos se encuentran señalados con un asterisco (`*`). Si el sistema detecta inconsistencias (longitudes inválidas o formatos de correo erróneos), interrumpe el almacenamiento y preserva los datos ingresados para su corrección rápida.
* **Remoción Segura (Borrado Lógico):** El botón de eliminación solicita una confirmación explícita. Para proteger la integridad referencial del histórico, los módulos de *Usuarios* y *Empresas* ejecutan una **desactivación de estado**, mientras que los *Tickets* se marcan con un flag oculto sin ser eliminados físicamente del motor de base de datos.

---

## 6. Centro de Notificaciones

Permite auditar el buzón de alertas del personal técnico. Al seleccionar un especialista y pulsar **Ver pendientes**, la interfaz cargará las acciones del sistema automatizadas por los *triggers*. La acción **Marcar como leída** archiva la alerta del tablero de pendientes del especialista.

---

## 7. Reportes e Historial

La opción **Reportes** del menú abre el Centro de Reportes. Al seleccionar un ticket se muestra su bitácora en orden cronológico: fecha y hora, usuario responsable, estado de origen, estado de destino y nota del cambio.
Desde esa pantalla se puede descargar el **reporte en PDF** (con el logo de la universidad, los datos del ticket, indicadores, un gráfico del tiempo que pasó en cada estado y la tabla de movimientos) o el **reporte en Excel** (.xlsx, con una hoja de resumen y otra con la bitácora lista para filtrar).

---

## 8. Consola de Configuración General

Permite indicar la dirección del servidor de servicios (por ejemplo `http://192.168.1.11:8080/BackendTickets`) sin reiniciar la aplicación. El botón **Probar conexión** consulta los 9 servicios SOAP y muestra, para cada uno, si respondió (**OK**) o falló, junto con el tiempo de respuesta y el motivo del error.

---

## 9. Catálogo de Mensajes del Sistema

| Mensaje de Error en Pantalla | Significado Técnico | Acción Correctiva Recomendada |
| :--- | :--- | :--- |
| *No se pudo conectar con el servicio...* | Pérdida de conectividad con la capa intermedia SOAP. | Verificar IP en la sección de *Configuración* y revisar reglas del firewall local. |
| *Transición de estado inválida* | Intento de saltar fases del ciclo de vida FIFO. | Avanzar los flujos estrictamente de uno en uno (`Solicitado -> En Proceso -> Finalizado`). |
| *El ticket ya fue reclamado* | Conflicto de concurrencia en base de datos. | Seleccionar otra incidencia libre; el ticket ya cuenta con un especialista asignado. |
| *Error (puede estar en uso)* | Restricción por clave foránea de integridad referencial. | No es posible eliminar el catálogo porque existen registros dependientes vinculados a él. |
