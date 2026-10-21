package tickets.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Configuración de la aplicación web: URL base del servidor de servicios SOAP.
 * Orden de prioridad al arrancar:
 *   1) propiedad de JVM  -Dsoap.base.url=...
 *   2) variable de entorno TICKETS_SOAP_URL
 *   3) archivo config.properties (clave soap.base.url)
 *   4) valor por defecto http://localhost:8080/BackendTickets
 * Además se puede cambiar en caliente desde la pantalla "Configuración".
 */
public final class AppConfig {

    private static final String POR_DEFECTO = "http://localhost:8080/BackendTickets";
    private static volatile String baseUrl = cargarInicial();

    private AppConfig() { }

    public static String getBaseUrl() {
        return baseUrl;
    }

    /** Cambia la URL base (solo http/https). Lanza IllegalArgumentException si no es válida. */
    public static void setBaseUrl(String nueva) {
        baseUrl = validar(nueva);
    }

    static String validar(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("La URL no puede estar vacía.");
        }
        String v = valor.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        URI uri;
        try {
            uri = URI.create(v);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La URL no tiene un formato válido.");
        }
        String esquema = uri.getScheme();
        if (esquema == null || !(esquema.equalsIgnoreCase("http") || esquema.equalsIgnoreCase("https"))
                || uri.getHost() == null) {
            throw new IllegalArgumentException(
                    "Use una URL como http://192.168.1.20:8080/BackendTickets");
        }
        return v;
    }

    private static String cargarInicial() {
        String v = System.getProperty("soap.base.url");
        if (vacio(v)) {
            v = System.getenv("TICKETS_SOAP_URL");
        }
        if (vacio(v)) {
            try (InputStream in = AppConfig.class.getResourceAsStream("/config.properties")) {
                if (in != null) {
                    Properties p = new Properties();
                    p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                    v = p.getProperty("soap.base.url");
                }
            } catch (IOException ignorado) {
                // se usa el valor por defecto
            }
        }
        if (vacio(v)) {
            v = POR_DEFECTO;
        }
        try {
            return validar(v);
        } catch (IllegalArgumentException e) {
            return POR_DEFECTO;
        }
    }

    private static boolean vacio(String s) {
        return s == null || s.trim().isEmpty();
    }
}
