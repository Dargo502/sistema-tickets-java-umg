<%@ page contentType="text/html" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="_header.jspf" %>

<div class="mb-4">
    <h1 class="h3 mb-1 fw-bold">Centro de Notificaciones</h1>
    <p class="text-muted small">Alertas de sistema de la infraestructura distribuida bajo Radmin VPN.</p>
</div>

<div class="card border-0 shadow-sm p-4" style="background-color: var(--bg-tarjeta);">
    <div class="alert alert-danger d-flex align-items-center gap-3 mb-3 border-0 shadow-sm py-3" role="alert">
        <svg class="icono icono-lg text-danger"><use href="#i-alert"/></svg>
        <div>
            <div class="fw-bold text-danger">Fallo de Comunicación - SQL Server</div>
            <span class="small text-muted">La conexión TCP/IP hacia la IP de Libbny falló en el puerto 1433 de la red virtual. Reintentando...</span>
        </div>
    </div>

    <div class="alert alert-success d-flex align-items-center gap-3 mb-3 border-0 shadow-sm py-3" role="alert">
        <svg class="icono icono-lg text-success"><use href="#i-check-circle"/></svg>
        <div>
            <div class="fw-bold text-success">Middleware Sincronizado Exitosamente</div>
            <span class="small text-muted">Servidor GlassFish 7.1.0 levantado en el puerto 9090. Web Services SOAP (WSTickets) listos para consumo asíncrono.</span>
        </div>
    </div>

    <div class="alert alert-info d-flex align-items-center gap-3 mb-0 border-0 shadow-sm py-3" role="alert">
        <svg class="icono icono-lg text-info"><use href="#i-info"/></svg>
        <div>
            <div class="fw-bold text-info">Nuevos Tickets en Cola FIFO</div>
            <span class="small text-muted">Se registraron 2 incidencias de prioridad alta desde el nodo cliente móvil.</span>
        </div>
    </div>
</div>

<%@ include file="_footer.jspf" %>
