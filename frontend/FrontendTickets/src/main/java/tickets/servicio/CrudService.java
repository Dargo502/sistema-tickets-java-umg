package tickets.servicio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import tickets.modelo.Campo;
import tickets.modelo.Entidad;
import tickets.modelo.Entidades;
import tickets.modelo.FilaVista;
import tickets.modelo.Opcion;
import tickets.modelo.Tabla;
import tickets.soap.SoapClient;
import tickets.soap.SoapException;
import tickets.util.Filas;

/**
 * Lógica de negocio de la capa web (sin dependencias de Servlet, para poder probarla):
 * lee, valida y guarda registros hablando únicamente con los servicios SOAP.
 * La aplicación web NUNCA accede a la base de datos directamente.
 */
public final class CrudService {

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern ENTERO = Pattern.compile("^-?[0-9]{1,9}$");

    private CrudService() { }

    // ------------------------------------------------------------------ lectura

    /** Todas las filas de la entidad como mapas {clave -> valor}. */
    public static List<Map<String, String>> filas(Entidad e) throws SoapException {
        List<String> crudas = SoapClient.invocar(e.getServicio(), e.getOpListar(), null);
        if (Filas.esError(crudas)) {
            throw new SoapException(crudas.get(0));
        }
        List<Map<String, String>> resultado = new ArrayList<>();
        for (String linea : crudas) {
            resultado.add(Filas.parsear(linea));
        }
        return resultado;
    }

    public static Tabla listar(Entidad e) throws SoapException {
        return construirTabla(filas(e), e.getClaveId());
    }

    /** Filtra por ID: usa la operación "por ID" del servicio y, si no existe, filtra la lista. */
    public static Tabla buscarPorId(Entidad e, String id) throws SoapException {
        if (!Filas.esEnteroPositivo(id)) {
            throw new IllegalArgumentException("El ID debe ser un número entero positivo.");
        }
        Map<String, String> fila = obtener(e, id);
        if (fila == null) {
            return Tabla.vacia().conAviso("No se encontró ningún registro con el ID " + id + ".");
        }
        List<Map<String, String>> unica = new ArrayList<>();
        unica.add(fila);
        return construirTabla(unica, e.getClaveId());
    }

    /**
     * Devuelve el registro con ese ID (o null si no existe). Combina la fila de la lista con
     * el detalle de la operación "por ID", porque cada una expone campos distintos.
     */
    public static Map<String, String> obtener(Entidad e, String id) throws SoapException {
        Map<String, String> combinado = null;
        for (Map<String, String> f : filas(e)) {
            if (id.equals(f.get(e.getClaveId()))) {
                combinado = new LinkedHashMap<>(f);
                break;
            }
        }
        if (e.getOpPorId() != null) {
            Map<String, String> p = new LinkedHashMap<>();
            p.put(e.getParamId(), id);
            String r = SoapClient.invocarTexto(e.getServicio(), e.getOpPorId(), p);
            if (Filas.esError(r)) {
                throw new SoapException(r);
            }
            if (Filas.noEncontrado(r) || Filas.vacio(r)) {
                return combinado;   // la lista podría tenerlo; si no, es null
            }
            Map<String, String> detalle = Filas.parsear(r);
            if (combinado == null) {
                combinado = new LinkedHashMap<>();
            }
            combinado.putAll(detalle);
        }
        return combinado;
    }

    public static List<Opcion> opciones(Entidad ref) throws SoapException {
        return opciones(ref, filas(ref));
    }

    /** Igual que opciones(ref) pero reutilizando filas ya leídas (evita llamar dos veces al servicio). */
    public static List<Opcion> opciones(Entidad ref, List<Map<String, String>> filas) {
        List<Opcion> r = new ArrayList<>();
        for (Map<String, String> f : filas) {
            String id = f.get(ref.getClaveId());
            if (Filas.vacio(id)) {
                continue;
            }
            String texto = f.get(ref.getClaveEtiqueta());
            r.add(new Opcion(id, Filas.vacio(texto) ? "ID " + id : texto));
        }
        return r;
    }

    /** Carga las opciones de todos los SELECT del formulario. Devuelve los errores por separado. */
    public static Map<String, List<Opcion>> opcionesDe(Entidad e, List<String> errores) {
        Map<String, List<Opcion>> mapa = new LinkedHashMap<>();
        for (Campo c : e.getCampos()) {
            if (c.getEntidadRef() == null) {
                continue;
            }
            Entidad ref = Entidades.obtener(c.getEntidadRef());
            try {
                mapa.put(c.getNombre(), opciones(ref));
            } catch (SoapException ex) {
                mapa.put(c.getNombre(), new ArrayList<Opcion>());
                errores.add("No se pudo cargar la lista de " + ref.getTitulo() + ": " + ex.getMessage());
            }
        }
        return mapa;
    }

