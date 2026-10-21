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
import tickets.modelo.Entidades;
import tickets.modelo.FilaVista;
import tickets.modelo.Opcion;
import tickets.modelo.Tabla;
import tickets.servicio.CrudService;
import tickets.soap.SoapClient;
import tickets.soap.SoapException;
import tickets.util.Filas;

/** Alertas pendientes de un especialista (WSNotificaciones) y marcado como leídas. */
@WebServlet(name = "NotificacionesServlet", urlPatterns = {"/notificaciones"})
public class NotificacionesServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final String SERVICIO = "WSNotificaciones";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idEsp = param(req, "idEspecialista");
        List<Opcion> especialistas = new ArrayList<>();
        try {
            especialistas = TableroServlet.especialistas(CrudService.filas(Entidades.obtener("usuarios")));
        } catch (SoapException ex) {
            aviso(req, "error", ex.getMessage());
        }
        Tabla tabla = Tabla.vacia();
        if (!idEsp.isEmpty()) {
            if (!Filas.esEnteroPositivo(idEsp)) {
                aviso(req, "error", "Seleccione un especialista válido.");
            } else {
                try {
                    Map<String, String> p = new LinkedHashMap<>();
                    p.put("idEspecialista", idEsp);
                    List<String> crudas = SoapClient.invocar(SERVICIO, "seleccionarNotificacionesPendientes", p);
                    if (Filas.esError(crudas)) {
                        aviso(req, "error", crudas.get(0));
                    } else {
                        List<Map<String, String>> filas = new ArrayList<>();
                        for (String linea : crudas) {
                            filas.add(Filas.parsear(linea));
                        }
                        tabla = CrudService.construirTabla(filas, "ID Notif");
                        if (filas.isEmpty()) {
                            tabla.conAviso("No hay notificaciones pendientes.");
                        }
                    }
                } catch (SoapException ex) {
                    aviso(req, "error", ex.getMessage());
                }
            }
        }
        req.setAttribute("especialistas", especialistas);
        req.setAttribute("idSeleccionado", idEsp);
        req.setAttribute("tabla", tabla);
        mostrar(req, resp, "notificaciones.jsp", "Notificaciones");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idNotif = param(req, "idNotificacion");
        String idEsp = param(req, "idEspecialista");
        try {
            if (!Filas.esEnteroPositivo(idNotif)) {
                throw new IllegalArgumentException("ID de notificación no válido.");
            }
            Map<String, String> p = new LinkedHashMap<>();
            p.put("idNotificacion", idNotif);
            String mensaje = SoapClient.invocarTexto(SERVICIO, "marcarNotificacionLeida", p);
            flash(req, tipoDe(mensaje), mensaje);
        } catch (IllegalArgumentException | SoapException ex) {
            flash(req, "error", ex.getMessage());
        }
        redirigir(req, resp, "/notificaciones" + (Filas.esEnteroPositivo(idEsp) ? "?idEspecialista=" + idEsp : ""));
    }
}
