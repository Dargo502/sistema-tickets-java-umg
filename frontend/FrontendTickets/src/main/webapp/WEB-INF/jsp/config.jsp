<%@ page contentType="text/html" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="_header.jspf" %>

<div class="mb-4">
    <h1 class="h3 mb-1 fw-bold">Configuración del Ecosistema</h1>
    <p class="text-muted small">URL del servidor de servicios SOAP y diagnóstico de conectividad del sistema distribuido.</p>
</div>

<div class="row g-4">
    <div class="col-12 col-lg-7">
        <div class="card border-0 shadow-sm p-4 h-100" style="background-color: var(--bg-tarjeta);">
            <h2 class="h5 fw-bold mb-3 d-flex align-items-center gap-2"><svg class="icono"><use href="#i-link"/></svg>Endpoints del Servidor SOAP</h2>
            <p class="text-muted small mb-4">Cambie la dirección del servidor de servicios sin recompilar. En una sola máquina use <code>http://localhost:8080/BackendTickets</code>.</p>

            <form method="POST" action="${pageContext.request.contextPath}/config">
                <div class="mb-4">
                    <label for="baseUrl" class="form-label fw-semibold small text-muted">URL base de los servicios</label>
                    <input type="text" id="baseUrl" name="baseUrl" class="form-control py-2 border-2 rounded-2"
                           value="<c:out value='${baseUrl}'/>"
                           placeholder="http://localhost:8080/BackendTickets" required>
                    <div class="form-text text-muted small mt-1">Incluya el protocolo <code>http://</code>, la IP o nombre del servidor, el puerto y el contexto <code>/BackendTickets</code>.</div>
                </div>

                <div class="d-flex flex-wrap gap-2 pt-2">
                    <button type="submit" class="btn btn-primary px-4 fw-semibold py-2 d-inline-flex align-items-center gap-2"><svg class="icono"><use href="#i-save"/></svg>Guardar conexión</button>
                    <a href="${pageContext.request.contextPath}/config?probar=1" class="btn btn-outline-primary px-4 fw-semibold py-2 d-inline-flex align-items-center gap-2"><svg class="icono"><use href="#i-check-circle"/></svg>Probar conexión con los servicios</a>
                </div>
            </form>
        </div>
    </div>

    <div class="col-12 col-lg-5">
        <div class="card border-0 shadow-sm p-4 h-100" style="background-color: var(--bg-tarjeta);">
            <h2 class="h5 fw-bold mb-3 d-flex align-items-center gap-2"><svg class="icono"><use href="#i-globe"/></svg>Arquitectura de 3 capas</h2>
            <ul class="list-unstyled small mb-0 d-flex flex-column gap-3">
                <li>
                    <span class="fw-bold d-block">Capa de datos</span>
                    <span class="text-muted">SQL Server · JDBC puerto 1433</span>
                </li>
                <li>
                    <span class="fw-bold d-block">Capa de servicios</span>
                    <span class="text-muted">BackendTickets · JAX-WS (SOAP) sobre GlassFish 7</span>
                </li>
                <li>
                    <span class="fw-bold d-block">Capa de presentación</span>
                    <span class="text-muted">FrontendTickets · Servlet/JSP MVC + Bootstrap 5</span>
                </li>
            </ul>
        </div>
    </div>
</div>

<c:if test="${not empty pruebas}">
    <div class="card border-0 shadow-sm p-4 mt-4" style="background-color: var(--bg-tarjeta);">
        <h2 class="h5 fw-bold mb-3 d-flex align-items-center gap-2"><svg class="icono"><use href="#i-clipboard"/></svg>Resultado del diagnóstico</h2>
        <p class="text-muted small mb-3">Servidor probado: <code><c:out value="${baseUrl}"/></code></p>
        <div class="table-responsive">
            <table class="table align-middle mb-0">
                <thead>
                    <tr>
                        <th>Servicio</th>
                        <th>Estado</th>
                        <th>Detalle</th>
                        <th class="text-end">Tiempo</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="p" items="${pruebas}">
                        <tr>
                            <td class="fw-semibold"><c:out value="${p.servicio}"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${p.ok}"><span class="badge bg-success d-inline-flex align-items-center gap-1"><svg class="icono"><use href="#i-check"/></svg>OK</span></c:when>
                                    <c:otherwise><span class="badge bg-danger d-inline-flex align-items-center gap-1"><svg class="icono"><use href="#i-alert"/></svg>Error</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td class="small text-muted"><c:out value="${p.detalle}"/></td>
                            <td class="text-end small text-muted"><c:out value="${p.milisegundos}"/> ms</td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</c:if>

<%@ include file="_footer.jspf" %>
