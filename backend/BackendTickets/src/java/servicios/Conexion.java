package servicios;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Punto único de conexión a SQL Server para todos los servicios SOAP.
 *
 * Las credenciales NO están en el código. Se leen en este orden de prioridad:
 *   1) Propiedades de JVM:      -Dtickets.db.url=...  -Dtickets.db.user=...  -Dtickets.db.password=...
 *   2) Variables de entorno:    TICKETS_DB_URL, TICKETS_DB_USER, TICKETS_DB_PASSWORD
 *   3) Archivo db.properties en el classpath (src/java/db.properties, ignorado por Git)
 *   4) Valores por defecto (SQL Server local en el puerto 1433)
 *
 * Para cambiar de servidor el día de la presentación basta con editar db.properties
 * (o las variables de entorno) y volver a desplegar: no se toca ningún WS*.java.
 */
public final class Conexion {

    private static final String URL_POR_DEFECTO =
            "jdbc:sqlserver://localhost:1433;databaseName=DB_ControlTickets;encrypt=true;trustServerCertificate=true;";
    private static final String USUARIO_POR_DEFECTO = "tickets_app";

    private static final String URL;
    private static final String USUARIO;
    private static final String CONTRASENA;

    static {
        Properties archivo = new Properties();
        try (InputStream in = Conexion.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                archivo.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (Exception ignorado) {
            // Sin archivo: se usan propiedades de JVM, variables de entorno o valores por defecto.
        }
        URL = leer("tickets.db.url", "TICKETS_DB_URL", archivo.getProperty("db.url"), URL_POR_DEFECTO);
        USUARIO = leer("tickets.db.user", "TICKETS_DB_USER", archivo.getProperty("db.user"), USUARIO_POR_DEFECTO);
        CONTRASENA = leer("tickets.db.password", "TICKETS_DB_PASSWORD", archivo.getProperty("db.password"), "");
    }

    private Conexion() { }

    /** Abre una conexión nueva. Quien la pide debe cerrarla (try-with-resources). */
    public static Connection obtener() throws SQLException, ClassNotFoundException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        return DriverManager.getConnection(URL, USUARIO, CONTRASENA);
    }

    private static String leer(String propiedadJvm, String variableEntorno, String valorArchivo, String porDefecto) {
        String v = System.getProperty(propiedadJvm);
        if (vacio(v)) {
            v = System.getenv(variableEntorno);
        }
        if (vacio(v)) {
            v = valorArchivo;
        }
        return vacio(v) ? porDefecto : v.trim();
    }

    private static boolean vacio(String s) {
        return s == null || s.trim().isEmpty();
    }
}
