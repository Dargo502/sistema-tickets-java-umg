package servicios;

import jakarta.jws.WebService;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@WebService(serviceName = "WSTickets")
public class WSTickets {

    // Conexión centralizada: credenciales fuera del código (ver Conexion.java y db.properties.example)
    private Connection obtenerConexion() throws Exception {
        return Conexion.obtener();
    }

    // --- 1. INSERTAR TICKET NUEVO ---
    @WebMethod(operationName = "insertarTicket")
    public String insertarTicket(
            @WebParam(name = "titulo") String titulo, 
            @WebParam(name = "descripcion") String descripcion,
            @WebParam(name = "idCategoria") int idCategoria,
            @WebParam(name = "idPrioridad") int idPrioridad,
            @WebParam(name = "idSolicitante") int idSolicitante) {
        try (Connection con = obtenerConexion()) {
            // Nota: Mandamos PosicionCola como 0. El Trigger "TR_Ticket_Insertar" se encargará 
            // de asignarle su Folio y calcular su posición real en la cola inmediatamente después.
            // El IdEstado siempre inicia en 1 (Solicitado).
            String sql = "INSERT INTO Ticket (Titulo, Descripcion, IdCategoria, IdPrioridad, IdEstado, IdSolicitante, PosicionCola) "
                       + "VALUES (?, ?, ?, ?, 1, ?, 0)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, titulo);
            ps.setString(2, descripcion);
            ps.setInt(3, idCategoria);
            ps.setInt(4, idPrioridad);
            ps.setInt(5, idSolicitante);
            ps.executeUpdate();
            return "¡Ticket creado exitosamente! (Folio e Historial generados por SQL)";
        } catch (Exception e) { 
            return "Error al crear ticket: " + e.getMessage(); 
        }
    }

    // --- 2. RECLAMAR TICKET (ASIGNAR ESPECIALISTA) ---
    @WebMethod(operationName = "reclamarTicket")
    public String reclamarTicket(@WebParam(name = "idTicket") int idTicket, @WebParam(name = "idEspecialista") int idEspecialista) {
        try (Connection con = obtenerConexion()) {
            // El Trigger "TR_Ticket_Reclamo" validará que no esté reclamado, le pondrá fecha de asignación y creará la notificación.
            String sql = "UPDATE Ticket SET IdEspecialista = ? WHERE IdTicket = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idEspecialista);
            ps.setInt(2, idTicket);
            int filas = ps.executeUpdate();
            return filas > 0 ? "¡Ticket reclamado exitosamente!" : "No se encontró el ticket.";
        } catch (Exception e) { 
            return "Error al reclamar (o ya fue reclamado): " + e.getMessage(); 
        }
    }

    // --- 3. CAMBIAR ESTADO DE TICKET ---
    @WebMethod(operationName = "cambiarEstadoTicket")
    public String cambiarEstadoTicket(@WebParam(name = "idTicket") int idTicket, @WebParam(name = "idNuevoEstado") int idNuevoEstado) {
        try (Connection con = obtenerConexion()) {
            // El Trigger "TR_Ticket_ValidarEstado" validará que no se salten pasos y creará el historial.
            String sql = "UPDATE Ticket SET IdEstado = ? WHERE IdTicket = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idNuevoEstado);
            ps.setInt(2, idTicket);
            int filas = ps.executeUpdate();
            return filas > 0 ? "¡Estado actualizado!" : "No se encontró el ticket.";
        } catch (Exception e) { 
            return "Error (Transición inválida o ticket no existe): " + e.getMessage(); 
        }
    }

    // --- 4. ELIMINAR TICKET (BORRADO LÓGICO) ---
    @WebMethod(operationName = "eliminarTicket")
    public String eliminarTicket(@WebParam(name = "idTicket") int idTicket) {
        try (Connection con = obtenerConexion()) {
            String sql = "UPDATE Ticket SET Eliminado = 1 WHERE IdTicket = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idTicket);
            int filas = ps.executeUpdate();
            return filas > 0 ? "¡Ticket eliminado de la vista!" : "No se encontró el ticket.";
        } catch (Exception e) { 
            return "Error al eliminar: " + e.getMessage(); 
        }
    }

