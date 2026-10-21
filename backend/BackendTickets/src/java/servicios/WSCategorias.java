package servicios;

import jakarta.jws.WebService;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@WebService(serviceName = "WSCategorias")
public class WSCategorias {

    // Conexión centralizada: credenciales fuera del código (ver Conexion.java y db.properties.example)
    private Connection obtenerConexion() throws Exception {
        return Conexion.obtener();
    }

    // --- 1. INSERTAR CATEGORIA ---
    @WebMethod(operationName = "insertarCategoria")
    public String insertarCategoria(@WebParam(name = "nombreCategoria") String nombreCategoria, @WebParam(name = "prefijo") String prefijo) {
        try (Connection con = obtenerConexion()) {
            String sql = "INSERT INTO CategoriaTicket (NombreCategoria, Prefijo) VALUES (?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nombreCategoria);
            ps.setString(2, prefijo);
            ps.executeUpdate();
            return "¡Categoría insertada correctamente!";
        } catch (Exception e) { 
            return "Error: " + e.getMessage(); 
        }
    }

    // --- 2. ACTUALIZAR CATEGORIA ---
    @WebMethod(operationName = "actualizarCategoria")
    public String actualizarCategoria(@WebParam(name = "idCategoria") int idCategoria, @WebParam(name = "nuevoNombre") String nuevoNombre, @WebParam(name = "nuevoPrefijo") String nuevoPrefijo) {
        try (Connection con = obtenerConexion()) {
            String sql = "UPDATE CategoriaTicket SET NombreCategoria = ?, Prefijo = ? WHERE IdCategoria = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nuevoNombre);
            ps.setString(2, nuevoPrefijo);
            ps.setInt(3, idCategoria);
            return ps.executeUpdate() > 0 ? "¡Categoría actualizada!" : "No se encontró la categoría.";
        } catch (Exception e) { 
            return "Error: " + e.getMessage(); 
        }
    }

    // --- 3. ELIMINAR CATEGORIA ---
    @WebMethod(operationName = "eliminarCategoria")
    public String eliminarCategoria(@WebParam(name = "idCategoria") int idCategoria) {
        try (Connection con = obtenerConexion()) {
            String sql = "DELETE FROM CategoriaTicket WHERE IdCategoria = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idCategoria);
            return ps.executeUpdate() > 0 ? "¡Categoría eliminada!" : "No se encontró la categoría.";
        } catch (Exception e) { 
            return "Error (puede estar en uso): " + e.getMessage(); 
        }
    }

    // --- 4. SELECCIONAR CATEGORIAS ---
    @WebMethod(operationName = "seleccionarCategorias")
    public List<String> seleccionarCategorias() {
        List<String> lista = new ArrayList<>();
        try (Connection con = obtenerConexion()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM CategoriaTicket");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add("ID: " + rs.getInt("IdCategoria") + " | Categoría: " + rs.getString("NombreCategoria") + " | Prefijo: " + rs.getString("Prefijo") + " | Consecutivo actual: " + rs.getInt("UltimoConsecutivo"));
            }
        } catch (Exception e) { 
            lista.add("Error: " + e.getMessage()); 
        }
        return lista;
    }
}