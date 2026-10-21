import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BACKEND SIMULADO (solo para desarrollo y pruebas, NO es parte de la entrega).
 * Imita los servicios SOAP de BackendTickets con datos en memoria y los mismos formatos de texto,
 * para poder probar la aplicación web sin SQL Server ni GlassFish.
 *
 *   Uso:   java MockBackend.java            (puerto 8081, con el parche de WSTickets)
 *          java MockBackend.java 8081 sinparche   (simula el backend ORIGINAL sin parche)
 *   Luego en la web (pantalla Configuración): http://localhost:8081/BackendTickets
 */
public class MockBackend {

    static boolean sinParche = false;
    static final List<Map<String, String>> empresas = new ArrayList<>(), roles = new ArrayList<>(),
            usuarios = new ArrayList<>(), categorias = new ArrayList<>(), estados = new ArrayList<>(),
            prioridades = new ArrayList<>(), tickets = new ArrayList<>(), historial = new ArrayList<>(),
            notifs = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 8081;
        sinParche = args.length > 1 && args[1].equalsIgnoreCase("sinparche");
        sembrar();
        HttpServer server = HttpServer.create(new InetSocketAddress(puerto), 0);
        for (String s : new String[]{"WSTickets", "WSUsuarios", "WSEmpresas", "WSRoles", "WSCategorias",
                "WSEstados", "WSPrioridades", "WSHistorial", "WSNotificaciones"}) {
            server.createContext("/BackendTickets/" + s, ex -> atender(s, ex));
        }
        server.start();
        System.out.println("MockBackend en http://localhost:" + puerto + "/BackendTickets  (sinParche=" + sinParche + ")");
    }

    // ---------------------------------------------------------------- HTTP / SOAP
    static void atender(String servicio, HttpExchange ex) throws IOException {
        String cuerpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("Body[^>]*>\\s*<(?:[A-Za-z0-9]+:)?([A-Za-z0-9_]+)").matcher(cuerpo);
        String respuesta;
        int codigo = 200;
        if (!ex.getRequestMethod().equals("POST") || !m.find()) {
            respuesta = fault("Solicitud SOAP inválida");
            codigo = 500;
        } else {
            String op = m.group(1);
            Map<String, String> p = new LinkedHashMap<>();
            Matcher mp = Pattern.compile("<([A-Za-z0-9_]+)>([^<]*)</\\1>").matcher(cuerpo.substring(m.end()));
            while (mp.find()) {
                p.put(mp.group(1), desescapar(mp.group(2)));
            }
            List<String> r = ejecutar(servicio, op, p);
            if (r == null) {
                respuesta = fault("Unexpected operation: " + op);
                codigo = 500;
            } else {
                StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" ?><S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\"><S:Body><ns2:"
                        + op + "Response xmlns:ns2=\"http://servicios/\">");
                for (String x : r) {
                    sb.append("<return>").append(escapar(x)).append("</return>");
                }
                respuesta = sb.append("</ns2:").append(op).append("Response></S:Body></S:Envelope>").toString();
            }
        }
        byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/xml;charset=utf-8");
        ex.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    static String fault(String texto) {
        return "<?xml version=\"1.0\" ?><S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\"><S:Body><S:Fault>"
                + "<faultcode>S:Server</faultcode><faultstring>" + escapar(texto) + "</faultstring></S:Fault></S:Body></S:Envelope>";
    }

    static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static String desescapar(String s) {
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&");
    }

    // ---------------------------------------------------------------- datos
    static Map<String, String> fila(String... kv) {
        Map<String, String> f = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            f.put(kv[i], kv[i + 1]);
        }
        return f;
    }

    static String sig(List<Map<String, String>> t) {
        int max = 0;
        for (Map<String, String> f : t) {
            max = Math.max(max, Integer.parseInt(f.get("id")));
        }
        return String.valueOf(max + 1);
    }

    static Map<String, String> buscar(List<Map<String, String>> t, String id) {
        for (Map<String, String> f : t) {
            if (f.get("id").equals(id)) {
                return f;
            }
        }
        return null;
    }

    static String nombre(List<Map<String, String>> t, String id, String campo) {
        Map<String, String> f = id == null ? null : buscar(t, id);
        return f == null ? "" : f.get(campo);
    }

    static void sembrar() {
        empresas.add(fila("id", "1", "nombre", "Empresa Demo S.A.", "dir", "Zona 1, Ciudad", "tel", "22223333", "activo", "1"));
        roles.add(fila("id", "1", "nombre", "Solicitante"));
        roles.add(fila("id", "2", "nombre", "Especialista"));
        roles.add(fila("id", "3", "nombre", "Administrador"));
        usuarios.add(fila("id", "1", "idEmpresa", "1", "idRol", "1", "nombre", "Ana Solicitante", "user", "ana", "pass", "1234", "correo", "ana@demo.com", "activo", "1"));
        usuarios.add(fila("id", "2", "idEmpresa", "1", "idRol", "2", "nombre", "Luis Especialista", "user", "luis", "pass", "1234", "correo", "luis@demo.com", "activo", "1"));
        usuarios.add(fila("id", "3", "idEmpresa", "1", "idRol", "3", "nombre", "Admin General", "user", "admin", "pass", "1234", "correo", "admin@demo.com", "activo", "1"));
        categorias.add(fila("id", "1", "nombre", "Hardware", "prefijo", "HD", "consec", "0"));
        categorias.add(fila("id", "2", "nombre", "Software", "prefijo", "SW", "consec", "0"));
        categorias.add(fila("id", "3", "nombre", "Redes", "prefijo", "NW", "consec", "0"));
        estados.add(fila("id", "1", "nombre", "Solicitado", "orden", "1"));
        estados.add(fila("id", "2", "nombre", "En Proceso", "orden", "2"));
        estados.add(fila("id", "3", "nombre", "Finalizado", "orden", "3"));
        prioridades.add(fila("id", "1", "nombre", "Alta", "peso", "1"));
        prioridades.add(fila("id", "2", "nombre", "Media", "peso", "2"));
        prioridades.add(fila("id", "3", "nombre", "Baja", "peso", "3"));
    }

    static String ahora() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }

    // ---------------------------------------------------------------- operaciones
    static List<String> uno(String s) {
        List<String> l = new ArrayList<>();
        l.add(s);
        return l;
    }

    static List<String> ejecutar(String svc, String op, Map<String, String> p) {
        switch (svc) {
            case "WSEmpresas": return empresasOp(op, p);
            case "WSRoles": return rolesOp(op, p);
            case "WSUsuarios": return usuariosOp(op, p);
            case "WSCategorias": return catalogoOp(categorias, "Categoría", op, p);
            case "WSEstados": return catalogoOp(estados, "Estado", op, p);
            case "WSPrioridades": return catalogoOp(prioridades, "Prioridad", op, p);
            case "WSTickets": return ticketsOp(op, p);
            case "WSHistorial": return historialOp(op, p);
            case "WSNotificaciones": return notifOp(op, p);
            default: return null;
        }
    }

    static List<String> empresasOp(String op, Map<String, String> p) {
        List<String> r = new ArrayList<>();
        switch (op) {
            case "insertarEmpresa":
                empresas.add(fila("id", sig(empresas), "nombre", p.get("nombreEmpresa"), "dir", p.get("direccion"), "tel", p.get("telefono"), "activo", "1"));
                return uno("¡Empresa insertada correctamente!");
            case "actualizarEmpresa": {
                Map<String, String> e = buscar(empresas, p.get("idEmpresa"));
                if (e == null) return uno("No se encontró la empresa con ese ID.");
                e.put("nombre", p.get("nuevoNombre")); e.put("dir", p.get("nuevaDireccion")); e.put("tel", p.get("nuevoTelefono"));
                return uno("¡Empresa actualizada!");
            }
            case "desactivarEmpresa": {
                Map<String, String> e = buscar(empresas, p.get("idEmpresa"));
                if (e == null) return uno("No se encontró la empresa.");
                e.put("activo", "0");
                return uno("¡Empresa desactivada correctamente!");
            }
            case "seleccionarEmpresas":
                for (Map<String, String> e : empresas) if (e.get("activo").equals("1"))
                    r.add("ID: " + e.get("id") + " | Nombre: " + e.get("nombre") + " | Tel: " + e.get("tel"));
                return r;
            case "seleccionarEmpresaPorId": {
                Map<String, String> e = buscar(empresas, p.get("idEmpresa"));
                if (e == null) return uno("No se encontró ninguna empresa con el ID: " + p.get("idEmpresa"));
                return uno("ID: " + e.get("id") + " | Nombre: " + e.get("nombre") + " | Dir: " + e.get("dir") + " | Tel: " + e.get("tel") + " | Activa: " + e.get("activo").equals("1"));
            }
            default: return null;
        }
    }

    static List<String> rolesOp(String op, Map<String, String> p) {
        List<String> r = new ArrayList<>();
        switch (op) {
            case "insertarRol":
                roles.add(fila("id", sig(roles), "nombre", p.get("nombreRol")));
                return uno("¡Rol insertado correctamente!");
            case "actualizarRol": {
                Map<String, String> e = buscar(roles, p.get("idRol"));
                if (e == null) return uno("No se encontró el Rol con ese ID.");
                e.put("nombre", p.get("nuevoNombre"));
                return uno("¡Rol actualizado!");
            }
            case "eliminarRol": {
                Map<String, String> e = buscar(roles, p.get("idRol"));
                if (e == null) return uno("No se encontró el Rol con ese ID.");
                for (Map<String, String> u : usuarios) if (u.get("idRol").equals(e.get("id")))
                    return uno("Error: The DELETE statement conflicted with the REFERENCE constraint \"FK_Usuario_Rol\".");
                roles.remove(e);
                return uno("¡Rol eliminado!");
            }
            case "seleccionarTodos":
                for (Map<String, String> e : roles) r.add("ID: " + e.get("id") + " | Rol: " + e.get("nombre"));
                return r;
            case "seleccionarPorId": {
                Map<String, String> e = buscar(roles, p.get("idRol"));
                return uno(e == null ? "No se encontró ningún rol con el ID: " + p.get("idRol") : "ID: " + e.get("id") + " | Rol: " + e.get("nombre"));
            }
            default: return null;
        }
    }

    static List<String> usuariosOp(String op, Map<String, String> p) {
        List<String> r = new ArrayList<>();
        switch (op) {
            case "insertarUsuario":
                for (Map<String, String> u : usuarios) if (u.get("user").equals(p.get("nombreUsuario")))
                    return uno("Error al insertar: Violation of UNIQUE KEY constraint. Cannot insert duplicate key (" + p.get("nombreUsuario") + ").");
                usuarios.add(fila("id", sig(usuarios), "idEmpresa", p.get("idEmpresa"), "idRol", p.get("idRol"), "nombre", p.get("nombreCompleto"),
                        "user", p.get("nombreUsuario"), "pass", p.get("password"), "correo", p.get("correo"), "activo", "1"));
                return uno("¡Usuario insertado correctamente!");
            case "actualizarUsuario": {
                Map<String, String> u = buscar(usuarios, p.get("idUsuario"));
                if (u == null) return uno("No se encontró el usuario con ese ID.");
                u.put("idEmpresa", p.get("idEmpresa")); u.put("idRol", p.get("idRol")); u.put("nombre", p.get("nombreCompleto"));
                u.put("user", p.get("nombreUsuario")); u.put("correo", p.get("correo"));
                return uno("¡Usuario actualizado!");
            }
            case "desactivarUsuario": {
                Map<String, String> u = buscar(usuarios, p.get("idUsuario"));
                if (u == null) return uno("No se encontró el usuario.");
                u.put("activo", "0");
                return uno("¡Usuario desactivado correctamente!");
            }
            case "seleccionarUsuarios":
                for (Map<String, String> u : usuarios) if (u.get("activo").equals("1"))
                    r.add("ID: " + u.get("id") + " | Nombre: " + u.get("nombre") + " | User: " + u.get("user") + " | Rol: " + nombre(roles, u.get("idRol"), "nombre"));
                return r;
            case "seleccionarUsuarioPorId": {
                Map<String, String> u = buscar(usuarios, p.get("idUsuario"));
                if (u == null) return uno("No se encontró ningún usuario con el ID: " + p.get("idUsuario"));
                return uno("ID: " + u.get("id") + " | Nombre: " + u.get("nombre") + " | User: " + u.get("user") + " | Correo: " + u.get("correo"));
            }
            default: return null;
        }
    }

    /** Categorías, estados y prioridades comparten forma: (nombre, número). */
    static List<String> catalogoOp(List<Map<String, String>> t, String etiqueta, String op, Map<String, String> p) {
        String num = t == categorias ? "prefijo" : t == estados ? "orden" : "peso";
        String etNum = t == categorias ? "Prefijo" : t == estados ? "Orden" : "Peso";
        List<String> r = new ArrayList<>();
        if (op.startsWith("insertar")) {
            Map<String, String> f = fila("id", sig(t), "nombre", p.values().iterator().next(), num, new ArrayList<>(p.values()).get(1));
            if (t == categorias) f.put("consec", "0");
            t.add(f);
            return uno("¡" + etiqueta + " insertado correctamente!");
        }
        String idKey = new ArrayList<>(p.keySet()).isEmpty() ? "" : new ArrayList<>(p.keySet()).get(0);
        if (op.startsWith("actualizar")) {
            Map<String, String> f = buscar(t, p.get(idKey));
            if (f == null) return uno("No se encontró el registro.");
            List<String> v = new ArrayList<>(p.values());
            f.put("nombre", v.get(1)); f.put(num, v.get(2));
            return uno("¡" + etiqueta + " actualizado!");
        }
        if (op.startsWith("eliminar")) {
            Map<String, String> f = buscar(t, p.get(idKey));
            if (f == null) return uno("No se encontró el registro.");
            boolean enUso = false;
            for (Map<String, String> tk : tickets) {
                enUso |= (t == categorias && tk.get("idCategoria").equals(f.get("id")))
                        || (t == estados && tk.get("idEstado").equals(f.get("id")))
                        || (t == prioridades && tk.get("idPrioridad").equals(f.get("id")));
            }
            if (enUso) return uno("Error (puede estar en uso): The DELETE statement conflicted with the REFERENCE constraint.");
            t.remove(f);
            return uno("¡" + etiqueta + " eliminado!");
        }
        if (op.startsWith("seleccionar")) {
            for (Map<String, String> f : t) {
                String s = "ID: " + f.get("id") + " | " + etiqueta + ": " + f.get("nombre") + " | " + etNum + ": " + f.get(num);
                if (t == categorias) s += " | Consecutivo actual: " + f.get("consec");
                r.add(s);
            }
            return r;
        }
        return null;
    }

    static String lineaTicket(Map<String, String> t) {
        String s = (sinParche ? "" : "ID: " + t.get("id") + " | ")
                + "Folio: " + t.get("folio") + " | Título: " + t.get("titulo")
                + " | Cat: " + nombre(categorias, t.get("idCategoria"), "nombre")
                + " | Prio: " + nombre(prioridades, t.get("idPrioridad"), "nombre")
                + " | Estado: " + nombre(estados, t.get("idEstado"), "nombre")
                + " | Solicitante: " + nombre(usuarios, t.get("idSolicitante"), "nombre");
        if (!sinParche) s += " | Especialista: " + nombre(usuarios, t.get("idEspecialista"), "nombre");
        return s;
    }

    static void log(Map<String, String> t, String ant, String nuevo, String usuario, String nota) {
        historial.add(fila("idTicket", t.get("id"), "ant", ant, "nuevo", nuevo, "usuario", usuario, "nota", nota, "fecha", ahora()));
    }

    static List<String> ticketsOp(String op, Map<String, String> p) {
        List<String> r = new ArrayList<>();
        switch (op) {
            case "insertarTicket": {
                Map<String, String> c = buscar(categorias, p.get("idCategoria"));
                if (c == null || buscar(usuarios, p.get("idSolicitante")) == null)
                    return uno("Error al crear ticket: The INSERT statement conflicted with the FOREIGN KEY constraint.");
                c.put("consec", String.valueOf(Integer.parseInt(c.get("consec")) + 1));
                String id = sig(tickets);
                Map<String, String> t = fila("id", id, "folio", c.get("prefijo") + "-" + String.format("%03d", Integer.parseInt(c.get("consec"))),
                        "titulo", p.get("titulo"), "desc", p.get("descripcion"), "idCategoria", p.get("idCategoria"),
                        "idPrioridad", p.get("idPrioridad"), "idEstado", "1", "idSolicitante", p.get("idSolicitante"), "idEspecialista", "", "eliminado", "0");
                tickets.add(t);
                log(t, null, "Solicitado", nombre(usuarios, p.get("idSolicitante"), "nombre"), "Ticket creado e ingresado a la cola FIFO");
                return uno("¡Ticket creado exitosamente! (Folio e Historial generados por SQL)");
            }
            case "reclamarTicket": {
                Map<String, String> t = buscar(tickets, p.get("idTicket"));
                if (t == null) return uno("No se encontró el ticket.");
                if (!t.get("idEspecialista").isEmpty())
                    return uno("Error al reclamar (o ya fue reclamado): Este ticket ya fue reclamado por otro especialista.");
                t.put("idEspecialista", p.get("idEspecialista"));
                notifs.add(fila("id", sig(notifs), "idEsp", p.get("idEspecialista"), "folio", t.get("folio"), "leida", "0", "fecha", ahora(),
                        "msg", "Incidente " + t.get("folio") + " reclamado por especialista"));
                return uno("¡Ticket reclamado exitosamente!");
            }
            case "cambiarEstadoTicket": {
                Map<String, String> t = buscar(tickets, p.get("idTicket"));
                if (t == null) return uno("Error (Transición inválida o ticket no existe): ticket inexistente");
                int viejo = Integer.parseInt(nombre(estados, t.get("idEstado"), "orden"));
                String nuevoOrden = nombre(estados, p.get("idNuevoEstado"), "orden");
                if (nuevoOrden.isEmpty() || Integer.parseInt(nuevoOrden) != viejo + 1)
                    return uno("Error (Transición inválida o ticket no existe): Transicion de estado invalida: solo se permite avanzar un paso (Solicitado -> En Proceso -> Finalizado).");
                String antes = nombre(estados, t.get("idEstado"), "nombre");
                t.put("idEstado", p.get("idNuevoEstado"));
                log(t, antes, nombre(estados, p.get("idNuevoEstado"), "nombre"), nombre(usuarios, t.get("idEspecialista").isEmpty() ? t.get("idSolicitante") : t.get("idEspecialista"), "nombre"), "Cambio de estado de ticket");
                return uno("¡Estado actualizado!");
            }
            case "eliminarTicket": {
                Map<String, String> t = buscar(tickets, p.get("idTicket"));
                if (t == null) return uno("No se encontró el ticket.");
                t.put("eliminado", "1");
                return uno("¡Ticket eliminado de la vista!");
            }
            case "seleccionarTickets":
                for (Map<String, String> t : tickets) if (t.get("eliminado").equals("0")) r.add(lineaTicket(t));
                return r;
            case "seleccionarTicketPorId": {
                if (sinParche) return null;
                Map<String, String> t = buscar(tickets, p.get("idTicket"));
                if (t == null || t.get("eliminado").equals("1")) return uno("No se encontró ningún ticket con el ID: " + p.get("idTicket"));
                return uno(lineaTicket(t) + " | Descripción: " + t.get("desc"));
            }
            case "actualizarTicket": {
                if (sinParche) return null;
                Map<String, String> t = buscar(tickets, p.get("idTicket"));
                if (t == null) return uno("No se encontró el ticket.");
                t.put("titulo", p.get("titulo")); t.put("desc", p.get("descripcion"));
                t.put("idCategoria", p.get("idCategoria")); t.put("idPrioridad", p.get("idPrioridad"));
                return uno("¡Ticket actualizado!");
            }
            default: return null;
        }
    }

    static List<String> historialOp(String op, Map<String, String> p) {
        if (!op.equals("seleccionarHistorialPorTicket")) return null;
        List<String> r = new ArrayList<>();
        for (Map<String, String> h : historial) if (h.get("idTicket").equals(p.get("idTicket")))
            r.add("Fecha: " + h.get("fecha") + " | Usuario: " + h.get("usuario") + " | De: " + (h.get("ant") == null ? "Ninguno (Nuevo)" : h.get("ant"))
                    + " -> A: " + h.get("nuevo") + " | Nota: " + h.get("nota"));
        return r;
    }

    static List<String> notifOp(String op, Map<String, String> p) {
        List<String> r = new ArrayList<>();
        if (op.equals("seleccionarNotificacionesPendientes")) {
            for (Map<String, String> n : notifs) if (n.get("idEsp").equals(p.get("idEspecialista")) && n.get("leida").equals("0"))
                r.add("ID Notif: " + n.get("id") + " | Ticket: " + n.get("folio") + " | Fecha: " + n.get("fecha") + " | Alerta: " + n.get("msg"));
            return r;
        }
        if (op.equals("marcarNotificacionLeida")) {
            Map<String, String> n = buscar(notifs, p.get("idNotificacion"));
            if (n == null) return uno("No se encontró la notificación.");
            n.put("leida", "1");
            return uno("¡Notificación marcada como leída!");
        }
        return null;
    }
}
