package servicios;

import jakarta.jws.WebService;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@WebService(serviceName = "WSEstados")
public class WSEstados {

    // Conexión centralizada: credenciales fuera del código (ver Conexion.java y db.properties.example)
    private Connection obtenerConexion() throws Exception {
        return Conexion.obtener();
    }

    // --- 1. INSERTAR ESTADO ---
    @WebMethod(operationName = "insertarEstado")
    public String insertarEstado(@WebParam(name = "nombreEstado") String nombreEstado, @WebParam(name = "orden") int orden) {
        try (Connection con = obtenerConexion()) {
            String sql = "INSERT INTO EstadoTicket (NombreEstado, Orden) VALUES (?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nombreEstado);
            ps.setInt(2, orden);
            ps.executeUpdate();
            return "¡Estado insertado correctamente!";
        } catch (Exception e) { 
            return "Error: " + e.getMessage(); 
        }
    }

    // --- 2. ACTUALIZAR ESTADO ---
    @WebMethod(operationName = "actualizarEstado")
    public String actualizarEstado(@WebParam(name = "idEstado") int idEstado, @WebParam(name = "nuevoNombre") String nuevoNombre, @WebParam(name = "nuevoOrden") int nuevoOrden) {
        try (Connection con = obtenerConexion()) {
            String sql = "UPDATE EstadoTicket SET NombreEstado = ?, Orden = ? WHERE IdEstado = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nuevoNombre);
            ps.setInt(2, nuevoOrden);
            ps.setInt(3, idEstado);
            return ps.executeUpdate() > 0 ? "¡Estado actualizado!" : "No se encontró el estado.";
        } catch (Exception e) { 
            return "Error: " + e.getMessage(); 
        }
    }

    // --- 3. ELIMINAR ESTADO ---
    @WebMethod(operationName = "eliminarEstado")
    public String eliminarEstado(@WebParam(name = "idEstado") int idEstado) {
        try (Connection con = obtenerConexion()) {
            String sql = "DELETE FROM EstadoTicket WHERE IdEstado = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idEstado);
            return ps.executeUpdate() > 0 ? "¡Estado eliminado!" : "No se encontró el estado.";
        } catch (Exception e) { 
            return "Error (puede estar en uso): " + e.getMessage(); 
        }
    }

    // --- 4. SELECCIONAR ESTADOS ---
    @WebMethod(operationName = "seleccionarEstados")
    public List<String> seleccionarEstados() {
        List<String> lista = new ArrayList<>();
        try (Connection con = obtenerConexion()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM EstadoTicket ORDER BY Orden ASC");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add("ID: " + rs.getInt("IdEstado") + " | Estado: " + rs.getString("NombreEstado") + " | Orden: " + rs.getInt("Orden"));
            }
        } catch (Exception e) { 
            lista.add("Error: " + e.getMessage()); 
        }
        return lista;
    }
}