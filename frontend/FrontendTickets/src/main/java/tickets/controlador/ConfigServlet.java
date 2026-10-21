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
import tickets.config.AppConfig;
import tickets.modelo.Entidad;
import tickets.modelo.Entidades;
import tickets.servicio.CrudService;
import tickets.soap.SoapClient;
import tickets.soap.SoapException;

/**
 * Configuración en caliente de la URL del servidor de servicios y prueba de conectividad.
 * Útil el día de la presentación: si cambia la IP del servidor de servicios no hay que recompilar.
 */
@WebServlet(name = "ConfigServlet", urlPatterns = {"/config"})
public class ConfigServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    /** Resultado de probar un servicio. */
    public static final class Prueba {
        private final String servicio;
        private final boolean ok;
        private final String detalle;
        private final long milisegundos;

        Prueba(String servicio, boolean ok, String detalle, long milisegundos) {
            this.servicio = servicio;
            this.ok = ok;
            this.detalle = detalle;
            this.milisegundos = milisegundos;
        }

        public String getServicio() { return servicio; }
        public boolean isOk() { return ok; }
        public String getDetalle() { return detalle; }
        public long getMilisegundos() { return milisegundos; }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("ping".equals(param(req, "accion"))) {
            resp.setContentType("text/plain;charset=UTF-8");
            try {
                SoapClient.invocar("WSRoles", "seleccionarTodos", null);
                resp.getWriter().write("OK");
            } catch (SoapException ex) {
                resp.getWriter().write("ERROR: " + ex.getMessage());
            }
            return;
        }
        if ("1".equals(param(req, "probar"))) {
            req.setAttribute("pruebas", probarServicios());
        }
        mostrar(req, resp, "config.jsp", "Configuración");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            AppConfig.setBaseUrl(param(req, "baseUrl"));
            flash(req, "ok", "URL del servidor de servicios actualizada: " + AppConfig.getBaseUrl());
        } catch (IllegalArgumentException ex) {
            flash(req, "error", ex.getMessage());
        }
        redirigir(req, resp, "/config");
    }

    private static List<Prueba> probarServicios() {
        List<Prueba> pruebas = new ArrayList<>();
        for (Entidad e : Entidades.todas()) {
            long t0 = System.nanoTime();
            try {
                int n = CrudService.filas(e).size();
                pruebas.add(new Prueba(e.getServicio(), true, n + " registro(s)", ms(t0)));
            } catch (SoapException ex) {
                pruebas.add(new Prueba(e.getServicio(), false, ex.getMessage(), ms(t0)));
            }
        }
        pruebas.add(sonda("WSHistorial", "seleccionarHistorialPorTicket", "idTicket"));
        pruebas.add(sonda("WSNotificaciones", "seleccionarNotificacionesPendientes", "idEspecialista"));
        return pruebas;
    }

    private static Prueba sonda(String servicio, String operacion, String parametro) {
        long t0 = System.nanoTime();
        try {
            Map<String, String> p = new LinkedHashMap<>();
            p.put(parametro, "0");
            List<String> r = SoapClient.invocar(servicio, operacion, p);
            boolean error = !r.isEmpty() && r.get(0).startsWith("Error");
            return new Prueba(servicio, !error, error ? r.get(0) : "responde", ms(t0));
        } catch (SoapException ex) {
            return new Prueba(servicio, false, ex.getMessage(), ms(t0));
        }
    }

    private static long ms(long t0) {
        return (System.nanoTime() - t0) / 1_000_000L;
    }
}
