package servicios;

import jakarta.jws.WebService;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@WebService(serviceName = "WSPrioridades")
public class WSPrioridades {

    // Conexión centralizada: credenciales fuera del código (ver Conexion.java y db.properties.example)
    private Connection obtenerConexion() throws Exception {
        return Conexion.obtener();
    }

    // --- 1. INSERTAR PRIORIDAD ---
    @WebMethod(operationName = "insertarPrioridad")
    public String insertarPrioridad(@WebParam(name = "nombrePrioridad") String nombrePrioridad, @WebParam(name = "nivelPeso") int nivelPeso) {
        try (Connection con = obtenerConexion()) {
            String sql = "INSERT INTO PrioridadTicket (NombrePrioridad, NivelPeso) VALUES (?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nombrePrioridad);
            ps.setInt(2, nivelPeso);
            ps.executeUpdate();
            return "¡Prioridad insertada correctamente!";
        } catch (Exception e) { 
            return "Error: " + e.getMessage(); 
        }
    }

    // --- 2. ACTUALIZAR PRIORIDAD ---
    @WebMethod(operationName = "actualizarPrioridad")
    public String actualizarPrioridad(@WebParam(name = "idPrioridad") int idPrioridad, @WebParam(name = "nuevoNombre") String nuevoNombre, @WebParam(name = "nuevoPeso") int nuevoPeso) {
        try (Connection con = obtenerConexion()) {
            String sql = "UPDATE PrioridadTicket SET NombrePrioridad = ?, NivelPeso = ? WHERE IdPrioridad = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nuevoNombre);
            ps.setInt(2, nuevoPeso);
            ps.setInt(3, idPrioridad);
            return ps.executeUpdate() > 0 ? "¡Prioridad actualizada!" : "No se encontró la prioridad.";
        } catch (Exception e) { 
            return "Error: " + e.getMessage(); 
        }
    }

    // --- 3. ELIMINAR PRIORIDAD ---
    @WebMethod(operationName = "eliminarPrioridad")
    public String eliminarPrioridad(@WebParam(name = "idPrioridad") int idPrioridad) {
        try (Connection con = obtenerConexion()) {
            String sql = "DELETE FROM PrioridadTicket WHERE IdPrioridad = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idPrioridad);
            return ps.executeUpdate() > 0 ? "¡Prioridad eliminada!" : "No se encontró la prioridad.";
        } catch (Exception e) { 
            return "Error (puede estar en uso): " + e.getMessage(); 
        }
    }

    // --- 4. SELECCIONAR PRIORIDADES ---
    @WebMethod(operationName = "seleccionarPrioridades")
    public List<String> seleccionarPrioridades() {
        List<String> lista = new ArrayList<>();
        try (Connection con = obtenerConexion()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM PrioridadTicket ORDER BY NivelPeso ASC");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add("ID: " + rs.getInt("IdPrioridad") + " | Prioridad: " + rs.getString("NombrePrioridad") + " | Peso: " + rs.getInt("NivelPeso"));
            }
        } catch (Exception e) { 
            lista.add("Error: " + e.getMessage()); 
        }
        return lista;
    }
}