package servicios;

import jakarta.jws.WebService;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@WebService(serviceName = "WSEmpresas")
public class WSEmpresas {

    // Conexión centralizada: credenciales fuera del código (ver Conexion.java y db.properties.example)
    private Connection obtenerConexion() throws Exception {
        return Conexion.obtener();
    }

    // --- 1. INSERTAR EMPRESA ---
    @WebMethod(operationName = "insertarEmpresa")
    public String insertarEmpresa(
            @WebParam(name = "nombreEmpresa") String nombreEmpresa, 
            @WebParam(name = "direccion") String direccion, 
            @WebParam(name = "telefono") String telefono) {
        try (Connection con = obtenerConexion()) {
            // Nota: Activo es 1 por default y FechaRegistro es GETDATE() por default en la BD
            String sql = "INSERT INTO Empresa (NombreEmpresa, Direccion, Telefono) VALUES (?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nombreEmpresa);
            ps.setString(2, direccion);
            ps.setString(3, telefono);
            ps.executeUpdate();
            return "¡Empresa insertada correctamente!";
        } catch (Exception e) {
            return "Error al insertar: " + e.getMessage();
        }
    }

    // --- 2. ACTUALIZAR EMPRESA ---
    @WebMethod(operationName = "actualizarEmpresa")
    public String actualizarEmpresa(
            @WebParam(name = "idEmpresa") int idEmpresa, 
            @WebParam(name = "nuevoNombre") String nuevoNombre,
            @WebParam(name = "nuevaDireccion") String nuevaDireccion,
            @WebParam(name = "nuevoTelefono") String nuevoTelefono) {
        try (Connection con = obtenerConexion()) {
            String sql = "UPDATE Empresa SET NombreEmpresa = ?, Direccion = ?, Telefono = ? WHERE IdEmpresa = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nuevoNombre);
            ps.setString(2, nuevaDireccion);
            ps.setString(3, nuevoTelefono);
            ps.setInt(4, idEmpresa);
            int filas = ps.executeUpdate();
            return filas > 0 ? "¡Empresa actualizada!" : "No se encontró la empresa con ese ID.";
        } catch (Exception e) {
            return "Error al actualizar: " + e.getMessage();
        }
    }

    // --- 3. ELIMINAR (DESACTIVAR) EMPRESA ---
    // En sistemas profesionales no se borra la empresa, se inactiva (Borrado lógico)
    @WebMethod(operationName = "desactivarEmpresa")
    public String desactivarEmpresa(@WebParam(name = "idEmpresa") int idEmpresa) {
        try (Connection con = obtenerConexion()) {
            String sql = "UPDATE Empresa SET Activo = 0 WHERE IdEmpresa = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idEmpresa);
            int filas = ps.executeUpdate();
            return filas > 0 ? "¡Empresa desactivada correctamente!" : "No se encontró la empresa.";
        } catch (Exception e) {
            return "Error al desactivar: " + e.getMessage();
        }
    }

    // --- 4. SELECCIONAR TODAS LAS EMPRESAS ACTIVAS ---
    @WebMethod(operationName = "seleccionarEmpresas")
    public List<String> seleccionarEmpresas() {
        List<String> listaEmpresas = new ArrayList<>();
        try (Connection con = obtenerConexion()) {
            String sql = "SELECT * FROM Empresa WHERE Activo = 1";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String info = "ID: " + rs.getInt("IdEmpresa") + 
                              " | Nombre: " + rs.getString("NombreEmpresa") +
                              " | Tel: " + rs.getString("Telefono");
                listaEmpresas.add(info);
            }
        } catch (Exception e) {
            listaEmpresas.add("Error: " + e.getMessage());
        }
        return listaEmpresas;
    }

    // --- 5. SELECCIONAR EMPRESA POR ID ---
    @WebMethod(operationName = "seleccionarEmpresaPorId")
    public String seleccionarEmpresaPorId(@WebParam(name = "idEmpresa") int idEmpresa) {
        try (Connection con = obtenerConexion()) {
            String sql = "SELECT * FROM Empresa WHERE IdEmpresa = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idEmpresa);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return "ID: " + rs.getInt("IdEmpresa") + 
                       " | Nombre: " + rs.getString("NombreEmpresa") +
                       " | Dir: " + rs.getString("Direccion") +
                       " | Tel: " + rs.getString("Telefono") +
                       " | Activa: " + rs.getBoolean("Activo");
            } else {
                return "No se encontró ninguna empresa con el ID: " + idEmpresa;
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}