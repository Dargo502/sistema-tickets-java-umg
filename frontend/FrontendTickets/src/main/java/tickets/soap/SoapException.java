package tickets.soap;

/** Error de comunicación con el servidor de servicios (red, HTTP, SOAP Fault o XML inválido). */
public class SoapException extends Exception {
    private static final long serialVersionUID = 1L;

    public SoapException(String mensaje) {
        super(mensaje);
    }

    public SoapException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
