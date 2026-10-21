<%@ page contentType="text/html" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="_header.jspf" %>

<!-- FILA SUPERIOR: TÍTULO Y CONTROLES LIMPIOS -->
<div class="d-flex justify-content-between align-items-center mb-1 flex-wrap gap-2">
    <div>
        <h1 class="h3 mb-1 fw-bold">Tablero de Gestión</h1>
        <p class="text-muted small mb-0">Flujo automatizado para atención técnica estructurada.</p>
    </div>
    
    <!-- Controles de visualización técnicos sin emojis -->
    <div class="d-flex align-items-center gap-1">
        <button class="btn btn-jira-action d-inline-flex align-items-center px-2 py-1" id="btnToggleTheme" onclick="alternarModoOscuro()" title="Cambiar tema" aria-label="Cambiar tema">
            <svg class="icono i-tema-luna"><use href="#i-moon"/></svg>
            <svg class="icono i-tema-sol"><use href="#i-sun"/></svg>
        </button>
        <button class="btn btn-jira-action d-inline-flex align-items-center px-2 py-1" onclick="togglePantallaCompleta()" title="Pantalla completa" aria-label="Pantalla completa">
            <svg class="icono i-fs-entrar"><use href="#i-maximize"/></svg>
            <svg class="icono i-fs-salir"><use href="#i-minimize"/></svg>
        </button>
    </div>
</div>

<!-- FILA INTERMEDIA: ALINEACIÓN PERFECTA DEL BUSCADOR, FILTROS Y BOTÓN -->
<div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-3 pb-3 border-bottom border-secondary-subtle">
    <div class="d-flex align-items-center gap-2 flex-wrap" style="flex: 1;">
        <div class="position-relative" style="max-width: 250px; width: 100%;">
            <svg class="icono icono-input"><use href="#i-search"/></svg>
            <input type="text" id="buscadorTickets" class="form-control py-1.5 fs-7 rounded-2 ps-5" 
                   placeholder="Buscar ticket..." aria-label="Buscar ticket" onkeyup="filtrarTicketsPorTexto()">
        </div>
        <button class="btn btn-sm btn-jira-action py-1.5" onclick="filtrarPrioridad('alta')">Prioridad Alta</button>
        <button class="btn btn-sm btn-jira-action py-1.5" onclick="filtrarPrioridad('todas')">Mostrar Todo</button>
    </div>
    
    <div>
        <button class="btn btn-primary bg-primary border-primary px-4 py-1.5 fw-semibold text-white d-inline-flex align-items-center gap-2" data-bs-toggle="modal" data-bs-target="#modalNuevoTicket">
            <svg class="icono"><use href="#i-plus"/></svg>Crear Ticket
        </button>
    </div>
</div>

<c:if test="${avisoParche}">
    <div class="alert alert-warning small" role="alert">
        El servicio <code>WSTickets</code> no devuelve el ID de los tickets: las acciones del tablero están deshabilitadas.
        Aplique el parche de <code>WSTickets.java</code> en el backend.
    </div>
</c:if>
<div id="avisoTablero"></div>

<c:if test="${empty columnas}">
    <div class="text-center text-muted py-5">
        <svg class="icono icono-xl d-block mx-auto mb-2"><use href="#i-info"/></svg>
        <p class="mb-2">No se pudo cargar el tablero desde los servicios.</p>
        <a class="btn btn-sm btn-primary text-white" href="${pageContext.request.contextPath}/config">Revisar la conexión</a>
    </div>
</c:if>

<!-- TABLERO KANBAN: columnas y tarjetas generadas desde los servicios SOAP -->
<div class="tablero" id="tablero" data-url="${pageContext.request.contextPath}/inicio">
    <c:forEach var="col" items="${columnas}" varStatus="cs">
        <div class="columna" data-estado-id="<c:out value='${col.id}'/>" data-orden="<c:out value='${col.orden}'/>"
             ondragover="permitirSoltar(event)" ondrop="soltarTicket(event, this)">
            <h2>
                <span><c:out value="${col.nombre}"/></span>
                <div class="d-flex align-items-center gap-2">
                    <span class="contador"><c:out value="${fn:length(col.tickets)}"/></span>
                    <c:if test="${cs.last}">
                        <button class="btn p-0 border-0 bg-transparent d-inline-flex align-items-center" onclick="limpiarColumnaFinalizados()" title="Ocultar finalizados de la vista" aria-label="Ocultar finalizados de la vista" style="color: var(--texto-secundario) !important;">
                            <svg class="icono"><use href="#i-trash"/></svg>
                        </button>
                    </c:if>
                </div>
            </h2>
            <div class="cuerpo-columna" style="min-height: 500px;">
                <c:forEach var="t" items="${col.tickets}">
                    <div class="tarjeta prio-<c:out value='${fn:toLowerCase(t.Prio)}'/>" id="ticket-<c:out value='${t.ID}'/>"
                         draggable="${not empty t.ID and not empty t._siguienteId}" ondragstart="iniciarArrastre(event)"
                         data-id="<c:out value='${t.ID}'/>" data-orden="<c:out value='${col.orden}'/>"
                         data-siguiente-id="<c:out value='${t._siguienteId}'/>"
                         data-puede-reclamar="<c:out value='${t._puedeReclamar}'/>">
                        <header class="d-flex justify-content-between align-items-center">
                            <span class="badge bg-secondary-subtle text-body small px-2"><c:out value="${t.Folio}"/></span>
                            <small class="text-muted fw-bold"><c:out value="${t.Prio}"/></small>
                        </header>
                        <div class="titulo my-2 fw-bold"><c:out value="${t['Título']}"/></div>
                        <div class="meta mb-1"><c:out value="${t.Cat}"/> · Solicita: <c:out value="${t.Solicitante}"/></div>
                        <c:if test="${not empty t.Especialista}">
                            <div class="meta mb-1">Atiende: <c:out value="${t.Especialista}"/></div>
                        </c:if>
                        <div class="d-flex justify-content-end gap-2 border-top pt-2 mt-2">
                            <c:choose>
                                <c:when test="${t._puedeReclamar eq 'true'}">
                                    <button type="button" class="btn btn-sm btn-outline-primary py-1 px-2 rounded-2 fw-semibold d-inline-flex align-items-center" style="font-size:0.85rem;" onclick="avanzarDesdeBoton(this)">
                                        Atender <svg class="icono ms-1"><use href="#i-arrow-right"/></svg>
                                    </button>
                                </c:when>
                                <c:when test="${t._puedeAvanzar eq 'true'}">
                                    <button type="button" class="btn btn-sm btn-outline-success py-1 px-2 rounded-2 fw-semibold d-inline-flex align-items-center" style="font-size:0.85rem;" onclick="avanzarDesdeBoton(this)">
                                        Pasar a <c:out value="${t._siguienteNombre}"/> <svg class="icono ms-1"><use href="#i-arrow-right"/></svg>
                                    </button>
                                </c:when>
                                <c:when test="${empty t._siguienteId}">
                                    <span class="text-success small fw-bold d-inline-flex align-items-center" style="font-size:0.85rem;">
                                        <svg class="icono me-1"><use href="#i-check"/></svg> Resuelto
                                    </span>
                                </c:when>
                            </c:choose>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </div>
    </c:forEach>
