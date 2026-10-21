package tickets.controlador;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tickets.modelo.Entidades;
import tickets.modelo.Opcion;
import tickets.modelo.Tabla;
import tickets.servicio.CrudService;
import tickets.soap.SoapClient;
import tickets.soap.SoapException;
import tickets.util.Filas;
import tickets.reporte.DatosBitacora;
import tickets.reporte.ExcelBitacora;
import tickets.reporte.ReporteBitacora;

/**
 * Centro de reportes: historial de cambios de estado de un ticket (WSHistorial)
 * con exportación a un reporte PDF formal y a un libro de Excel (.xlsx).
 *   GET /historial?id=N                          muestra la bitácora
 *   GET /historial?id=N&accion=exportar&tipo=pdf|excel   descarga el reporte
 */
@WebServlet(name = "HistorialServlet", urlPatterns = {"/historial"})
public class HistorialServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final String SERVICIO = "WSHistorial";
    private static final String OPERACION = "seleccionarHistorialPorTicket";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = param(req, "id");
        String accion = param(req, "accion");

        if (!id.isEmpty() && !Filas.esEnteroPositivo(id)) {
            aviso(req, "error", "El ID del ticket debe ser un número entero positivo.");
            id = "";
        }

        // Exportar: no dibuja la página, devuelve el archivo
        if ("exportar".equals(accion) && !id.isEmpty()) {
            exportar(req, resp, id, param(req, "tipo"));
            return;
        }

        // Desplegable con los tickets reales (Folio — Título)
        List<Opcion> tickets = new ArrayList<>();
        try {
            for (Map<String, String> t : CrudService.filas(Entidades.obtener("tickets"))) {
                String idTicket = t.get("ID");
                if (!Filas.vacio(idTicket)) {
                    tickets.add(new Opcion(idTicket, t.getOrDefault("Folio", "Ticket " + idTicket)
                            + " — " + t.getOrDefault("Título", "")));
                }
            }
        } catch (SoapException ex) {
            aviso(req, "error", "No se pudo cargar la lista de tickets: " + ex.getMessage());
        }

        Tabla tabla = Tabla.vacia();
        if (!id.isEmpty()) {
            try {
                List<Map<String, String>> filas = cargarHistorial(id);
                tabla = CrudService.construirTabla(filas, null);
                if (filas.isEmpty()) {
                    tabla.conAviso("Este ticket aún no tiene movimientos registrados.");
                }
            } catch (SoapException ex) {
                aviso(req, "error", ex.getMessage());   // el error real, no uno genérico
            }
        }
        req.setAttribute("tickets", tickets);
        req.setAttribute("idSeleccionado", id);
        req.setAttribute("tabla", tabla);
        mostrar(req, resp, "historial.jsp", "Centro de Reportes Gerenciales");
    }

    // ------------------------------------------------------------------ datos

    private List<Map<String, String>> cargarHistorial(String idTicket) throws SoapException {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("idTicket", idTicket);
        List<String> crudas = SoapClient.invocar(SERVICIO, OPERACION, p);
        if (Filas.esError(crudas)) {
            throw new SoapException(crudas.get(0));
        }
        List<Map<String, String>> filas = new ArrayList<>();
        for (String linea : crudas) {
            filas.add(separarTransicion(Filas.parsear(linea)));
        }
        return filas;
    }

    /** El servicio devuelve "De: X -> A: Y" en un solo campo; se separa en dos columnas. */
    static Map<String, String> separarTransicion(Map<String, String> fila) {
        String de = fila.get("De");
        String marca = " -> A: ";
        if (de == null || !de.contains(marca)) {
            return fila;
        }
        Map<String, String> nueva = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : fila.entrySet()) {
            if ("De".equals(e.getKey())) {
                int i = de.indexOf(marca);
                nueva.put("De", de.substring(0, i));
                nueva.put("A", de.substring(i + marca.length()));
            } else {
                nueva.put(e.getKey(), e.getValue());
            }
        }
        return nueva;
    }

    // ------------------------------------------------------------------ exportación

    private void exportar(HttpServletRequest req, HttpServletResponse resp, String id, String tipo)
            throws IOException {
        List<Map<String, String>> filas;
        try {
            filas = cargarHistorial(id);
        } catch (SoapException ex) {
            flash(req, "error", "No se pudo generar el reporte: " + ex.getMessage());
            redirigir(req, resp, "/historial?id=" + id);
            return;
        }
        // el id ya fue validado como número, por eso es seguro usarlo en el nombre del archivo
        DatosBitacora datos = DatosBitacora.crear(id, ticketPorId(id), filas, LocalDateTime.now());
        if ("excel".equalsIgnoreCase(tipo)) {
            enviar(resp, ExcelBitacora.generar(datos), "Bitacora_Ticket_" + id + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else {
            enviar(resp, ReporteBitacora.generar(logo(), datos), "Bitacora_Ticket_" + id + ".pdf", "application/pdf");
        }
    }

    private void enviar(HttpServletResponse resp, byte[] bytes, String nombre, String tipoMime) throws IOException {
        resp.setContentType(tipoMime);
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + nombre + "\"");
        resp.setContentLength(bytes.length);
        try (OutputStream out = resp.getOutputStream()) {
            out.write(bytes);
        }
    }

    /** Datos del ticket para el encabezado del reporte; si no se pueden leer, el reporte sale solo con la bitácora. */
    private Map<String, String> ticketPorId(String id) {
        try {
            for (Map<String, String> t : CrudService.filas(Entidades.obtener("tickets"))) {
                if (id.equals(t.get("ID"))) {
                    return t;
                }
            }
        } catch (SoapException ex) {
            // se ignora: el reporte no depende de estos datos
        }
        return null;
    }

    private static volatile byte[] logoCache;

    /** Logo de la UMG que ya está en el WAR (css/logo-umg.png); null si no se encuentra. */
    private byte[] logo() {
        byte[] cache = logoCache;
        if (cache == null) {
            try (InputStream in = getServletContext().getResourceAsStream("/css/logo-umg.png")) {
                if (in != null) {
                    cache = in.readAllBytes();
                    logoCache = cache;
                }
            } catch (IOException ex) {
                cache = null;
            }
        }
        return cache;
    }
}
