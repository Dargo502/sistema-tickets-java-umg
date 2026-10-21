<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ include file="/WEB-INF/jsp/_header.jspf" %>
<section class="encabezado-pagina"><h1>Ocurrió un problema</h1></section>
<div class="alert alert-danger">
    La página solicitada no está disponible o se produjo un error interno.
    Código: <c:out value="${pageContext.errorData.statusCode}"/>
    <c:if test="${not empty pageContext.exception}">
        <hr class="my-2">
        <small>Detalle técnico: <c:out value="${pageContext.exception.class.simpleName}"/> —
        <c:out value="${pageContext.exception.message}"/></small>
    </c:if>
</div>
<p><a class="btn btn-primary" href="<c:url value='/inicio'/>">Volver al inicio</a></p>
<%@ include file="/WEB-INF/jsp/_footer.jspf" %>
