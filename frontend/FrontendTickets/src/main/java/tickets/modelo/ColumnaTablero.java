package tickets.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Columna del tablero FIFO (un estado y los tickets que contiene, en orden de llegada). */
public final class ColumnaTablero {
    private final String nombre;
    private final int orden;
    private final String id;
    private final List<Map<String, String>> tickets = new ArrayList<>();

    public ColumnaTablero(String id, String nombre, int orden) {
        this.id = id;
        this.nombre = nombre;
        this.orden = orden;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public int getOrden() { return orden; }
    public List<Map<String, String>> getTickets() { return tickets; }
}
