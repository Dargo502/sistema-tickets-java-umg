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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import tickets.modelo.Campo;
import tickets.modelo.ColumnaTablero;
import tickets.modelo.Entidad;
import tickets.modelo.Entidades;
import tickets.modelo.Opcion;
import tickets.servicio.CrudService;
import tickets.soap.SoapClient;
import tickets.soap.SoapException;
import tickets.util.Filas;

/**
 * Tablero Kanban de la pantalla de inicio, alimentado por los servicios SOAP.
 *   GET  /inicio                      dibuja las columnas con los tickets reales
 *   POST /inicio  accion=crear        registra un ticket nuevo (WSTickets.insertarTicket)
 *   POST /inicio  accion=atender      reclama el ticket y lo pasa a "En Proceso"
 *   POST /inicio  accion=avanzar      mueve el ticket al siguiente estado
 * Todas las acciones POST responden con redirect (PRG) y un mensaje que muestra el encabezado.
 */
@WebServlet(name = "InicioServlet", urlPatterns = {"/inicio"})
public class InicioServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final String SERVICIO = "WSTickets";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<ColumnaTablero> columnas = new ArrayList<>();
        List<Opcion> categorias = new ArrayList<>();
        List<Opcion> prioridades = new ArrayList<>();
        List<Opcion> solicitantes = new ArrayList<>();
        List<Opcion> especialistas = new ArrayList<>();
        boolean avisoParche = false;
        try {
            // Las 5 consultas son independientes: se lanzan a la vez para que la pantalla cargue más rápido
            ExecutorService pool = Executors.newFixedThreadPool(5);
            List<Map<String, String>> estados, tickets, usuarios, listaCategorias, listaPrioridades;
            try {
                Future<List<Map<String, String>>> fEstados = leer(pool, "estados");
                Future<List<Map<String, String>>> fTickets = leer(pool, "tickets");
                Future<List<Map<String, String>>> fUsuarios = leer(pool, "usuarios");
                Future<List<Map<String, String>>> fCategorias = leer(pool, "categorias");
                Future<List<Map<String, String>>> fPrioridades = leer(pool, "prioridades");
                estados = esperar(fEstados);
                tickets = esperar(fTickets);
                usuarios = esperar(fUsuarios);
                listaCategorias = esperar(fCategorias);
                listaPrioridades = esperar(fPrioridades);
            } finally {
                pool.shutdown();
            }
            columnas = TableroServlet.armarColumnas(estados);
            avisoParche = TableroServlet.distribuir(columnas, tickets);
            especialistas = TableroServlet.especialistas(usuarios);
            solicitantes = CrudService.opciones(Entidades.obtener("usuarios"), usuarios);
            categorias = CrudService.opciones(Entidades.obtener("categorias"), listaCategorias);
            prioridades = CrudService.opciones(Entidades.obtener("prioridades"), listaPrioridades);
        } catch (SoapException ex) {
            aviso(req, "error", "No se pudo cargar el tablero: " + ex.getMessage());
        }
        req.setAttribute("columnas", columnas);
        req.setAttribute("categorias", categorias);
        req.setAttribute("prioridades", prioridades);
        req.setAttribute("solicitantes", solicitantes);
        req.setAttribute("especialistas", especialistas);
        req.setAttribute("avisoParche", avisoParche);
        mostrar(req, resp, "inicio.jsp", "Inicio");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String accion = param(req, "accion");
        try {
            switch (accion) {
                case "crear":
                    crear(req);
                    break;
                case "atender":
                    atender(req);
                    break;
                case "avanzar":
                    avanzar(req);
                    break;
                default:
                    flash(req, "error", "Acción no reconocida.");
            }
        } catch (IllegalArgumentException | SoapException ex) {
            flash(req, "error", ex.getMessage());
        }
        redirigir(req, resp, "/inicio");
    }

    // ------------------------------------------------------------------ acciones

    private void crear(HttpServletRequest req) throws SoapException {
        Entidad e = Entidades.obtener("tickets");
        Map<String, String> valores = new LinkedHashMap<>();
        for (Campo c : e.getCampos()) {
            valores.put(c.getNombre(), param(req, c.getNombre()));
        }
        List<String> errores = CrudService.validar(e, valores, false);
        if (!errores.isEmpty()) {
            flash(req, "error", String.join(" ", errores));
            return;
        }
        String mensaje = CrudService.guardar(e, valores, null);
        flash(req, tipoDe(mensaje), mensaje);
    }

    private void atender(HttpServletRequest req) throws SoapException {
        String idTicket = idValido(req, "idTicket", "ticket");
        String idEsp = idValido(req, "idEspecialista", "especialista");
        String idEstado = idValido(req, "idNuevoEstado", "estado");

        Map<String, String> reclamo = new LinkedHashMap<>();
        reclamo.put("idTicket", idTicket);
        reclamo.put("idEspecialista", idEsp);
        String r1 = SoapClient.invocarTexto(SERVICIO, "reclamarTicket", reclamo);
        if (!"ok".equals(tipoDe(r1))) {
            flash(req, "error", r1);
            return;
        }
        Map<String, String> cambio = new LinkedHashMap<>();
        cambio.put("idTicket", idTicket);
        cambio.put("idNuevoEstado", idEstado);
        String r2 = SoapClient.invocarTexto(SERVICIO, "cambiarEstadoTicket", cambio);
        if ("ok".equals(tipoDe(r2))) {
            flash(req, "ok", "Ticket reclamado y movido a la siguiente etapa.");
        } else if (yaEstaEnEtapa(idTicket, idEstado)) {
            flash(req, "ok", "El ticket ya estaba en esa etapa; no se repitió la acción.");
        } else {
            flash(req, "error", "El ticket se reclamó, pero no pudo avanzar: " + r2);
        }
    }

    private void avanzar(HttpServletRequest req) throws SoapException {
        String idTicket = idValido(req, "idTicket", "ticket");
        String idEstado = idValido(req, "idNuevoEstado", "estado");
        Map<String, String> p = new LinkedHashMap<>();
        p.put("idTicket", idTicket);
        p.put("idNuevoEstado", idEstado);
        String r = SoapClient.invocarTexto(SERVICIO, "cambiarEstadoTicket", p);
        if ("ok".equals(tipoDe(r))) {
            flash(req, "ok", "Ticket movido de etapa.");
        } else if (yaEstaEnEtapa(idTicket, idEstado)) {
            flash(req, "ok", "El ticket ya estaba en esa etapa; no se repitió la acción.");
        } else {
            flash(req, "error", r);
        }
    }

    /**
     * Si el cambio de estado falló, comprueba si el ticket YA está en la etapa pedida
     * (típico de un doble clic o de una página desactualizada). En ese caso no es un error real.
     */
    private boolean yaEstaEnEtapa(String idTicket, String idEstadoDestino) {
        try {
            Map<String, String> p = new LinkedHashMap<>();
            p.put("idTicket", idTicket);
            String r = SoapClient.invocarTexto(SERVICIO, "seleccionarTicketPorId", p);
            if (Filas.vacio(r) || Filas.esError(r) || Filas.noEncontrado(r)) {
                return false;
            }
            String estadoActual = Filas.parsear(r).get("Estado");
            if (estadoActual == null) {
                return false;
            }
            for (Map<String, String> e : CrudService.filas(Entidades.obtener("estados"))) {
                if (idEstadoDestino.equals(e.get("ID"))) {
                    return estadoActual.equals(e.get("Estado"));
                }
            }
        } catch (SoapException ignorado) {
            // si no se puede comprobar, se informa el error original
        }
        return false;
    }

    private static Future<List<Map<String, String>>> leer(ExecutorService pool, String claveEntidad) {
        Callable<List<Map<String, String>>> tarea = () -> CrudService.filas(Entidades.obtener(claveEntidad));
        return pool.submit(tarea);
    }

    private static <T> T esperar(Future<T> futuro) throws SoapException {
        try {
            return futuro.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof SoapException) {
                throw (SoapException) e.getCause();
            }
            throw new SoapException("Error interno al consultar los servicios: " + e.getCause(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SoapException("La consulta fue interrumpida.", e);
        }
    }

    private static String idValido(HttpServletRequest req, String nombre, String etiqueta) {
        String v = param(req, nombre);
        if (!Filas.esEnteroPositivo(v)) {
            throw new IllegalArgumentException("Dato no válido: " + etiqueta + ".");
        }
        return v;
    }
}
