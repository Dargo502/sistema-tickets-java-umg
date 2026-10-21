package tickets.modelo;

/** Elemento de una lista desplegable (id para el servicio, texto para el usuario). */
public final class Opcion {
    private final String id;
    private final String texto;

    public Opcion(String id, String texto) {
        this.id = id;
        this.texto = texto;
    }

    public String getId() { return id; }
    public String getTexto() { return texto; }
}
