package tickets.modelo;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import tickets.modelo.Campo.Tipo;

/**
 * Catálogo de módulos CRUD. Cada entrada declara el servicio SOAP y los nombres exactos de las
 * operaciones y parámetros publicados por el backend (@WebMethod / @WebParam).
 * Si el equipo agrega o renombra operaciones en el backend, solo se cambia este archivo.
 *
 * IMPORTANTE: el orden de los campos debe coincidir con el orden de los parámetros del método Java.
 */
public final class Entidades {

    private static final Map<String, Entidad> MAPA = new LinkedHashMap<>();

    static {
        // ---- Tickets (cola FIFO). porId/actualizar requieren el parche del backend (ver backend/parches).
        registrar(new Entidad("tickets", "Tickets", "Ticket", "WSTickets")
                .icono("🎫").descripcion("Incidentes en la cola FIFO")
                .id("idTicket").etiqueta("Folio")
                .listar("seleccionarTickets").porId("seleccionarTicketPorId")
                .insertar("insertarTicket").actualizar("actualizarTicket")
                .eliminar("eliminarTicket", "Eliminar")
                .campo(new Campo("titulo", "Título", Tipo.TEXTO).max(150).param("titulo").prefill("Título"))
                .campo(new Campo("descripcion", "Descripción", Tipo.AREA).opcional().max(1000)
                        .param("descripcion").prefill("Descripción"))
                .campo(new Campo("categoria", "Categoría", Tipo.SELECT).ref("categorias")
                        .param("idCategoria").prefill("Cat"))
                .campo(new Campo("prioridad", "Prioridad", Tipo.SELECT).ref("prioridades")
                        .param("idPrioridad").prefill("Prio"))
                .campo(new Campo("solicitante", "Solicitante", Tipo.SELECT).ref("usuarios")
                        .paramInsertar("idSolicitante").prefill("Solicitante")));

        registrar(new Entidad("usuarios", "Usuarios", "Usuario", "WSUsuarios")
                .icono("👤").descripcion("Solicitantes, especialistas y administradores")
                .id("idUsuario").etiqueta("Nombre")
                .listar("seleccionarUsuarios").porId("seleccionarUsuarioPorId")
                .insertar("insertarUsuario").actualizar("actualizarUsuario")
                .eliminar("desactivarUsuario", "Desactivar")
                .campo(new Campo("empresa", "Empresa", Tipo.SELECT).ref("empresas").param("idEmpresa")
                        .prefill("Empresa"))
                .campo(new Campo("rol", "Rol", Tipo.SELECT).ref("roles").param("idRol").prefill("Rol"))
                .campo(new Campo("nombreCompleto", "Nombre completo", Tipo.TEXTO).param("nombreCompleto")
                        .prefill("Nombre"))
                .campo(new Campo("nombreUsuario", "Nombre de usuario", Tipo.TEXTO).max(50).param("nombreUsuario")
                        .prefill("User"))
                .campo(new Campo("password", "Contraseña", Tipo.CLAVE).max(100).paramInsertar("password"))
                .campo(new Campo("correo", "Correo", Tipo.CORREO).param("correo").prefill("Correo")));

        registrar(new Entidad("empresas", "Empresas", "Empresa", "WSEmpresas")
                .icono("🏢").descripcion("Empresas a las que pertenecen los usuarios")
                .id("idEmpresa").etiqueta("Nombre")
                .listar("seleccionarEmpresas").porId("seleccionarEmpresaPorId")
                .insertar("insertarEmpresa").actualizar("actualizarEmpresa")
                .eliminar("desactivarEmpresa", "Desactivar")
                .campo(new Campo("nombre", "Nombre de la empresa", Tipo.TEXTO)
                        .paramInsertar("nombreEmpresa").paramActualizar("nuevoNombre").prefill("Nombre"))
                .campo(new Campo("direccion", "Dirección", Tipo.TEXTO).opcional().max(200)
                        .paramInsertar("direccion").paramActualizar("nuevaDireccion").prefill("Dir"))
                .campo(new Campo("telefono", "Teléfono", Tipo.TEXTO).opcional().max(20)
                        .paramInsertar("telefono").paramActualizar("nuevoTelefono").prefill("Tel")));

        registrar(new Entidad("roles", "Roles", "Rol", "WSRoles")
                .icono("🛡️").descripcion("Solicitante, Especialista, Administrador")
                .id("idRol").etiqueta("Rol")
                .listar("seleccionarTodos").porId("seleccionarPorId")
                .insertar("insertarRol").actualizar("actualizarRol")
                .eliminar("eliminarRol", "Eliminar")
                .campo(new Campo("nombre", "Nombre del rol", Tipo.TEXTO).max(50)
                        .paramInsertar("nombreRol").paramActualizar("nuevoNombre").prefill("Rol")));

        registrar(new Entidad("categorias", "Categorías", "Categoría", "WSCategorias")
                .icono("🗂️").descripcion("Hardware, Software, Redes… (genera el folio)")
                .id("idCategoria").etiqueta("Categoría")
                .listar("seleccionarCategorias")
                .insertar("insertarCategoria").actualizar("actualizarCategoria")
                .eliminar("eliminarCategoria", "Eliminar")
                .campo(new Campo("nombre", "Nombre de la categoría", Tipo.TEXTO).max(60)
                        .paramInsertar("nombreCategoria").paramActualizar("nuevoNombre").prefill("Categoría"))
                .campo(new Campo("prefijo", "Prefijo del folio", Tipo.TEXTO).max(5)
                        .paramInsertar("prefijo").paramActualizar("nuevoPrefijo").prefill("Prefijo")));

        registrar(new Entidad("estados", "Estados", "Estado", "WSEstados")
                .icono("🔄").descripcion("Solicitado → En Proceso → Finalizado")
                .id("idEstado").etiqueta("Estado")
                .listar("seleccionarEstados")
                .insertar("insertarEstado").actualizar("actualizarEstado")
                .eliminar("eliminarEstado", "Eliminar")
                .campo(new Campo("nombre", "Nombre del estado", Tipo.TEXTO).max(30)
                        .paramInsertar("nombreEstado").paramActualizar("nuevoNombre").prefill("Estado"))
                .campo(new Campo("orden", "Orden", Tipo.NUMERO)
                        .paramInsertar("orden").paramActualizar("nuevoOrden").prefill("Orden")));

        registrar(new Entidad("prioridades", "Prioridades", "Prioridad", "WSPrioridades")
                .icono("⚑").descripcion("Alta, Media, Baja")
                .id("idPrioridad").etiqueta("Prioridad")
                .listar("seleccionarPrioridades")
                .insertar("insertarPrioridad").actualizar("actualizarPrioridad")
                .eliminar("eliminarPrioridad", "Eliminar")
                .campo(new Campo("nombre", "Nombre de la prioridad", Tipo.TEXTO).max(20)
                        .paramInsertar("nombrePrioridad").paramActualizar("nuevoNombre").prefill("Prioridad"))
                .campo(new Campo("peso", "Peso (1 = más urgente)", Tipo.NUMERO)
                        .paramInsertar("nivelPeso").paramActualizar("nuevoPeso").prefill("Peso")));
    }

    private Entidades() { }

    private static void registrar(Entidad e) {
        MAPA.put(e.getClave(), e);
    }

    public static Entidad obtener(String clave) {
        return clave == null ? null : MAPA.get(clave);
    }

    public static Collection<Entidad> todas() {
        return Collections.unmodifiableCollection(MAPA.values());
    }
}
