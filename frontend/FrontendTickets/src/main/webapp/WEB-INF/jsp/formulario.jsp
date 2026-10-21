<%@ include file="/WEB-INF/jsp/_header.jspf" %>
<section class="encabezado-pagina">
    <h1><c:out value="${modo eq 'nuevo' ? 'Nuevo' : 'Editar'}"/> <c:out value="${fn:toLowerCase(entidad.singular)}"/></h1>
    <a class="btn secundario" href="<c:url value='/crud/${entidad.clave}'/>"><svg class="icono"><use href="#i-arrow-left"/></svg> Volver</a>
</section>

<c:if test="${not empty errores}">
    <div class="alerta error" role="alert">
        <ul>
            <c:forEach var="er" items="${errores}"><li><c:out value="${er}"/></li></c:forEach>
        </ul>
    </div>
</c:if>

<form class="formulario" method="post" action="<c:url value='/crud/${entidad.clave}/guardar'/>">
    <c:if test="${modo eq 'editar'}">
        <input type="hidden" name="id" value="<c:out value='${idActual}'/>">
    </c:if>
    <c:forEach var="campo" items="${entidad.campos}">
        <c:if test="${modo eq 'nuevo' or not campo.soloInsertar}">
            <div class="campo">
                <label for="f-${campo.nombre}"><c:out value="${campo.etiqueta}"/><c:if test="${campo.requerido}"> *</c:if></label>
                <c:choose>
                    <c:when test="${campo.tipoNombre eq 'SELECT'}">
                        <select id="f-${campo.nombre}" name="${campo.nombre}" ${campo.requerido ? 'required' : ''}>
                            <option value="">-- Seleccione --</option>
                            <c:forEach var="op" items="${opciones[campo.nombre]}">
                                <option value="<c:out value='${op.id}'/>" ${valores[campo.nombre] eq op.id ? 'selected' : ''}><c:out value="${op.texto}"/></option>
                            </c:forEach>
                        </select>
                    </c:when>
                    <c:when test="${campo.tipoNombre eq 'AREA'}">
                        <textarea id="f-${campo.nombre}" name="${campo.nombre}" rows="4" maxlength="${campo.max}" ${campo.requerido ? 'required' : ''}><c:out value="${valores[campo.nombre]}"/></textarea>
                    </c:when>
                    <c:when test="${campo.tipoNombre eq 'CLAVE'}">
                        <input id="f-${campo.nombre}" name="${campo.nombre}" type="password" autocomplete="new-password" minlength="4" maxlength="${campo.max}" ${campo.requerido ? 'required' : ''}>
                    </c:when>
                    <c:when test="${campo.tipoNombre eq 'NUMERO'}">
                        <input id="f-${campo.nombre}" name="${campo.nombre}" type="number" step="1" inputmode="numeric" value="<c:out value='${valores[campo.nombre]}'/>" ${campo.requerido ? 'required' : ''}>
                    </c:when>
                    <c:when test="${campo.tipoNombre eq 'CORREO'}">
                        <input id="f-${campo.nombre}" name="${campo.nombre}" type="email" maxlength="${campo.max}" value="<c:out value='${valores[campo.nombre]}'/>" ${campo.requerido ? 'required' : ''}>
                    </c:when>
                    <c:otherwise>
                        <input id="f-${campo.nombre}" name="${campo.nombre}" type="text" maxlength="${campo.max}" value="<c:out value='${valores[campo.nombre]}'/>" ${campo.requerido ? 'required' : ''}>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:if>
    </c:forEach>
    <div class="botones">
        <button class="btn primario" type="submit">Guardar</button>
        <a class="btn secundario" href="<c:url value='/crud/${entidad.clave}'/>">Cancelar</a>
    </div>
</form>
<%@ include file="/WEB-INF/jsp/_footer.jspf" %>
