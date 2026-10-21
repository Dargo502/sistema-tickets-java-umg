package servicios;

import jakarta.jws.WebService;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@WebService(serviceName = "WSRoles")
public class WSRoles {

    // Conexión centralizada: credenciales fuera del código (ver Conexion.java y db.properties.example)
    private Connection obtenerConexion() throws Exception {
        return Conexion.obtener();
    }

    // --- 1. INSERTAR ---
    @WebMethod(operationName = "insertarRol")
    public String insertarRol(@WebParam(name = "nombreRol") String nombreRol) {
        try (Connection con = obtenerConexion()) {
            String sql = "INSERT INTO Rol (NombreRol) VALUES (?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nombreRol);
            ps.executeUpdate();
            return "¡Rol insertado correctamente!";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // --- 2. ACTUALIZAR ---
    @WebMethod(operationName = "actualizarRol")
    public String actualizarRol(@WebParam(name = "idRol") int idRol, @WebParam(name = "nuevoNombre") String nuevoNombre) {
        try (Connection con = obtenerConexion()) {
            String sql = "UPDATE Rol SET NombreRol = ? WHERE IdRol = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nuevoNombre);
            ps.setInt(2, idRol);
            int filas = ps.executeUpdate();
            return filas > 0 ? "¡Rol actualizado!" : "No se encontró el Rol con ese ID.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // --- 3. ELIMINAR ---
    @WebMethod(operationName = "eliminarRol")
    public String eliminarRol(@WebParam(name = "idRol") int idRol) {
        try (Connection con = obtenerConexion()) {
            String sql = "DELETE FROM Rol WHERE IdRol = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idRol);
            int filas = ps.executeUpdate();
            return filas > 0 ? "¡Rol eliminado!" : "No se encontró el Rol con ese ID.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // --- 4. SELECCIONAR TODOS ---
    @WebMethod(operationName = "seleccionarTodos")
    public List<String> seleccionarTodos() {
        List<String> listaRoles = new ArrayList<>();
        try (Connection con = obtenerConexion()) {
            String sql = "SELECT * FROM Rol";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                listaRoles.add("ID: " + rs.getInt("IdRol") + " | Rol: " + rs.getString("NombreRol"));
            }
        } catch (Exception e) {
            listaRoles.add("Error: " + e.getMessage());
        }
        return listaRoles;
    }

    // --- 5. SELECCIONAR POR ID ---
    @WebMethod(operationName = "seleccionarPorId")
    public String seleccionarPorId(@WebParam(name = "idRol") int idRol) {
        try (Connection con = obtenerConexion()) {
            String sql = "SELECT * FROM Rol WHERE IdRol = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idRol);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return "ID: " + rs.getInt("IdRol") + " | Rol: " + rs.getString("NombreRol");
            } else {
                return "No se encontró ningún rol con el ID: " + idRol;
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}