package tickets.soap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import tickets.config.AppConfig;

/**
 * Cliente SOAP 1.1 mínimo (sin clases generadas) para los servicios JAX-WS del backend.
 * Cada servicio vive en  {baseUrl}/{NombreServicio}  y usa el namespace http://servicios/.
 * Los parámetros se envían como elementos sin namespace (así los publica JAX-WS con @WebParam).
 * Las respuestas se leen de los elementos return.
 */
public final class SoapClient {

    private static final String NS_SERVICIOS = "http://servicios/";
    private static final int TIMEOUT_CONEXION_MS = 5000;
    private static final int TIMEOUT_LECTURA_MS = 15000;
    private static final Pattern NOMBRE_XML = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    private SoapClient() { }

    /** Invoca una operación y devuelve todos los elementos return (lista vacía si no hay ninguno). */
    public static List<String> invocar(String servicio, String operacion, Map<String, String> parametros)
            throws SoapException {
        validarNombre(servicio);
        validarNombre(operacion);
        Map<String, String> params = parametros == null ? Collections.<String, String>emptyMap() : parametros;
        for (String nombre : params.keySet()) {
            validarNombre(nombre);
        }

        String url = AppConfig.getBaseUrl() + "/" + servicio;
        byte[] cuerpo = construirSobre(operacion, params).getBytes(StandardCharsets.UTF_8);
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) java.net.URI.create(url).toURL().openConnection();
            con.setConnectTimeout(TIMEOUT_CONEXION_MS);
            con.setReadTimeout(TIMEOUT_LECTURA_MS);
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            con.setRequestProperty("SOAPAction", "\"\"");
            con.setFixedLengthStreamingMode(cuerpo.length);
            try (OutputStream os = con.getOutputStream()) {
                os.write(cuerpo);
            }
            int codigo = con.getResponseCode();
            InputStream in = codigo >= 400 ? con.getErrorStream() : con.getInputStream();
            byte[] respuesta = in == null ? new byte[0] : in.readAllBytes();
            return interpretar(respuesta, codigo, url);
        } catch (IllegalArgumentException e) {
            throw new SoapException("La URL del servicio no es válida: " + url, e);
        } catch (IOException e) {
            throw new SoapException("No se pudo conectar con " + url + " (" + e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : ": " + e.getMessage()) + ")", e);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    /** Invoca una operación que devuelve un solo String ("" si no vino ninguno). */
    public static String invocarTexto(String servicio, String operacion, Map<String, String> parametros)
            throws SoapException {
        List<String> r = invocar(servicio, operacion, parametros);
        return r.isEmpty() ? "" : r.get(0);
    }

    // ------------------------------------------------------------------ interno

    static String construirSobre(String operacion, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
          .append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"")
          .append(NS_SERVICIOS).append("\"><soapenv:Header/><soapenv:Body><ser:")
          .append(operacion).append('>');
        for (Map.Entry<String, String> p : params.entrySet()) {
            sb.append('<').append(p.getKey()).append('>')
              .append(escaparXml(p.getValue()))
              .append("</").append(p.getKey()).append('>');
        }
        sb.append("</ser:").append(operacion).append("></soapenv:Body></soapenv:Envelope>");
        return sb.toString();
    }

    static List<String> interpretar(byte[] respuesta, int codigoHttp, String url) throws SoapException {
        if (respuesta.length == 0) {
            throw new SoapException("Respuesta vacía de " + url + " (HTTP " + codigoHttp + ").");
        }
        Document doc;
        try {
            doc = parsear(respuesta);
        } catch (Exception e) {
            throw new SoapException("Respuesta no válida de " + url + " (HTTP " + codigoHttp
                    + "). Verifique que la URL base y el nombre del servicio sean correctos.", e);
        }
        Element body = hijo(doc.getDocumentElement(), "Body");
        if (body == null) {
            throw new SoapException("La respuesta de " + url + " no es un mensaje SOAP (HTTP " + codigoHttp + ").");
        }
        Element primero = primerElemento(body);
        List<String> resultado = new ArrayList<>();
        if (primero == null) {
            return resultado;
        }
        if ("Fault".equals(nombreLocal(primero))) {
            String texto = textoDe(primero, "faultstring");
            throw new SoapException("El servicio devolvió un error SOAP: " + (texto.isEmpty() ? "(sin detalle)" : texto));
        }
        for (Node n = primero.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE && "return".equals(nombreLocal(n))) {
                resultado.add(n.getTextContent());
            }
        }
        return resultado;
    }

    private static Document parsear(byte[] xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        try {
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException ignorado) {
            // el parser del servidor puede no soportarlo
        }
        try {
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (ParserConfigurationException ignorado) {
            // idem
        }
        DocumentBuilder b = f.newDocumentBuilder();
        // evita que el parser escriba "[Fatal Error]" en la consola del servidor con respuestas no XML
        b.setErrorHandler(new ErrorHandler() {
            @Override public void warning(SAXParseException e) { }
            @Override public void error(SAXParseException e) { }
            @Override public void fatalError(SAXParseException e) throws SAXException { throw e; }
        });
        return b.parse(new ByteArrayInputStream(xml));
    }

    private static Element hijo(Element padre, String local) {
        for (Node n = padre.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE && local.equals(nombreLocal(n))) {
                return (Element) n;
            }
        }
        return null;
    }

    private static Element primerElemento(Element padre) {
        for (Node n = padre.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) n;
            }
        }
        return null;
    }

    private static String textoDe(Element raiz, String local) {
        for (Node n = raiz.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (local.equals(nombreLocal(n))) {
                    return n.getTextContent().trim();
                }
                String interno = textoDe((Element) n, local);
                if (!interno.isEmpty()) {
                    return interno;
                }
            }
        }
        return "";
    }

    private static String nombreLocal(Node n) {
        String l = n.getLocalName();
        if (l != null) {
            return l;
        }
        String nombre = n.getNodeName();
        int i = nombre.indexOf(':');
        return i >= 0 ? nombre.substring(i + 1) : nombre;
    }

    private static void validarNombre(String nombre) throws SoapException {
        if (nombre == null || !NOMBRE_XML.matcher(nombre).matches()) {
            throw new SoapException("Nombre de servicio/operación/parámetro no válido: " + nombre);
        }
    }

    static String escaparXml(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&apos;"); break;
                default:
                    // se descartan caracteres de control que XML 1.0 no admite
                    if (c >= 0x20 || c == '\t' || c == '\n' || c == '\r') {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
