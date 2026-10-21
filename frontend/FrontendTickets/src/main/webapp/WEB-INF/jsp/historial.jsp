<%@ page contentType="text/html" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="_header.jspf" %>

<div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
    <div>
        <h1 class="h3 mb-1 fw-bold">Centro de Reportes Gerenciales</h1>
        <p class="text-muted small mb-0">Auditoría de movimientos y exportación de la bitácora de cada ticket.</p>
    </div>
    <div class="d-flex gap-2">
        <a href="${pageContext.request.contextPath}/historial?accion=exportar&tipo=pdf&id=<c:out value='${idSeleccionado}'/>"
           class="btn btn-jira-action border-danger text-danger fw-semibold d-inline-flex align-items-center gap-2 ${empty idSeleccionado ? 'disabled' : ''}"
           ${empty idSeleccionado ? 'aria-disabled="true" tabindex="-1"' : ''}>
            <svg class="icono"><use href="#i-file-text"/></svg>Exportar PDF
        </a>
        <a href="${pageContext.request.contextPath}/historial?accion=exportar&tipo=excel&id=<c:out value='${idSeleccionado}'/>"
           class="btn btn-jira-action border-success text-success fw-semibold d-inline-flex align-items-center gap-2 ${empty idSeleccionado ? 'disabled' : ''}"
           ${empty idSeleccionado ? 'aria-disabled="true" tabindex="-1"' : ''}>
            <svg class="icono"><use href="#i-table"/></svg>Exportar Excel
        </a>
    </div>
</div>

<div class="card border-0 shadow-sm mb-4 p-3" style="background-color: var(--bg-tarjeta);">
    <form method="GET" action="${pageContext.request.contextPath}/historial" class="row g-2 align-items-center">
        <div class="col-12 col-md-9">
            <label class="visually-hidden" for="selectTicket">Ticket</label>
            <select id="selectTicket" name="id" class="form-select border-2 py-2 rounded-2" required>
                <option value="">-- Seleccione un ticket para ver su bitácora --</option>
                <c:forEach var="t" items="${tickets}">
                    <option value="<c:out value='${t.id}'/>" ${t.id eq idSeleccionado ? 'selected' : ''}><c:out value="${t.texto}"/></option>
                </c:forEach>
            </select>
        </div>
        <div class="col-12 col-md-3">
            <button type="submit" class="btn btn-primary bg-primary border-primary text-white w-100 py-2 fw-semibold d-inline-flex align-items-center justify-content-center gap-2">
                <svg class="icono"><use href="#i-search"/></svg>Consultar Bitácora
            </button>
        </div>
    </form>
</div>

<div class="card border-0 shadow-sm overflow-hidden mb-4" style="background-color: var(--bg-tarjeta);">
    <div class="card-body p-3">
        <c:choose>
            <c:when test="${empty idSeleccionado}">
                <div class="text-center py-5 text-muted">
                    <svg class="icono icono-xl d-block mx-auto mb-2"><use href="#i-clipboard"/></svg>
                    <h5 class="fw-bold">Bandeja de Auditoría Vacía</h5>
                    <p class="small text-muted mb-0">Seleccione un ticket para desplegar la línea de tiempo de sus estados.</p>
                </div>
            </c:when>
            <c:when test="${empty tabla.filas}">
                <div class="text-center py-4 text-muted">
                    <c:out value="${empty tabla.aviso ? 'No hay movimientos para mostrar.' : tabla.aviso}"/>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-responsive">
                    <table class="table table-hover table-striped align-middle mb-0">
                        <thead class="table-dark">
                            <tr>
                                <c:forEach var="col" items="${tabla.columnas}">
                                    <th><c:out value="${col}"/></th>
                                </c:forEach>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="fila" items="${tabla.filas}">
                                <tr>
                                    <c:forEach var="celda" items="${fila.celdas}" varStatus="st">
                                        <td>
                                            <c:choose>
                                                <c:when test="${tabla.columnas[st.index] eq 'De'}">
                                                    <span class="badge bg-secondary"><c:out value="${celda}"/></span>
                                                </c:when>
                                                <c:when test="${tabla.columnas[st.index] eq 'A'}">
                                                    <span class="badge bg-warning text-dark"><c:out value="${celda}"/></span>
                                                </c:when>
                                                <c:otherwise><c:out value="${celda}"/></c:otherwise>
                                            </c:choose>
                                        </td>
                                    </c:forEach>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="_footer.jspf" %>
