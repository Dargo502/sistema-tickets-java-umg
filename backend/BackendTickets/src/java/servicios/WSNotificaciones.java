package servicios;

import jakarta.jws.WebService;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@WebService(serviceName = "WSNotificaciones")
public class WSNotificaciones {

    // Conexión centralizada: credenciales fuera del código (ver Conexion.java y db.properties.example)
    private Connection obtenerConexion() throws Exception {
        return Conexion.obtener();
    }

    // --- 1. VER NOTIFICACIONES PENDIENTES DE UN ESPECIALISTA ---
    @WebMethod(operationName = "seleccionarNotificacionesPendientes")
    public List<String> seleccionarNotificacionesPendientes(@WebParam(name = "idEspecialista") int idEspecialista) {
        List<String> lista = new ArrayList<>();
        try (Connection con = obtenerConexion()) {
            String sql = "SELECT N.IdNotificacion, N.Mensaje, N.FechaNotificacion, T.Folio " +
                         "FROM NotificacionTicket N " +
                         "INNER JOIN Ticket T ON N.IdTicket = T.IdTicket " +
                         "WHERE N.IdEspecialista = ? AND N.Leida = 0 ORDER BY N.FechaNotificacion DESC";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idEspecialista);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                lista.add("ID Notif: " + rs.getInt("IdNotificacion") + 
                          " | Ticket: " + rs.getString("Folio") + 
                          " | Fecha: " + rs.getString("FechaNotificacion") + 
                          " | Alerta: " + rs.getString("Mensaje"));
            }
        } catch (Exception e) { 
            lista.add("Error al cargar notificaciones: " + e.getMessage()); 
        }
        return lista;
    }

    // --- 2. MARCAR NOTIFICACIÓN COMO LEÍDA ---
    @WebMethod(operationName = "marcarNotificacionLeida")
    public String marcarNotificacionLeida(@WebParam(name = "idNotificacion") int idNotificacion) {
        try (Connection con = obtenerConexion()) {
            String sql = "UPDATE NotificacionTicket SET Leida = 1 WHERE IdNotificacion = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idNotificacion);
            int filas = ps.executeUpdate();
            return filas > 0 ? "¡Notificación marcada como leída!" : "No se encontró la notificación.";
        } catch (Exception e) { 
            return "Error al actualizar notificación: " + e.getMessage(); 
        }
    }
}