</div>

<!-- MODAL: REGISTRAR INCIDENCIA (envía el formulario al servlet, que lo guarda por SOAP) -->
<div class="modal fade" id="modalNuevoTicket" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg">
            <div class="modal-header bg-dark text-white py-3">
                <h5 class="modal-title fw-bold">Registrar Incidencia</h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Cerrar"></button>
            </div>
            <form id="formTicketNuevo" method="POST" action="${pageContext.request.contextPath}/inicio">
                <input type="hidden" name="accion" value="crear">
                <div class="modal-body p-4">
                    <div class="mb-3">
                        <label class="form-label fw-semibold small text-muted" for="titulo">Título de la incidencia</label>
                        <input type="text" id="titulo" name="titulo" class="form-control" required maxlength="150" placeholder="Ej: Error de conexión SQL">
                    </div>
                    <div class="row g-3 mb-3">
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold small text-muted" for="categoria">Categoría</label>
                            <select id="categoria" name="categoria" class="form-select" required>
                                <option value="">-- Seleccione --</option>
                                <c:forEach var="c" items="${categorias}">
                                    <option value="<c:out value='${c.id}'/>"><c:out value="${c.texto}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold small text-muted" for="prioridad">Nivel de Prioridad</label>
                            <select id="prioridad" name="prioridad" class="form-select" required>
                                <c:forEach var="p" items="${prioridades}">
                                    <option value="<c:out value='${p.id}'/>" ${p.texto eq 'Media' ? 'selected' : ''}><c:out value="${p.texto}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold small text-muted" for="solicitante">Solicitante</label>
                        <select id="solicitante" name="solicitante" class="form-select" required>
                            <option value="">-- Seleccione --</option>
                            <c:forEach var="u" items="${solicitantes}">
                                <option value="<c:out value='${u.id}'/>"><c:out value="${u.texto}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold small text-muted" for="descripcion">Descripción Detallada</label>
                        <textarea id="descripcion" name="descripcion" class="form-control" rows="3" maxlength="1000" placeholder="Detalle los síntomas del error..."></textarea>
                    </div>
                </div>
                <div class="modal-footer bg-light p-3">
                    <button type="button" class="btn text-secondary" data-bs-dismiss="modal">Cancelar</button>
                    <button type="submit" class="btn btn-primary bg-primary border-primary text-white px-4 fw-semibold">Guardar Ticket</button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- MODAL: ATENDER TICKET (elegir especialista; reclama y pasa a la siguiente etapa) -->
<div class="modal fade" id="modalAtender" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-sm">
        <div class="modal-content border-0 shadow-lg">
            <div class="modal-header bg-dark text-white py-3">
                <h5 class="modal-title fw-bold">Atender ticket</h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Cerrar"></button>
            </div>
            <form method="POST" action="${pageContext.request.contextPath}/inicio">
                <input type="hidden" name="accion" value="atender">
                <input type="hidden" name="idTicket" id="atenderIdTicket">
                <input type="hidden" name="idNuevoEstado" id="atenderIdEstado">
                <div class="modal-body p-4">
                    <label class="form-label fw-semibold small text-muted" for="idEspecialista">Especialista que lo atiende</label>
                    <select id="idEspecialista" name="idEspecialista" class="form-select" required>
                        <option value="">-- Seleccione --</option>
                        <c:forEach var="e" items="${especialistas}">
                            <option value="<c:out value='${e.id}'/>"><c:out value="${e.texto}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="modal-footer bg-light p-3">
                    <button type="button" class="btn text-secondary" data-bs-dismiss="modal">Cancelar</button>
                    <button type="submit" class="btn btn-primary bg-primary border-primary text-white px-4 fw-semibold">Atender</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%@ include file="_footer.jspf" %>
