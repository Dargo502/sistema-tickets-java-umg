<%@ include file="/WEB-INF/jsp/_header.jspf" %>
<section class="encabezado-pagina">
    <h1><c:out value="${entidad.icono}"/> <c:out value="${entidad.titulo}"/></h1>
    <c:if test="${entidad.puedeCrear}">
        <a class="btn primario" href="<c:url value='/crud/${entidad.clave}/nuevo'/>">+ Nuevo</a>
    </c:if>
</section>

<form class="buscador" method="get" action="<c:url value='/crud/${entidad.clave}'/>">
    <label for="q">Buscar por ID</label>
    <input id="q" name="id" type="number" min="1" inputmode="numeric" placeholder="Ej. 3" value="<c:out value='${q}'/>">
    <button class="btn" type="submit">Buscar</button>
    <c:if test="${not empty q}">
        <a class="btn secundario" href="<c:url value='/crud/${entidad.clave}'/>">Ver todos</a>
    </c:if>
</form>

<c:if test="${not empty errorServicio}">
    <div class="alerta error" role="alert"><c:out value="${errorServicio}"/></div>
</c:if>
<c:if test="${not empty tabla.aviso}">
    <div class="alerta info"><c:out value="${tabla.aviso}"/></div>
</c:if>

<c:choose>
    <c:when test="${empty tabla.filas}">
        <p class="vacio">No hay registros para mostrar.</p>
    </c:when>
    <c:otherwise>
        <div class="tabla-responsive">
            <table class="tabla">
                <thead>
                    <tr>
                        <c:forEach var="col" items="${tabla.columnas}">
                            <th><c:out value="${col}"/></th>
                        </c:forEach>
                        <c:if test="${entidad.puedeEditar or entidad.puedeEliminar}"><th>Acciones</th></c:if>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="fila" items="${tabla.filas}">
                        <tr>
                            <c:forEach var="celda" items="${fila.celdas}" varStatus="st">
                                <td data-label="<c:out value='${tabla.columnas[st.index]}'/>"><c:out value="${celda}"/></td>
                            </c:forEach>
                            <c:if test="${entidad.puedeEditar or entidad.puedeEliminar}">
                                <td data-label="Acciones" class="acciones">
                                    <c:if test="${not empty fila.id}">
                                        <c:if test="${entidad.puedeEditar}">
                                            <a class="btn pequeno" href="<c:url value='/crud/${entidad.clave}/editar'><c:param name='id' value='${fila.id}'/></c:url>">Editar</a>
                                        </c:if>
                                        <c:if test="${entidad.puedeEliminar}">
                                            <form method="post" action="<c:url value='/crud/${entidad.clave}/eliminar'/>" data-confirm="¿Confirma que desea continuar con esta acción?">
                                                <input type="hidden" name="id" value="<c:out value='${fila.id}'/>">
                                                <button class="btn peligro pequeno" type="submit"><c:out value="${entidad.textoEliminar}"/></button>
                                            </form>
                                        </c:if>
                                    </c:if>
                                </td>
                            </c:if>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
        <p class="conteo"><c:out value="${fn:length(tabla.filas)}"/> registro(s)</p>
    </c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/jsp/_footer.jspf" %>
