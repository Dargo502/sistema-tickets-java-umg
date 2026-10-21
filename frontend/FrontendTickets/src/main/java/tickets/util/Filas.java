package tickets.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilidades para interpretar las respuestas de texto del backend.
 * El backend devuelve cada registro como  "Clave: valor | Clave: valor | ..."
 */
public final class Filas {

    private Filas() { }

    /** Convierte "ID: 3 | Nombre: Ana" en un mapa ordenado {ID=3, Nombre=Ana}. */
    public static Map<String, String> parsear(String linea) {
        Map<String, String> fila = new LinkedHashMap<>();
        if (linea == null) {
            return fila;
        }
        int sinClave = 0;
        for (String parte : linea.split(" \\| ")) {
            int i = parte.indexOf(": ");
            if (i > 0) {
                fila.put(parte.substring(0, i).trim(), limpiar(parte.substring(i + 2)));
            } else if (parte.endsWith(":") && parte.length() > 1) {
                fila.put(parte.substring(0, parte.length() - 1).trim(), "");
            } else if (!parte.trim().isEmpty()) {
                sinClave++;
                fila.put(sinClave == 1 ? "Detalle" : "Detalle " + sinClave, limpiar(parte));
            }
        }
        return fila;
    }

    private static String limpiar(String valor) {
        String v = valor.trim();
        return "null".equals(v) ? "" : v;
    }

    /** El backend informa los fallos como texto que empieza con "Error". */
    public static boolean esError(String texto) {
        return texto != null && texto.trim().startsWith("Error");
    }

    public static boolean esError(List<String> lista) {
        return lista != null && !lista.isEmpty() && esError(lista.get(0));
    }

    public static boolean noEncontrado(String texto) {
        return texto != null && texto.trim().startsWith("No se encontr");
    }

    public static boolean esEnteroPositivo(String s) {
        return s != null && s.matches("[0-9]{1,9}") && Integer.parseInt(s) > 0;
    }

    public static boolean vacio(String s) {
        return s == null || s.trim().isEmpty();
    }
}
