package tickets.controlador;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tickets.modelo.Campo;
import tickets.modelo.Entidad;
import tickets.modelo.Entidades;
import tickets.modelo.Opcion;
import tickets.modelo.Tabla;
import tickets.servicio.CrudService;
import tickets.soap.SoapException;
import tickets.util.Filas;

/**
 * Controlador genérico CRUD (patrón MVC). Rutas:
 *   GET  /crud/{modulo}                lista (o filtra con ?id=)
 *   GET  /crud/{modulo}/nuevo          formulario de alta
 *   GET  /crud/{modulo}/editar?id=N    formulario de edición
 *   POST /crud/{modulo}/guardar        inserta o actualiza
 *   POST /crud/{modulo}/eliminar       elimina / desactiva
 */
@WebServlet(name = "CrudServlet", urlPatterns = {"/crud/*"})
public class CrudServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] partes = partes(req);
        Entidad e = partes.length > 0 ? Entidades.obtener(partes[0]) : null;
        if (e == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Módulo no encontrado");
            return;
        }
        String accion = partes.length > 1 ? partes[1] : "lista";
        switch (accion) {
            case "lista":
                lista(req, resp, e);
                break;
            case "nuevo":
                if (!e.isPuedeCrear()) {
                    resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Operación no disponible");
                    return;
                }
                formulario(req, resp, e, "nuevo", "", valoresVacios(e), new ArrayList<String>());
                break;
            case "editar":
                editar(req, resp, e);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Acción no encontrada");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] partes = partes(req);
        Entidad e = partes.length > 0 ? Entidades.obtener(partes[0]) : null;
        if (e == null || partes.length < 2) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Módulo no encontrado");
            return;
        }
        if ("guardar".equals(partes[1])) {
            guardar(req, resp, e);
        } else if ("eliminar".equals(partes[1])) {
            eliminar(req, resp, e);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Acción no encontrada");
        }
    }

    // ------------------------------------------------------------------ acciones

    private void lista(HttpServletRequest req, HttpServletResponse resp, Entidad e)
            throws ServletException, IOException {
        String q = param(req, "id");
        Tabla tabla = Tabla.vacia();
        try {
            tabla = q.isEmpty() ? CrudService.listar(e) : CrudService.buscarPorId(e, q);
        } catch (IllegalArgumentException ex) {
            req.setAttribute("errorServicio", ex.getMessage());
        } catch (SoapException ex) {
            req.setAttribute("errorServicio", ex.getMessage());
        }
        req.setAttribute("entidad", e);
        req.setAttribute("q", q);
        req.setAttribute("tabla", tabla);
        mostrar(req, resp, "lista.jsp", e.getTitulo());
    }

    private void editar(HttpServletRequest req, HttpServletResponse resp, Entidad e)
            throws ServletException, IOException {
        String id = param(req, "id");
        if (!e.isPuedeEditar() || !Filas.esEnteroPositivo(id)) {
            flash(req, "error", "No se puede editar: ID no válido u operación no disponible.");
            redirigir(req, resp, "/crud/" + e.getClave());
            return;
        }
        List<String> errores = new ArrayList<>();
        Map<String, List<Opcion>> opciones = CrudService.opcionesDe(e, errores);
        Map<String, String> fila;
        try {
            fila = CrudService.obtener(e, id);
        } catch (SoapException ex) {
            flash(req, "error", ex.getMessage());
            redirigir(req, resp, "/crud/" + e.getClave());
            return;
        }
        if (fila == null) {
            flash(req, "error", "No se encontró el registro con ID " + id + ".");
            redirigir(req, resp, "/crud/" + e.getClave());
            return;
        }
        Map<String, String> valores = CrudService.valoresFormulario(e, fila, opciones);
        req.setAttribute("opciones", opciones);
        formulario(req, resp, e, "editar", id, valores, errores);
    }

    private void guardar(HttpServletRequest req, HttpServletResponse resp, Entidad e)
            throws ServletException, IOException {
        String id = param(req, "id");
        boolean edicion = !id.isEmpty();
        if ((edicion && !e.isPuedeEditar()) || (!edicion && !e.isPuedeCrear())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Operación no disponible");
            return;
        }
        Map<String, String> valores = leerValores(req, e);
        List<String> errores = CrudService.validar(e, valores, edicion);
        if (edicion && !Filas.esEnteroPositivo(id)) {
            errores.add("El ID del registro no es válido.");
        }
        if (errores.isEmpty()) {
            try {
                String mensaje = CrudService.guardar(e, valores, edicion ? id : null);
                if (Filas.esError(mensaje) || Filas.noEncontrado(mensaje)) {
                    errores.add(mensaje);
                } else {
                    flash(req, "ok", mensaje);
                    redirigir(req, resp, "/crud/" + e.getClave());
                    return;
                }
            } catch (SoapException ex) {
                errores.add(ex.getMessage());
            }
        }
        formulario(req, resp, e, edicion ? "editar" : "nuevo", id, valores, errores);
    }

    private void eliminar(HttpServletRequest req, HttpServletResponse resp, Entidad e) throws IOException {
        String id = param(req, "id");
        try {
            String mensaje = CrudService.eliminar(e, id);
            flash(req, tipoDe(mensaje), mensaje);
        } catch (IllegalArgumentException | SoapException ex) {
            flash(req, "error", ex.getMessage());
        }
        redirigir(req, resp, "/crud/" + e.getClave());
    }

    // ------------------------------------------------------------------ auxiliares

    @SuppressWarnings("unchecked")
    private void formulario(HttpServletRequest req, HttpServletResponse resp, Entidad e, String modo, String id,
                            Map<String, String> valores, List<String> errores)
            throws ServletException, IOException {
        if (req.getAttribute("opciones") == null) {
            req.setAttribute("opciones", CrudService.opcionesDe(e, errores));
        }
        req.setAttribute("entidad", e);
        req.setAttribute("modo", modo);
        req.setAttribute("idActual", id);
        req.setAttribute("valores", valores);
        req.setAttribute("errores", errores);
        mostrar(req, resp, "formulario.jsp", ("nuevo".equals(modo) ? "Nuevo " : "Editar ") + e.getSingular());
    }

    private static Map<String, String> leerValores(HttpServletRequest req, Entidad e) {
        Map<String, String> valores = new LinkedHashMap<>();
        for (Campo c : e.getCampos()) {
            String v = req.getParameter(c.getNombre());
            valores.put(c.getNombre(), v == null ? "" : v.trim());
        }
        return valores;
    }

    private static Map<String, String> valoresVacios(Entidad e) {
        Map<String, String> valores = new LinkedHashMap<>();
        for (Campo c : e.getCampos()) {
            valores.put(c.getNombre(), "");
        }
        return valores;
    }

    /** "/empresas/editar" -> ["empresas","editar"] */
    private static String[] partes(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || info.length() <= 1) {
            return new String[0];
        }
        return info.substring(1).split("/");
    }
}
