package tickets.modelo;

import java.util.List;
import java.util.Map;

/** Una fila lista para mostrar en una tabla HTML. */
public final class FilaVista {
    private final String id;
    private final Map<String, String> datos;
    private final List<String> celdas;

    public FilaVista(String id, Map<String, String> datos, List<String> celdas) {
        this.id = id;
        this.datos = datos;
        this.celdas = celdas;
    }

    public String getId() { return id; }
    public Map<String, String> getDatos() { return datos; }
    public List<String> getCeldas() { return celdas; }
}
