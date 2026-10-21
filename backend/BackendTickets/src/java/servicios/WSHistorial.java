package servicios;

import jakarta.jws.WebService;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@WebService(serviceName = "WSHistorial")
public class WSHistorial {

    // Conexión centralizada: credenciales fuera del código (ver Conexion.java y db.properties.example)
    private Connection obtenerConexion() throws Exception {
        return Conexion.obtener();
    }

    // --- 1. SELECCIONAR HISTORIAL DE UN TICKET ESPECÍFICO ---
    // No hay método INSERT porque el Trigger "TR_Ticket_ValidarEstado" y "TR_Ticket_Insertar" lo hacen solos.
    @WebMethod(operationName = "seleccionarHistorialPorTicket")
    public List<String> seleccionarHistorialPorTicket(@WebParam(name = "idTicket") int idTicket) {
        List<String> lista = new ArrayList<>();
        try (Connection con = obtenerConexion()) {
            String sql = "SELECT H.IdHistorial, H.Comentario, H.FechaCambio, U.NombreCompleto AS Usuario, " +
                         "EA.NombreEstado AS EstadoAnterior, EN.NombreEstado AS EstadoNuevo " +
                         "FROM HistorialTicket H " +
                         "INNER JOIN Usuario U ON H.IdUsuarioAccion = U.IdUsuario " +
                         "LEFT JOIN EstadoTicket EA ON H.IdEstadoAnterior = EA.IdEstado " +
                         "INNER JOIN EstadoTicket EN ON H.IdEstadoNuevo = EN.IdEstado " +
                         "WHERE H.IdTicket = ? ORDER BY H.FechaCambio ASC";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idTicket);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String estadoAnt = rs.getString("EstadoAnterior") != null ? rs.getString("EstadoAnterior") : "Ninguno (Nuevo)";
                lista.add("Fecha: " + rs.getString("FechaCambio") + 
                          " | Usuario: " + rs.getString("Usuario") + 
                          " | De: " + estadoAnt + 
                          " -> A: " + rs.getString("EstadoNuevo") + 
                          " | Nota: " + rs.getString("Comentario"));
            }
        } catch (Exception e) { 
            lista.add("Error al cargar historial: " + e.getMessage()); 
        }
        return lista;
    }
}