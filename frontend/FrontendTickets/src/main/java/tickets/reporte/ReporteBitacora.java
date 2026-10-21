package tickets.reporte;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reporte formal de la bitácora de un ticket (PDF A4): encabezado institucional con logo, datos del ticket,
 * indicadores, gráfico de tiempo por estado, tabla de transiciones con encabezado repetido y pie con
 * numeración de páginas. Se genera sin librerías externas sobre {@link PdfLienzo}.
 */
public final class ReporteBitacora {

    private static final int AZUL = 0x16709F;
    private static final int AZUL_OSC = 0x0F4C6B;
    private static final int ROJO = 0xC62828;
    private static final int ORO = 0xB38728;
    private static final int TXT = 0x1F2933;
    private static final int GRIS = 0x5B6B7A;
    private static final int BORDE = 0xD5DCE3;
    private static final int FONDO = 0xF3F6F9;
    private static final int BLANCO = 0xFFFFFF;
    private static final int VERDE = 0x2E7D32;
    private static final int AMBAR = 0xB26A00;

    private static final double M = 40;                       // margen lateral y superior
    private static final double LIM_INF = 62;                 // por debajo de esto empieza el pie de página
    private static final double UTIL = PdfLienzo.ANCHO - 2 * M;
    private static final double TAM_TABLA = 8.5;
    private static final double INTERLINEA = 11.5;

    private final PdfLienzo p = new PdfLienzo();
    private final DatosBitacora d;
    private double y;

    private ReporteBitacora(DatosBitacora datos) {
        this.d = datos;
    }

    /** @param logoPng bytes del logo (puede ser null: el reporte sale sin logo) */
    public static byte[] generar(byte[] logoPng, DatosBitacora datos) throws IOException {
        ReporteBitacora r = new ReporteBitacora(datos);
        r.p.cargarImagen(logoPng);
        r.p.metadatos("Reporte de bitácora - Ticket #" + datos.idTicket, "Sistema de Tickets - UMG",
                datos.nombreTicket().replace('—', '-'), fechaPdf(datos.generado));
        r.construir();
        return r.p.generar();
    }

    private void construir() {
        cabeceraCompleta();
        titulo();
        tarjetaTicket();
        indicadores();
        graficoTiempos();
        tablaMovimientos();
        piesDePagina();
    }

    // ------------------------------------------------------------------ encabezados

    private void cabeceraCompleta() {
        double top = PdfLienzo.ALTO - M;
        double tx = M;
        if (p.tieneImagen()) {
            p.imagen(M, top - 58, 58, 58);
            tx = M + 70;
        }
        p.texto(tx, top - 14, "UNIVERSIDAD MARIANO GÁLVEZ DE GUATEMALA", true, 12, AZUL_OSC);
        p.texto(tx, top - 30, "Programación II · Sistema de Control de Tickets (cola FIFO)", false, 9, GRIS);
        p.texto(tx, top - 44, "Reporte generado el " + DatosBitacora.formatoFecha(d.generado) + " a las "
                + DatosBitacora.formatoHora(d.generado), false, 8.5, GRIS);
        p.textoDerecha(PdfLienzo.ANCHO - M, top - 14, "REPORTE DE BITÁCORA", true, 9, ROJO);
        p.textoDerecha(PdfLienzo.ANCHO - M, top - 31, "Ticket #" + d.idTicket, true, 12, TXT);
        double regla = top - 68;
        p.rectRelleno(M, regla, UTIL, 2, AZUL);
        p.rectRelleno(M, regla, 70, 2, ROJO);
        y = regla - 26;
    }

    private void paginaNueva() {
        p.nuevaPagina();
        double top = PdfLienzo.ALTO - M + 8;
        double tx = M;
        if (p.tieneImagen()) {
            p.imagen(M, top - 26, 26, 26);
            tx = M + 34;
        }
        p.texto(tx, top - 11, PdfLienzo.recortar("Bitácora de movimientos — " + d.nombreTicket(), 340, true, 9),
                true, 9, AZUL_OSC);
        p.texto(tx, top - 23, "Ticket #" + d.idTicket, false, 8, GRIS);
        p.textoDerecha(PdfLienzo.ANCHO - M, top - 11, "Generado el " + DatosBitacora.formatoFecha(d.generado)
                + " " + DatosBitacora.formatoHora(d.generado), false, 8, GRIS);
        p.rectRelleno(M, top - 32, UTIL, 1.2, AZUL);
        y = top - 32 - 20;
    }

