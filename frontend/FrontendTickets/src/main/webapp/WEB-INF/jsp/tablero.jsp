<%@ include file="/WEB-INF/jsp/_header.jspf" %>
<section class="encabezado-pagina">
    <h1><svg class="icono"><use href="#i-clipboard"/></svg> Tablero FIFO</h1>
    <a class="btn primario" href="<c:url value='/crud/tickets/nuevo'/>">+ Nuevo ticket</a>
</section>
<p class="intro">Los tickets se atienden en estricto orden de llegada (el más antiguo arriba). Un ticket debe ser reclamado por un especialista y avanza un estado a la vez.</p>

<c:if test="${avisoParche}">
    <div class="alerta info">
        El servicio <code>WSTickets</code> no devuelve el ID de cada ticket, por lo que las acciones están deshabilitadas.
        Aplique el parche del backend (carpeta <code>backend/parches</code>).
    </div>
</c:if>

<div class="tablero">
    <c:forEach var="col" items="${columnas}">
        <section class="columna">
            <h2><c:out value="${col.nombre}"/> <span class="contador"><c:out value="${fn:length(col.tickets)}"/></span></h2>
            <c:if test="${empty col.tickets}"><p class="vacio">Sin tickets</p></c:if>
            <c:forEach var="t" items="${col.tickets}" varStatus="st">
                <article class="tarjeta prio-<c:out value='${fn:toLowerCase(t.Prio)}'/>">
                    <header>
                        <strong><c:out value="${t['Folio']}"/></strong>
                        <span class="chip">#<c:out value="${st.index + 1}"/> en cola</span>
                    </header>
                    <p class="titulo"><c:out value="${t['Título']}"/></p>
                    <p class="meta">
                        <span class="chip"><c:out value="${t['Cat']}"/></span>
                        <span class="chip prio"><c:out value="${t['Prio']}"/></span>
                    </p>
                    <p class="meta">Solicita: <c:out value="${t['Solicitante']}"/></p>
                    <c:if test="${not empty t['Especialista']}">
                        <p class="meta">Atiende: <c:out value="${t['Especialista']}"/></p>
                    </c:if>

                    <c:if test="${t['_puedeReclamar'] eq 'true'}">
                        <form class="accion" method="post" action="<c:url value='/tablero'/>">
                            <input type="hidden" name="accion" value="reclamar">
                            <input type="hidden" name="idTicket" value="<c:out value='${t.ID}'/>">
                            <select name="idEspecialista" required aria-label="Especialista">
                                <option value="">Especialista…</option>
                                <c:forEach var="e" items="${especialistas}">
                                    <option value="<c:out value='${e.id}'/>"><c:out value="${e.texto}"/></option>
                                </c:forEach>
                            </select>
                            <button class="btn primario pequeno" type="submit">Reclamar</button>
                        </form>
                    </c:if>
                    <c:if test="${t['_puedeAvanzar'] eq 'true'}">
                        <form class="accion" method="post" action="<c:url value='/tablero'/>" data-confirm="¿Mover el ticket a la siguiente etapa?">
                            <input type="hidden" name="accion" value="avanzar">
                            <input type="hidden" name="idTicket" value="<c:out value='${t.ID}'/>">
                            <input type="hidden" name="idNuevoEstado" value="<c:out value='${t._siguienteId}'/>">
                            <button class="btn primario pequeno" type="submit">Pasar a <c:out value="${t['_siguienteNombre']}"/><svg class="icono ms-1"><use href="#i-arrow-right"/></svg></button>
                        </form>
                    </c:if>
                    <c:if test="${t['_puedeEliminar'] eq 'true'}">
                        <div class="accion fila">
                            <a class="btn pequeno" href="<c:url value='/historial'><c:param name='id' value='${t.ID}'/></c:url>">Historial</a>
                            <a class="btn pequeno" href="<c:url value='/crud/tickets/editar'><c:param name='id' value='${t.ID}'/></c:url>">Editar</a>
                            <form method="post" action="<c:url value='/tablero'/>" data-confirm="¿Eliminar este ticket?">
                                <input type="hidden" name="accion" value="eliminar">
                                <input type="hidden" name="idTicket" value="<c:out value='${t.ID}'/>">
                                <button class="btn peligro pequeno" type="submit">Eliminar</button>
                            </form>
                        </div>
                    </c:if>
                </article>
            </c:forEach>
        </section>
    </c:forEach>
</div>
<%@ include file="/WEB-INF/jsp/_footer.jspf" %>
