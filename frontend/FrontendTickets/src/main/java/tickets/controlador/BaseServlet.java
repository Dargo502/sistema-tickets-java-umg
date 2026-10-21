package tickets.controlador;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import tickets.config.AppConfig;
import tickets.modelo.Entidades;

/** Utilidades comunes de los controladores: vistas, mensajes flash y redirecciones (patrón PRG). */
public abstract class BaseServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    protected static final String JSP = "/WEB-INF/jsp/";

    /** Muestra una vista JSP (el controlador nunca escribe HTML). */
    protected void mostrar(HttpServletRequest req, HttpServletResponse resp, String vista, String titulo)
            throws ServletException, IOException {
        trasladarFlash(req);
        req.setAttribute("titulo", titulo);
        req.setAttribute("entidades", Entidades.todas());
        req.setAttribute("baseUrl", AppConfig.getBaseUrl());
        req.getRequestDispatcher(JSP + vista).forward(req, resp);
    }

    /** Mensaje para la PRÓXIMA página (sobrevive a un redirect). tipo: "ok" | "error". */
    protected void flash(HttpServletRequest req, String tipo, String mensaje) {
        HttpSession s = req.getSession();
        s.setAttribute("flashTipo", tipo);
        s.setAttribute("flashMensaje", mensaje);
    }

    /** Mensaje para la página que se está dibujando ahora mismo. */
    protected void aviso(HttpServletRequest req, String tipo, String mensaje) {
        req.setAttribute("flashTipo", tipo);
        req.setAttribute("flashMensaje", mensaje);
    }

    private void trasladarFlash(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("flashMensaje") == null) {
            return;
        }
        if (req.getAttribute("flashMensaje") == null) {
            req.setAttribute("flashTipo", s.getAttribute("flashTipo"));
            req.setAttribute("flashMensaje", s.getAttribute("flashMensaje"));
        }
        s.removeAttribute("flashTipo");
        s.removeAttribute("flashMensaje");
    }

    protected void redirigir(HttpServletRequest req, HttpServletResponse resp, String ruta) throws IOException {
        resp.sendRedirect(req.getContextPath() + ruta);
    }

    protected static String param(HttpServletRequest req, String nombre) {
        String v = req.getParameter(nombre);
        return v == null ? "" : v.trim();
    }

    /** "Error: ..." -> tipo error; cualquier otro texto del servicio se considera éxito. */
    protected static String tipoDe(String mensajeServicio) {
        return tickets.util.Filas.esError(mensajeServicio) || tickets.util.Filas.noEncontrado(mensajeServicio)
                ? "error" : "ok";
    }
}