    private void asegurar(double altura) {
        if (y - altura < LIM_INF) {
            paginaNueva();
        }
    }

    private void titulo() {
        p.texto(M, y - 14, "Bitácora de movimientos del ticket", true, 18, TXT);
        y -= 28;
        List<String> lineas = PdfLienzo.envolver(d.nombreTicket(), UTIL, false, 11);
        for (int i = 0; i < Math.min(2, lineas.size()); i++) {
            String l = lineas.get(i);
            if (i == 1 && lineas.size() > 2) {
                l = PdfLienzo.recortar(l + " ...", UTIL, false, 11);
            }
            p.texto(M, y - 11, l, false, 11, GRIS);
            y -= 15;
        }
        y -= 10;
    }

    private void seccion(String texto) {
        asegurar(60);
        p.texto(M, y - 12, texto, true, 12, AZUL_OSC);
        p.rectRelleno(M, y - 19, UTIL, 0.8, BORDE);
        p.rectRelleno(M, y - 19.4, 40, 1.6, ROJO);
        y -= 32;
    }

    // ------------------------------------------------------------------ datos del ticket e indicadores

    private void tarjetaTicket() {
        List<String[]> items = new ArrayList<>();
        String folio = d.dato("Folio");
        items.add(new String[]{"Folio", folio.isEmpty() ? "#" + d.idTicket : folio});
        items.add(new String[]{"Estado actual", d.estadoActual.isEmpty() ? "—" : d.estadoActual});
        agregar(items, "Categoría", d.dato("Cat"));
        agregar(items, "Prioridad", d.dato("Prio"));
        agregar(items, "Solicitante", d.dato("Solicitante"));
        agregar(items, "Especialista", d.dato("Especialista"));

        int columnas = 3;
        int filas = (items.size() + columnas - 1) / columnas;
        double filaAlto = 34;
        double alto = filas * filaAlto + 12;
        asegurar(alto + 10);
        p.rectRelleno(M, y - alto, UTIL, alto, FONDO);
        p.rectRelleno(M, y - alto, 3.5, alto, AZUL);
        double ancho = (UTIL - 24) / columnas;
        for (int i = 0; i < items.size(); i++) {
            double cx = M + 16 + (i % columnas) * ancho;
            double top = y - 8 - (i / columnas) * filaAlto;
            p.texto(cx, top - 10, items.get(i)[0].toUpperCase(Locale.forLanguageTag("es-GT")), true, 7, GRIS);
            boolean esEstado = "Estado actual".equals(items.get(i)[0]);
            p.texto(cx, top - 25, PdfLienzo.recortar(items.get(i)[1], ancho - 10, esEstado, 10.5), esEstado, 10.5,
                    esEstado ? colorEstado(items.get(i)[1]) : TXT);
        }
        y -= alto + 16;
    }

    private static void agregar(List<String[]> items, String etiqueta, String valor) {
        if (!valor.isEmpty()) {
            items.add(new String[]{etiqueta, valor});
        }
    }

    private void indicadores() {
        double sep = 10;
        double ancho = (UTIL - 3 * sep) / 4;
        double alto = 58;
        asegurar(alto + 10);
        String[] etiquetas = {"Movimientos registrados", "Usuarios participantes",
            d.cicloCerrado ? "Duración del ciclo" : "Tiempo transcurrido", "Estado actual"};
        String[] valores = {String.valueOf(d.movimientos.size()), String.valueOf(d.usuariosDistintos),
            d.segundosTotales == null ? "—" : DatosBitacora.duracion(d.segundosTotales),
            d.estadoActual.isEmpty() ? "—" : d.estadoActual};
        int[] acentos = {AZUL, AZUL, ORO, colorEstado(d.estadoActual)};
        for (int i = 0; i < 4; i++) {
            double x = M + i * (ancho + sep);
            p.rectRelleno(x, y - alto, ancho, alto, BLANCO);
            p.rectBorde(x, y - alto, ancho, alto, 0.8, BORDE);
            p.rectRelleno(x, y - 3.5, ancho, 3.5, acentos[i]);
            boolean grande = i < 2;
            double tam = grande ? 20 : 12;
            String v = PdfLienzo.recortar(valores[i], ancho - 16, true, tam);
            if (i == 2 && PdfLienzo.ancho(v, true, tam) > ancho - 16) {
                v = PdfLienzo.recortar(valores[i], ancho - 16, true, 12);
                tam = 12;
            }
            p.texto(x + 10, y - 30, v, true, tam, i == 3 ? colorEstado(d.estadoActual) : TXT);
            p.texto(x + 10, y - 47, etiquetas[i], false, 8, GRIS);
        }
        y -= alto + 22;
    }

