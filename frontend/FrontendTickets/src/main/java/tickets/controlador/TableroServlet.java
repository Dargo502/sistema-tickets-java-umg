package tickets.controlador;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tickets.modelo.ColumnaTablero;
import tickets.modelo.Entidad;
import tickets.modelo.Entidades;
import tickets.modelo.Opcion;
import tickets.servicio.CrudService;
import tickets.soap.SoapClient;
import tickets.soap.SoapException;
import tickets.util.Filas;

/**
 * Tablero de la cola FIFO: una columna por estado (Solicitado → En Proceso → Finalizado) con los
 * tickets en estricto orden de llegada. Permite reclamar, avanzar de estado y eliminar.
 * Las reglas (no saltar estados, no doble reclamo) las valida la base de datos mediante triggers.
 */
@WebServlet(name = "TableroServlet", urlPatterns = {"/tablero"})
public class TableroServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final String SERVICIO = "WSTickets";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<ColumnaTablero> columnas = new ArrayList<>();
        List<Opcion> especialistas = new ArrayList<>();
        boolean avisoParche = false;
        try {
            List<Map<String, String>> estados = CrudService.filas(Entidades.obtener("estados"));
            List<Map<String, String>> tickets = CrudService.filas(Entidades.obtener("tickets"));
            List<Map<String, String>> usuarios = CrudService.filas(Entidades.obtener("usuarios"));

            columnas = armarColumnas(estados);
            avisoParche = distribuir(columnas, tickets);
            especialistas = especialistas(usuarios);
        } catch (SoapException ex) {
            aviso(req, "error", ex.getMessage());
        }
        req.setAttribute("columnas", columnas);
        req.setAttribute("especialistas", especialistas);
        req.setAttribute("avisoParche", avisoParche);
        mostrar(req, resp, "tablero.jsp", "Tablero FIFO");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String accion = param(req, "accion");
        String idTicket = param(req, "idTicket");
        try {
            if (!Filas.esEnteroPositivo(idTicket)) {
                throw new IllegalArgumentException("ID de ticket no válido.");
            }
            Map<String, String> p = new LinkedHashMap<>();
            p.put("idTicket", idTicket);
            String mensaje;
            switch (accion) {
                case "reclamar":
                    String esp = param(req, "idEspecialista");
                    if (!Filas.esEnteroPositivo(esp)) {
                        throw new IllegalArgumentException("Seleccione un especialista.");
                    }
                    p.put("idEspecialista", esp);
                    mensaje = SoapClient.invocarTexto(SERVICIO, "reclamarTicket", p);
                    break;
                case "avanzar":
                    String estado = param(req, "idNuevoEstado");
                    if (!Filas.esEnteroPositivo(estado)) {
                        throw new IllegalArgumentException("El ticket ya está en el último estado.");
                    }
                    p.put("idNuevoEstado", estado);
                    mensaje = SoapClient.invocarTexto(SERVICIO, "cambiarEstadoTicket", p);
                    break;
                case "eliminar":
                    mensaje = CrudService.eliminar(Entidades.obtener("tickets"), idTicket);
                    break;
                default:
                    throw new IllegalArgumentException("Acción no reconocida.");
            }
            flash(req, tipoDe(mensaje), mensaje);
        } catch (IllegalArgumentException | SoapException ex) {
            flash(req, "error", ex.getMessage());
        }
        redirigir(req, resp, "/tablero");
    }

    // ------------------------------------------------------------------ lógica del tablero

    static List<ColumnaTablero> armarColumnas(List<Map<String, String>> estados) {
        List<ColumnaTablero> columnas = new ArrayList<>();
        for (Map<String, String> e : estados) {
            int orden = entero(e.get("Orden"), 999);
            columnas.add(new ColumnaTablero(e.get("ID"), e.get("Estado"), orden));
        }
        columnas.sort(Comparator.comparingInt(ColumnaTablero::getOrden));
        return columnas;
    }

    /**
     * Reparte los tickets por estado y calcula las acciones permitidas para cada uno.
     * @return true si el backend no devuelve el ID de los tickets (falta el parche).
     */
    static boolean distribuir(List<ColumnaTablero> columnas, List<Map<String, String>> tickets) {
        boolean sinId = false;
        for (Map<String, String> t : tickets) {
            ColumnaTablero actual = null;
            int indice = -1;
            for (int i = 0; i < columnas.size(); i++) {
                if (columnas.get(i).getNombre() != null && columnas.get(i).getNombre().equals(t.get("Estado"))) {
                    actual = columnas.get(i);
                    indice = i;
                }
            }
            if (actual == null) {
                continue;
            }
            boolean tieneId = !Filas.vacio(t.get("ID"));
            sinId |= !tieneId;
            ColumnaTablero siguiente = indice + 1 < columnas.size() ? columnas.get(indice + 1) : null;
            boolean especialistaConocido = t.containsKey("Especialista");
            boolean reclamado = especialistaConocido && !Filas.vacio(t.get("Especialista"));
            boolean primera = indice == 0;

            t.put("_siguienteId", siguiente == null || siguiente.getId() == null ? "" : siguiente.getId());
            t.put("_siguienteNombre", siguiente == null ? "" : siguiente.getNombre());
            t.put("_puedeReclamar", String.valueOf(tieneId && siguiente != null && primera && !reclamado));
            t.put("_puedeAvanzar", String.valueOf(tieneId && siguiente != null
                    && (!primera || reclamado || !especialistaConocido)));
            t.put("_puedeEliminar", String.valueOf(tieneId));
            actual.getTickets().add(t);
        }
        return sinId;
    }

    static List<Opcion> especialistas(List<Map<String, String>> usuarios) {
        List<Opcion> soloEspecialistas = new ArrayList<>();
        List<Opcion> todos = new ArrayList<>();
        for (Map<String, String> u : usuarios) {
            String id = u.get("ID");
            if (Filas.vacio(id)) {
                continue;
            }
            String nombre = u.getOrDefault("Nombre", "Usuario " + id);
            String user = u.get("User");
            Opcion o = new Opcion(id, Filas.vacio(user) ? nombre : nombre + " (" + user + ")");
            todos.add(o);
            String rol = u.get("Rol");
            if (rol != null && rol.toLowerCase().contains("especialista")) {
                soloEspecialistas.add(o);
            }
        }
        return soloEspecialistas.isEmpty() ? todos : soloEspecialistas;
    }

    private static int entero(String s, int porDefecto) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return porDefecto;
        }
    }
}
