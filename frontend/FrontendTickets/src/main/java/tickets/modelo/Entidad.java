package tickets.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Metadatos de una tabla del modelo y de las operaciones SOAP que la gestionan. */
public final class Entidad {

    private final String clave;
    private final String titulo;
    private final String singular;
    private final String servicio;
    private String icono = "";
    private String descripcion = "";
    private String paramId;
    private String claveId = "ID";
    private String claveEtiqueta = "ID";
    private String opListar;
    private String opPorId;
    private String opInsertar;
    private String opActualizar;
    private String opEliminar;
    private String textoEliminar = "Eliminar";
    private final List<Campo> campos = new ArrayList<>();

    public Entidad(String clave, String titulo, String singular, String servicio) {
        this.clave = clave;
        this.titulo = titulo;
        this.singular = singular;
        this.servicio = servicio;
    }

    public Entidad icono(String i) { this.icono = i; return this; }
    public Entidad descripcion(String d) { this.descripcion = d; return this; }
    public Entidad id(String paramId) { this.paramId = paramId; return this; }
    public Entidad claveId(String k) { this.claveId = k; return this; }
    public Entidad etiqueta(String k) { this.claveEtiqueta = k; return this; }
    public Entidad listar(String op) { this.opListar = op; return this; }
    public Entidad porId(String op) { this.opPorId = op; return this; }
    public Entidad insertar(String op) { this.opInsertar = op; return this; }
    public Entidad actualizar(String op) { this.opActualizar = op; return this; }
    public Entidad eliminar(String op, String texto) { this.opEliminar = op; this.textoEliminar = texto; return this; }
    public Entidad campo(Campo c) { this.campos.add(c); return this; }

    public String getClave() { return clave; }
    public String getTitulo() { return titulo; }
    public String getSingular() { return singular; }
    public String getServicio() { return servicio; }
    public String getIcono() { return icono; }
    public String getDescripcion() { return descripcion; }
    public String getParamId() { return paramId; }
    public String getClaveId() { return claveId; }
    public String getClaveEtiqueta() { return claveEtiqueta; }
    public String getOpListar() { return opListar; }
    public String getOpPorId() { return opPorId; }
    public String getOpInsertar() { return opInsertar; }
    public String getOpActualizar() { return opActualizar; }
    public String getOpEliminar() { return opEliminar; }
    public String getTextoEliminar() { return textoEliminar; }
    public List<Campo> getCampos() { return Collections.unmodifiableList(campos); }

    public boolean isPuedeCrear() { return opInsertar != null; }
    public boolean isPuedeEditar() { return opActualizar != null; }
    public boolean isPuedeEliminar() { return opEliminar != null; }
}
