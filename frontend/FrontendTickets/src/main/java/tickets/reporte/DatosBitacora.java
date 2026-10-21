package tickets.reporte;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Datos ya procesados de la bitácora de un ticket, compartidos por el reporte PDF y el de Excel:
 * movimientos con fecha interpretada, estado actual, usuarios que participaron y tiempo en cada estado.
 */
public final class DatosBitacora {

    /** Un cambio de estado. {@code fecha} es null si el texto del servicio no se pudo interpretar. */
    public static final class Movimiento {
        public final int numero;
        public final LocalDateTime fecha;
        public final String fechaTexto;
        public final String usuario;
        public final String de;
        public final String a;
        public final String nota;

        Movimiento(int numero, LocalDateTime fecha, String fechaTexto, String usuario, String de, String a, String nota) {
            this.numero = numero;
            this.fecha = fecha;
            this.fechaTexto = fechaTexto;
            this.usuario = usuario;
            this.de = de;
            this.a = a;
            this.nota = nota;
        }
    }

    private static final Pattern FECHA = Pattern.compile(
            "(\\d{4})-(\\d{2})-(\\d{2})[ T](\\d{2}):(\\d{2}):(\\d{2})");

    public final String idTicket;
    public final Map<String, String> ticket;           // fila del ticket (puede venir vacía)
    public final List<Movimiento> movimientos;
    public final LocalDateTime generado;
    public final String estadoActual;
    public final int usuariosDistintos;
    public final boolean cicloCerrado;                 // el último estado es final (resuelto/cerrado)
    public final Long segundosTotales;                 // null si las fechas no se pudieron interpretar
    public final Map<String, Long> segundosPorEstado;  // vacío si no hay fechas válidas

    private DatosBitacora(String idTicket, Map<String, String> ticket, List<Movimiento> movimientos,
            LocalDateTime generado, String estadoActual, int usuarios, boolean cerrado, Long total,
            Map<String, Long> porEstado) {
        this.idTicket = idTicket;
        this.ticket = ticket;
        this.movimientos = movimientos;
        this.generado = generado;
        this.estadoActual = estadoActual;
        this.usuariosDistintos = usuarios;
        this.cicloCerrado = cerrado;
        this.segundosTotales = total;
        this.segundosPorEstado = porEstado;
    }

    /**
     * @param filas   filas de WSHistorial ya separadas en claves Fecha, Usuario, De, A, Nota
     * @param ticket  fila del ticket (Folio, Título, Cat, Prio, Estado, Solicitante...) o null si no se pudo leer
     */
    public static DatosBitacora crear(String idTicket, Map<String, String> ticket,
            List<Map<String, String>> filas, LocalDateTime generado) {
        List<Movimiento> movs = new ArrayList<>();
        Set<String> usuarios = new LinkedHashSet<>();
        int n = 1;
        for (Map<String, String> f : filas) {
            String fechaTexto = limpio(f.get("Fecha"));
            Movimiento m = new Movimiento(n++, interpretar(fechaTexto), fechaTexto, limpio(f.get("Usuario")),
                    limpio(f.get("De")), limpio(f.get("A")), limpio(f.get("Nota")));
            movs.add(m);
            if (!m.usuario.isEmpty()) {
                usuarios.add(m.usuario);
            }
        }
        Map<String, String> t = ticket == null ? Collections.<String, String>emptyMap() : ticket;

        String estado = limpio(t.get("Estado"));
        if (estado.isEmpty() && !movs.isEmpty()) {
            estado = movs.get(movs.size() - 1).a;
        }
        boolean cerrado = esEstadoFinal(movs.isEmpty() ? estado : movs.get(movs.size() - 1).a);

        // tiempo en cada estado: de un movimiento al siguiente; el estado vigente cuenta hasta "ahora"
        boolean fechasValidas = !movs.isEmpty();
        for (Movimiento m : movs) {
            if (m.fecha == null) {
                fechasValidas = false;
            }
        }
        Map<String, Long> porEstado = new LinkedHashMap<>();
        Long total = null;
        if (fechasValidas) {
            for (int i = 0; i < movs.size(); i++) {
                Movimiento m = movs.get(i);
                LocalDateTime fin = i + 1 < movs.size() ? movs.get(i + 1).fecha : (cerrado ? m.fecha : generado);
                long seg = Math.max(0, Duration.between(m.fecha, fin).getSeconds());
                porEstado.merge(m.a, seg, Long::sum);
            }
            LocalDateTime ultimo = cerrado ? movs.get(movs.size() - 1).fecha : generado;
            total = Math.max(0, Duration.between(movs.get(0).fecha, ultimo).getSeconds());
        }
        return new DatosBitacora(idTicket, t, Collections.unmodifiableList(movs), generado, estado,
                usuarios.size(), cerrado, total, porEstado);
    }

    // ------------------------------------------------------------------ utilidades compartidas

    static String limpio(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return "null".equalsIgnoreCase(t) ? "" : t;
    }

    static LocalDateTime interpretar(String texto) {
        Matcher m = FECHA.matcher(texto == null ? "" : texto);
        if (!m.find()) {
            return null;
        }
        try {
            return LocalDateTime.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)),
                    Integer.parseInt(m.group(6)));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    static String sinAcentos(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    public static boolean esEstadoFinal(String estado) {
        String e = sinAcentos(estado);
        return e.contains("resuel") || e.contains("cerrad") || e.contains("final") || e.contains("complet");
    }

    /** "3 d 4 h", "2 h 15 min", "12 min", "menos de 1 min". */
    public static String duracion(long segundos) {
        long min = segundos / 60;
        if (min < 1) {
            return "menos de 1 min";
        }
        long dias = min / 1440;
        long horas = (min % 1440) / 60;
        long minutos = min % 60;
        if (dias > 0) {
            return dias + " d " + horas + " h";
        }
        if (horas > 0) {
            return horas + " h " + minutos + " min";
        }
        return minutos + " min";
    }

    public static String formatoFecha(LocalDateTime f) {
        return f == null ? "" : String.format(Locale.ROOT, "%02d/%02d/%04d", f.getDayOfMonth(), f.getMonthValue(), f.getYear());
    }

    public static String formatoHora(LocalDateTime f) {
        return f == null ? "" : String.format(Locale.ROOT, "%02d:%02d:%02d", f.getHour(), f.getMinute(), f.getSecond());
    }

    /** Valor de la fila del ticket, o "" si no existe. */
    public String dato(String clave) {
        return limpio(ticket.get(clave));
    }

    /** Nombre del ticket para títulos: "Folio — Título", o "Ticket #id" si no se pudo leer. */
    public String nombreTicket() {
        String folio = dato("Folio");
        String titulo = dato("Título");
        if (!folio.isEmpty() && !titulo.isEmpty()) {
            return folio + " — " + titulo;
        }
        if (!folio.isEmpty()) {
            return folio;
        }
        return "Ticket #" + idTicket;
    }
}
