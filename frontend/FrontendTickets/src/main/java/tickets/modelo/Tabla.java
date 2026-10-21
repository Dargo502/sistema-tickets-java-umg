package tickets.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Resultado tabular: columnas dinámicas (según lo que devuelva el servicio) y filas. */
public final class Tabla {
    private final List<String> columnas;
    private final List<FilaVista> filas;
    private String aviso = "";

    public Tabla(List<String> columnas, List<FilaVista> filas) {
        this.columnas = columnas;
        this.filas = filas;
    }

    public static Tabla vacia() {
        return new Tabla(Collections.<String>emptyList(), new ArrayList<FilaVista>());
    }

    public Tabla conAviso(String texto) { this.aviso = texto; return this; }

    public List<String> getColumnas() { return columnas; }
    public List<FilaVista> getFilas() { return filas; }
    public String getAviso() { return aviso; }
}
