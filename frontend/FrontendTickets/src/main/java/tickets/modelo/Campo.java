package tickets.modelo;

/** Describe un campo de formulario y cómo se envía al servicio SOAP. */
public final class Campo {

    public enum Tipo { TEXTO, NUMERO, CORREO, CLAVE, AREA, SELECT }

    private final String nombre;
    private final String etiqueta;
    private final Tipo tipo;
    private boolean requerido = true;
    private int max = 150;
    private String paramInsertar;
    private String paramActualizar;
    private String entidadRef;
    private String clavePrefill;

    public Campo(String nombre, String etiqueta, Tipo tipo) {
        this.nombre = nombre;
        this.etiqueta = etiqueta;
        this.tipo = tipo;
    }

    public Campo opcional() { this.requerido = false; return this; }
    public Campo max(int maximo) { this.max = maximo; return this; }
    /** Mismo nombre de parámetro SOAP para insertar y actualizar. */
    public Campo param(String p) { this.paramInsertar = p; this.paramActualizar = p; return this; }
    public Campo paramInsertar(String p) { this.paramInsertar = p; return this; }
    public Campo paramActualizar(String p) { this.paramActualizar = p; return this; }
    /** Para SELECT: clave de la entidad que provee las opciones. */
    public Campo ref(String clave) { this.entidadRef = clave; return this; }
    /** Clave de la respuesta del servicio usada para precargar el valor al editar. */
    public Campo prefill(String clave) { this.clavePrefill = clave; return this; }

    public String getNombre() { return nombre; }
    public String getEtiqueta() { return etiqueta; }
    public Tipo getTipo() { return tipo; }
    public String getTipoNombre() { return tipo.name(); }
    public boolean isRequerido() { return requerido; }
    public int getMax() { return max; }
    public String getParamInsertar() { return paramInsertar; }
    public String getParamActualizar() { return paramActualizar; }
    public String getEntidadRef() { return entidadRef; }
    public String getClavePrefill() { return clavePrefill; }
    /** Campo que solo existe al crear (p. ej. la contraseña o el solicitante). */
    public boolean isSoloInsertar() { return paramActualizar == null; }
}