    // ------------------------------------------------------------------ gráfico de tiempo por estado

    private void graficoTiempos() {
        long suma = 0;
        for (long v : d.segundosPorEstado.values()) {
            suma += v;
        }
        if (d.segundosPorEstado.isEmpty() || suma <= 0) {
            return;
        }
        double filaAlto = 24;
        asegurar(32 + d.segundosPorEstado.size() * filaAlto + 24);
        seccion("Tiempo acumulado en cada estado");
        double etiquetaAncho = 112;
        double valorAncho = 112;
        double pista = UTIL - etiquetaAncho - valorAncho;
        boolean hayVigente = false;
        for (Map.Entry<String, Long> e : d.segundosPorEstado.entrySet()) {
            boolean vigente = !d.cicloCerrado && e.getKey().equals(d.estadoActual);
            hayVigente |= vigente;
            String etiqueta = PdfLienzo.recortar(e.getKey() + (vigente ? " *" : ""), etiquetaAncho - 8, false, 9);
            p.texto(M, y - 12, etiqueta, false, 9, TXT);
            p.rectRelleno(M + etiquetaAncho, y - 14, pista, 12, FONDO);
            double parte = (double) e.getValue() / suma;
            p.rectRelleno(M + etiquetaAncho, y - 14, Math.max(2, parte * pista), 12, colorEstado(e.getKey()));
            p.texto(M + etiquetaAncho + pista + 8, y - 12, DatosBitacora.duracion(e.getValue()) + "  ("
                    + String.format(Locale.ROOT, "%.0f%%", parte * 100) + ")", false, 9, TXT);
            y -= filaAlto;
        }
        if (hayVigente) {
            p.texto(M, y - 8, "* Estado vigente: su tiempo se mide hasta la fecha y hora de generación del reporte.",
                    false, 7.5, GRIS);
            y -= 12;
        }
        y -= 14;
    }

    // ------------------------------------------------------------------ tabla de transiciones

    private final double[] cw = {24, 78, 98, 76, 76, UTIL - 24 - 78 - 98 - 76 - 76};
    private static final String[] ENCABEZADOS = {"No.", "Fecha y hora", "Usuario", "Desde", "Hacia", "Nota"};

    private void tablaMovimientos() {
        seccion("Línea de tiempo de transiciones");
        if (d.movimientos.isEmpty()) {
            asegurar(50);
            p.rectRelleno(M, y - 42, UTIL, 42, FONDO);
            p.textoCentro(M + UTIL / 2, y - 25, "Este ticket aún no tiene movimientos registrados.", false, 10, GRIS);
            y -= 52;
            return;
        }
        asegurar(20 + 36);
        encabezadoTabla();
        int idx = 0;
        for (DatosBitacora.Movimiento m : d.movimientos) {
            List<String> fecha = fechaLineas(m, cw[1] - 10);
            List<String> usuario = PdfLienzo.envolver(m.usuario.isEmpty() ? "—" : m.usuario, cw[2] - 10, false, TAM_TABLA);
            List<String> nota = PdfLienzo.envolver(m.nota.isEmpty() ? "—" : m.nota, cw[5] - 10, false, TAM_TABLA);
            int lineas = Math.max(1, Math.max(fecha.size(), Math.max(usuario.size(), nota.size())));
            double alto = Math.max(26, lineas * INTERLINEA + 10);
            if (y - alto < LIM_INF) {
                paginaNueva();
                encabezadoTabla();
            }
            if (idx % 2 == 1) {
                p.rectRelleno(M, y - alto, UTIL, alto, FONDO);
            }
            p.rectRelleno(M, y - alto, UTIL, 0.5, BORDE);
            double x = M;
            p.texto(x + 5, y - 5 - TAM_TABLA, String.valueOf(m.numero), true, TAM_TABLA, GRIS);
            x += cw[0];
            dibujarLineas(fecha, x + 5, y, TXT);
            x += cw[1];
            dibujarLineas(usuario, x + 5, y, TXT);
            x += cw[2];
            pildora(m.de, x + 5, y - 5, cw[3] - 10);
            x += cw[3];
            pildora(m.a, x + 5, y - 5, cw[4] - 10);
            x += cw[4];
            dibujarLineas(nota, x + 5, y, m.nota.isEmpty() ? GRIS : TXT);
            y -= alto;
            idx++;
        }
        p.rectRelleno(M, y - 0.8, UTIL, 0.8, AZUL);
        y -= 14;
        asegurar(14);
        p.texto(M, y - 8, "Fin del reporte · " + d.movimientos.size() + (d.movimientos.size() == 1
                ? " movimiento registrado." : " movimientos registrados."), false, 8, GRIS);
    }