    /** Convierte una fila del servicio en valores de formulario (campo -> valor). */
    public static Map<String, String> valoresFormulario(Entidad e, Map<String, String> fila,
                                                        Map<String, List<Opcion>> opciones) {
        Map<String, String> valores = new LinkedHashMap<>();
        for (Campo c : e.getCampos()) {
            String origen = c.getClavePrefill() == null ? null : fila.get(c.getClavePrefill());
            String valor = origen == null ? "" : origen;
            if (c.getEntidadRef() != null) {
                valor = resolverOpcion(opciones.get(c.getNombre()), valor);
            }
            if (c.getTipo() == Campo.Tipo.CLAVE) {
                valor = "";
            }
            valores.put(c.getNombre(), valor);
        }
        return valores;
    }

    private static String resolverOpcion(List<Opcion> lista, String valor) {
        if (lista == null || Filas.vacio(valor)) {
            return "";
        }
        for (Opcion o : lista) {
            if (o.getTexto().equalsIgnoreCase(valor)) {
                return o.getId();
            }
        }
        for (Opcion o : lista) {
            if (o.getId().equals(valor)) {
                return o.getId();
            }
        }
        return "";
    }

    // ------------------------------------------------------------------ escritura

    /** Valida los valores del formulario. Devuelve la lista de mensajes de error (vacía si todo OK). */
    public static List<String> validar(Entidad e, Map<String, String> valores, boolean edicion) {
        List<String> errores = new ArrayList<>();
        for (Campo c : e.getCampos()) {
            if (edicion && c.isSoloInsertar()) {
                continue;
            }
            String v = valores.get(c.getNombre());
            v = v == null ? "" : v.trim();
            if (v.isEmpty()) {
                if (c.isRequerido()) {
                    errores.add("El campo \"" + c.getEtiqueta() + "\" es obligatorio.");
                }
                continue;
            }
            if (v.length() > c.getMax()) {
                errores.add("\"" + c.getEtiqueta() + "\" admite máximo " + c.getMax() + " caracteres.");
            }
            switch (c.getTipo()) {
                case NUMERO:
                    if (!ENTERO.matcher(v).matches()) {
                        errores.add("\"" + c.getEtiqueta() + "\" debe ser un número entero.");
                    }
                    break;
                case SELECT:
                    if (!Filas.esEnteroPositivo(v)) {
                        errores.add("Seleccione una opción válida en \"" + c.getEtiqueta() + "\".");
                    }
                    break;
                case CORREO:
                    if (!CORREO.matcher(v).matches()) {
                        errores.add("\"" + c.getEtiqueta() + "\" no es un correo válido.");
                    }
                    break;
                case CLAVE:
                    if (v.length() < 4) {
                        errores.add("\"" + c.getEtiqueta() + "\" debe tener al menos 4 caracteres.");
                    }
                    break;
                default:
                    break;
            }
        }
        return errores;
    }

    /** Inserta (id == null) o actualiza (id != null). Devuelve el mensaje textual del servicio. */
    public static String guardar(Entidad e, Map<String, String> valores, String id) throws SoapException {
        boolean edicion = id != null;
        String operacion = edicion ? e.getOpActualizar() : e.getOpInsertar();
        if (operacion == null) {
            throw new SoapException("Esta operación no está disponible para " + e.getTitulo() + ".");
        }
        Map<String, String> params = new LinkedHashMap<>();
        if (edicion) {
            params.put(e.getParamId(), id);
        }
        for (Campo c : e.getCampos()) {
            String param = edicion ? c.getParamActualizar() : c.getParamInsertar();
            if (param != null) {
                String v = valores.get(c.getNombre());
                params.put(param, v == null ? "" : v.trim());
            }
        }
        return SoapClient.invocarTexto(e.getServicio(), operacion, params);
    }

    public static String eliminar(Entidad e, String id) throws SoapException {
        if (e.getOpEliminar() == null) {
            throw new SoapException("Esta operación no está disponible para " + e.getTitulo() + ".");
        }
        if (!Filas.esEnteroPositivo(id)) {
            throw new IllegalArgumentException("ID no válido.");
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put(e.getParamId(), id);
        return SoapClient.invocarTexto(e.getServicio(), e.getOpEliminar(), params);
    }

    // ------------------------------------------------------------------ utilidades

    /** Arma una tabla cuyas columnas son la unión (en orden) de las claves de todas las filas. */
    public static Tabla construirTabla(List<Map<String, String>> filas, String claveId) {
        List<String> columnas = new ArrayList<>();
        for (Map<String, String> f : filas) {
            for (String k : f.keySet()) {
                if (!columnas.contains(k)) {
                    columnas.add(k);
                }
            }
        }
        List<FilaVista> vistas = new ArrayList<>();
        for (Map<String, String> f : filas) {
            List<String> celdas = new ArrayList<>();
            for (String col : columnas) {
                String v = f.get(col);
                celdas.add(v == null ? "" : v);
            }
            String id = claveId == null ? "" : f.get(claveId);
            vistas.add(new FilaVista(id == null ? "" : id, f, celdas));
        }
        return new Tabla(columnas, vistas);
    }
}