    // --- 5. SELECCIONAR TICKETS ACTIVOS (LA COLA FIFO) ---
    @WebMethod(operationName = "seleccionarTickets")
    public List<String> seleccionarTickets() {
        List<String> listaTickets = new ArrayList<>();
        try (Connection con = obtenerConexion()) {
            // Unimos varias tablas para que la app frontend reciba los textos y no puros números
            String sql = "SELECT T.IdTicket, T.Folio, T.Titulo, C.NombreCategoria, P.NombrePrioridad, E.NombreEstado, U.NombreCompleto AS Solicitante, UE.NombreCompleto AS Especialista "
                       + "FROM Ticket T "
                       + "INNER JOIN CategoriaTicket C ON T.IdCategoria = C.IdCategoria "
                       + "INNER JOIN PrioridadTicket P ON T.IdPrioridad = P.IdPrioridad "
                       + "INNER JOIN EstadoTicket E ON T.IdEstado = E.IdEstado "
                       + "INNER JOIN Usuario U ON T.IdSolicitante = U.IdUsuario "
                       + "LEFT JOIN Usuario UE ON T.IdEspecialista = UE.IdUsuario "
                       + "WHERE T.Eliminado = 0 "
                       + "ORDER BY T.PosicionCola ASC";
            
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String info = "ID: " + rs.getInt("IdTicket") + " | Folio: " + rs.getString("Folio") + 
                              " | Título: " + rs.getString("Titulo") +
                              " | Cat: " + rs.getString("NombreCategoria") +
                              " | Prio: " + rs.getString("NombrePrioridad") +
                              " | Estado: " + rs.getString("NombreEstado") +
                              " | Solicitante: " + rs.getString("Solicitante") +
                              " | Especialista: " + rs.getString("Especialista");
                listaTickets.add(info);
            }
        } catch (Exception e) { 
            listaTickets.add("Error: " + e.getMessage()); 
        }
        return listaTickets;
    }

    // --- 6. SELECCIONAR TICKET POR ID (PARCHE) ---
    @WebMethod(operationName = "seleccionarTicketPorId")
    public String seleccionarTicketPorId(@WebParam(name = "idTicket") int idTicket) {
        try (Connection con = obtenerConexion()) {
            String sql = "SELECT T.IdTicket, T.Folio, T.Titulo, T.Descripcion, C.NombreCategoria, P.NombrePrioridad, E.NombreEstado, "
                       + "U.NombreCompleto AS Solicitante, UE.NombreCompleto AS Especialista "
                       + "FROM Ticket T "
                       + "INNER JOIN CategoriaTicket C ON T.IdCategoria = C.IdCategoria "
                       + "INNER JOIN PrioridadTicket P ON T.IdPrioridad = P.IdPrioridad "
                       + "INNER JOIN EstadoTicket E ON T.IdEstado = E.IdEstado "
                       + "INNER JOIN Usuario U ON T.IdSolicitante = U.IdUsuario "
                       + "LEFT JOIN Usuario UE ON T.IdEspecialista = UE.IdUsuario "
                       + "WHERE T.IdTicket = ? AND T.Eliminado = 0";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idTicket);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return "ID: " + rs.getInt("IdTicket") +
                       " | Folio: " + rs.getString("Folio") +
                       " | Título: " + rs.getString("Titulo") +
                       " | Descripción: " + rs.getString("Descripcion") +
                       " | Cat: " + rs.getString("NombreCategoria") +
                       " | Prio: " + rs.getString("NombrePrioridad") +
                       " | Estado: " + rs.getString("NombreEstado") +
                       " | Solicitante: " + rs.getString("Solicitante") +
                       " | Especialista: " + rs.getString("Especialista");
            }
            return "No se encontró ningún ticket con el ID: " + idTicket;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // --- 7. ACTUALIZAR DATOS GENERALES DEL TICKET (PARCHE) ---
    @WebMethod(operationName = "actualizarTicket")
    public String actualizarTicket(
            @WebParam(name = "idTicket") int idTicket,
            @WebParam(name = "titulo") String titulo,
            @WebParam(name = "descripcion") String descripcion,
            @WebParam(name = "idCategoria") int idCategoria,
            @WebParam(name = "idPrioridad") int idPrioridad) {
        try (Connection con = obtenerConexion()) {
            String sql = "UPDATE Ticket SET Titulo = ?, Descripcion = ?, IdCategoria = ?, IdPrioridad = ? "
                       + "WHERE IdTicket = ? AND Eliminado = 0";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, titulo);
            ps.setString(2, descripcion);
            ps.setInt(3, idCategoria);
            ps.setInt(4, idPrioridad);
            ps.setInt(5, idTicket);
            int filas = ps.executeUpdate();
            return filas > 0 ? "¡Ticket actualizado!" : "No se encontró el ticket.";
        } catch (Exception e) {
            return "Error al actualizar ticket: " + e.getMessage();
        }
    }
}