    private List<String> fechaLineas(DatosBitacora.Movimiento m, double ancho) {
        if (m.fecha != null) {
            List<String> l = new ArrayList<>();
            l.add(DatosBitacora.formatoFecha(m.fecha));
            l.add(DatosBitacora.formatoHora(m.fecha));
            return l;
        }
        return PdfLienzo.envolver(m.fechaTexto.isEmpty() ? "—" : m.fechaTexto, ancho, false, TAM_TABLA);
    }

    private void dibujarLineas(List<String> lineas, double x, double top, int color) {
        double base = top - 5 - TAM_TABLA;
        for (String l : lineas) {
            p.texto(x, base, l, false, TAM_TABLA, color);
            base -= INTERLINEA;
        }
    }

    private void encabezadoTabla() {
        double alto = 20;
        p.rectRelleno(M, y - alto, UTIL, alto, AZUL);
        double x = M;
        for (int i = 0; i < ENCABEZADOS.length; i++) {
            p.texto(x + 5, y - 13.5, ENCABEZADOS[i], true, TAM_TABLA, BLANCO);
            x += cw[i];
        }
        y -= alto;
    }

    /** Etiqueta de estado con fondo suave del color del estado. */
    private void pildora(String estado, double x, double top, double anchoMax) {
        if (estado.isEmpty()) {
            p.texto(x, top - TAM_TABLA - 1, "—", false, TAM_TABLA, GRIS);
            return;
        }
        double tam = 8;
        String mostrado = DatosBitacora.sinAcentos(estado).startsWith("ninguno") ? "Inicio" : estado;
        String texto = PdfLienzo.recortar(mostrado, anchoMax - 10, true, tam);
        double w = PdfLienzo.ancho(texto, true, tam) + 10;
        int color = colorEstado(estado);
        p.rectRelleno(x, top - 14, w, 14, mezclar(color, 0.86));
        p.texto(x + 5, top - 10.2, texto, true, tam, color);
    }

    // ------------------------------------------------------------------ pie de página

    private void piesDePagina() {
        int total = p.totalPaginas();
        for (int i = 0; i < total; i++) {
            p.irAPagina(i);
            p.rectRelleno(M, 46, UTIL, 0.6, BORDE);
            p.texto(M, 34, "Sistema de Tickets · Programación II · Universidad Mariano Gálvez de Guatemala", false, 7.5, GRIS);
            p.textoDerecha(PdfLienzo.ANCHO - M, 34, "Página " + (i + 1) + " de " + total, true, 7.5, GRIS);
        }
    }

    // ------------------------------------------------------------------ colores

    static int colorEstado(String estado) {
        String e = DatosBitacora.sinAcentos(estado);
        if (e.contains("ninguno") || e.isEmpty()) {
            return GRIS;
        }
        if (DatosBitacora.esEstadoFinal(estado)) {
            return VERDE;
        }
        if (e.contains("proceso") || e.contains("atenc") || e.contains("curso") || e.contains("progres")) {
            return AMBAR;
        }
        return AZUL;
    }

    /** Mezcla un color con blanco: 0 = el color puro, 1 = blanco. */
    private static int mezclar(int rgb, double haciaBlanco) {
        int r = (int) Math.round(((rgb >> 16) & 0xFF) + (255 - ((rgb >> 16) & 0xFF)) * haciaBlanco);
        int g = (int) Math.round(((rgb >> 8) & 0xFF) + (255 - ((rgb >> 8) & 0xFF)) * haciaBlanco);
        int b = (int) Math.round((rgb & 0xFF) + (255 - (rgb & 0xFF)) * haciaBlanco);
        return (r << 16) | (g << 8) | b;
    }

    private static String fechaPdf(LocalDateTime f) {
        int minutos = java.time.ZoneId.systemDefault().getRules().getOffset(f).getTotalSeconds() / 60;
        return String.format(Locale.ROOT, "D:%04d%02d%02d%02d%02d%02d%s%02d'%02d'", f.getYear(), f.getMonthValue(),
                f.getDayOfMonth(), f.getHour(), f.getMinute(), f.getSecond(), minutos < 0 ? "-" : "+",
                Math.abs(minutos) / 60, Math.abs(minutos) % 60);
    }
}
