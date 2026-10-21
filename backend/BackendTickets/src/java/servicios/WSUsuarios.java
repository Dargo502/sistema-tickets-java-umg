package servicios;

import jakarta.jws.WebService;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@WebService(serviceName = "WSUsuarios")
public class WSUsuarios {

    // Conexión centralizada: credenciales fuera del código (ver Conexion.java y db.properties.example)
    private Connection obtenerConexion() throws Exception {
        return Conexion.obtener();
    }

    // --- 1. INSERTAR USUARIO ---
    @WebMethod(operationName = "insertarUsuario")
    public String insertarUsuario(
            @WebParam(name = "idEmpresa") int idEmpresa, 
            @WebParam(name = "idRol") int idRol, 
            @WebParam(name = "nombreCompleto") String nombreCompleto,
            @WebParam(name = "nombreUsuario") String nombreUsuario,
            @WebParam(name = "password") String password,
            @WebParam(name = "correo") String correo) {
        try (Connection con = obtenerConexion()) {
            // La contraseña se guarda como hash SHA-256 (nunca en texto plano), como indica el script SQL
            String sql = "INSERT INTO Usuario (IdEmpresa, IdRol, NombreCompleto, NombreUsuario, Contrasena, Correo) "
                       + "VALUES (?, ?, ?, ?, HASHBYTES('SHA2_256', ?), ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idEmpresa);
            ps.setInt(2, idRol);
            ps.setString(3, nombreCompleto);
            ps.setString(4, nombreUsuario);
            ps.setString(5, password);
            ps.setString(6, correo);
            ps.executeUpdate();
            return "¡Usuario insertado correctamente!";
        } catch (Exception e) {
            return "Error al insertar: " + e.getMessage();
        }
    }

    // --- 2. ACTUALIZAR USUARIO (Datos generales, sin tocar contraseña) ---
    @WebMethod(operationName = "actualizarUsuario")
    public String actualizarUsuario(
            @WebParam(name = "idUsuario") int idUsuario, 
            @WebParam(name = "idEmpresa") int idEmpresa, 
            @WebParam(name = "idRol") int idRol, 
            @WebParam(name = "nombreCompleto") String nombreCompleto,
            @WebParam(name = "nombreUsuario") String nombreUsuario,
            @WebParam(name = "correo") String correo) {
        try (Connection con = obtenerConexion()) {
            String sql = "UPDATE Usuario SET IdEmpresa = ?, IdRol = ?, NombreCompleto = ?, NombreUsuario = ?, Correo = ? WHERE IdUsuario = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idEmpresa);
            ps.setInt(2, idRol);
            ps.setString(3, nombreCompleto);
            ps.setString(4, nombreUsuario);
            ps.setString(5, correo);
            ps.setInt(6, idUsuario);
            int filas = ps.executeUpdate();
            return filas > 0 ? "¡Usuario actualizado!" : "No se encontró el usuario con ese ID.";
        } catch (Exception e) {
            return "Error al actualizar: " + e.getMessage();
        }
    }

    // --- 3. DESACTIVAR USUARIO (Borrado lógico) ---
    @WebMethod(operationName = "desactivarUsuario")
    public String desactivarUsuario(@WebParam(name = "idUsuario") int idUsuario) {
        try (Connection con = obtenerConexion()) {
            String sql = "UPDATE Usuario SET Activo = 0 WHERE IdUsuario = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idUsuario);
            int filas = ps.executeUpdate();
            return filas > 0 ? "¡Usuario desactivado correctamente!" : "No se encontró el usuario.";
        } catch (Exception e) {
            return "Error al desactivar: " + e.getMessage();
        }
    }

    // --- 4. SELECCIONAR TODOS LOS USUARIOS ACTIVOS ---
    @WebMethod(operationName = "seleccionarUsuarios")
    public List<String> seleccionarUsuarios() {
        List<String> listaUsuarios = new ArrayList<>();
        try (Connection con = obtenerConexion()) {
            // Hacemos un INNER JOIN para traer el nombre del Rol en lugar de solo el número
            String sql = "SELECT u.IdUsuario, u.NombreCompleto, u.NombreUsuario, u.Correo, r.NombreRol "
                       + "FROM Usuario u INNER JOIN Rol r ON u.IdRol = r.IdRol "
                       + "WHERE u.Activo = 1";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String info = "ID: " + rs.getInt("IdUsuario") + 
                              " | Nombre: " + rs.getString("NombreCompleto") +
                              " | User: " + rs.getString("NombreUsuario") +
                              " | Rol: " + rs.getString("NombreRol");
                listaUsuarios.add(info);
            }
        } catch (Exception e) {
            listaUsuarios.add("Error: " + e.getMessage());
        }
        return listaUsuarios;
    }

    // --- 5. SELECCIONAR USUARIO POR ID ---
    @WebMethod(operationName = "seleccionarUsuarioPorId")
    public String seleccionarUsuarioPorId(@WebParam(name = "idUsuario") int idUsuario) {
        try (Connection con = obtenerConexion()) {
            String sql = "SELECT * FROM Usuario WHERE IdUsuario = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return "ID: " + rs.getInt("IdUsuario") + 
                       " | Nombre: " + rs.getString("NombreCompleto") +
                       " | User: " + rs.getString("NombreUsuario") +
                       " | Correo: " + rs.getString("Correo");
            } else {
                return "No se encontró ningún usuario con el ID: " + idUsuario;
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}