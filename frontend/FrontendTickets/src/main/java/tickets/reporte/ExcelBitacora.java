package tickets.reporte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Libro de Excel (.xlsx) con la bitácora de un ticket, generado sin librerías externas (OOXML + ZIP).
 * Hoja "Resumen": datos del ticket, indicadores y tiempo por estado. Hoja "Bitácora": tabla con filtros,
 * fechas reales de Excel, encabezado fijo y configuración de impresión horizontal.
 * Los textos se guardan como cadenas, por lo que una nota como "=1+1" nunca se ejecuta como fórmula.
 */
public final class ExcelBitacora {

    // estilos (índices de cellXfs en styles.xml)
    private static final int S_DEFECTO = 0;
    private static final int S_ENCABEZADO = 1;
    private static final int S_TITULO = 2;
    private static final int S_SUBTITULO = 3;
    private static final int S_CELDA = 4;
    private static final int S_FECHA = 5;
    private static final int S_CENTRO = 6;
    private static final int S_ETIQUETA = 7;
    private static final int S_VALOR = 8;
    private static final int S_PORCENTAJE = 9;

    private static final LocalDateTime ORIGEN_EXCEL = LocalDateTime.of(1899, 12, 30, 0, 0);

    private ExcelBitacora() {
    }

    public static byte[] generar(DatosBitacora d) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            entrada(zip, "[Content_Types].xml", tiposContenido());
            entrada(zip, "_rels/.rels", relacionesRaiz());
            entrada(zip, "xl/workbook.xml", libro(d));
            entrada(zip, "xl/_rels/workbook.xml.rels", relacionesLibro());
            entrada(zip, "xl/styles.xml", estilos());
            entrada(zip, "xl/worksheets/sheet1.xml", hojaResumen(d));
            entrada(zip, "xl/worksheets/sheet2.xml", hojaBitacora(d));
        }
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ hojas

    private static String hojaResumen(DatosBitacora d) {
        Hoja h = new Hoja();
        h.celda(1, 1, "Reporte de bitácora — Ticket #" + d.idTicket, S_TITULO);
        h.celda(2, 1, "Universidad Mariano Gálvez de Guatemala · Programación II · Sistema de Control de Tickets",
                S_SUBTITULO);
        h.celda(3, 1, "Generado el " + DatosBitacora.formatoFecha(d.generado) + " a las "
                + DatosBitacora.formatoHora(d.generado), S_SUBTITULO);
        h.unir("A1:C1");
        h.unir("A2:C2");
        h.unir("A3:C3");

        int f = 5;
        f = par(h, f, "Folio", d.dato("Folio").isEmpty() ? "#" + d.idTicket : d.dato("Folio"));
        f = par(h, f, "Título", d.dato("Título"));
        f = par(h, f, "Categoría", d.dato("Cat"));
        f = par(h, f, "Prioridad", d.dato("Prio"));
        f = par(h, f, "Solicitante", d.dato("Solicitante"));
        f = par(h, f, "Especialista", d.dato("Especialista"));
        f = par(h, f, "Estado actual", d.estadoActual);
        f = par(h, f, "Movimientos registrados", String.valueOf(d.movimientos.size()));
        f = par(h, f, "Usuarios participantes", String.valueOf(d.usuariosDistintos));
        f = par(h, f, d.cicloCerrado ? "Duración del ciclo" : "Tiempo transcurrido",
                d.segundosTotales == null ? "" : DatosBitacora.duracion(d.segundosTotales));

        long suma = 0;
        for (long v : d.segundosPorEstado.values()) {
            suma += v;
        }
        if (!d.segundosPorEstado.isEmpty() && suma > 0) {
            f++;
            h.celda(f, 1, "Estado", S_ENCABEZADO);
            h.celda(f, 2, "Tiempo acumulado", S_ENCABEZADO);
            h.celda(f, 3, "Porcentaje", S_ENCABEZADO);
            f++;
            for (Map.Entry<String, Long> e : d.segundosPorEstado.entrySet()) {
                boolean vigente = !d.cicloCerrado && e.getKey().equals(d.estadoActual);
                h.celda(f, 1, e.getKey() + (vigente ? " *" : ""), S_CELDA);
                h.celda(f, 2, DatosBitacora.duracion(e.getValue()), S_CELDA);
                h.numero(f, 3, (double) e.getValue() / suma, S_PORCENTAJE);
                f++;
            }
            if (!d.cicloCerrado) {
                h.celda(f, 1, "* Estado vigente: se mide hasta la fecha y hora de generación del reporte.", S_SUBTITULO);
                h.unir("A" + f + ":C" + f);
            }
        }
        h.columnas(28, 62, 16);
        return h.construir(null, null, false);
    }

    private static int par(Hoja h, int fila, String etiqueta, String valor) {
        if (valor == null || valor.isEmpty()) {
            return fila;
        }
        h.celda(fila, 1, etiqueta, S_ETIQUETA);
        h.celda(fila, 2, valor, S_VALOR);
        return fila + 1;
    }

    private static String hojaBitacora(DatosBitacora d) {
        Hoja h = new Hoja();
        h.celda(1, 1, "Bitácora de movimientos — " + d.nombreTicket(), S_TITULO);
        h.unir("A1:G1");
        String[] enc = {"No.", "Ticket", "Fecha y hora", "Usuario", "Desde", "Hacia", "Nota"};
        for (int i = 0; i < enc.length; i++) {
            h.celda(3, i + 1, enc[i], S_ENCABEZADO);
        }
        int fila = 4;
        for (DatosBitacora.Movimiento m : d.movimientos) {
            h.numero(fila, 1, m.numero, S_CENTRO);
            h.celda(fila, 2, d.dato("Folio").isEmpty() ? "#" + d.idTicket : d.dato("Folio"), S_CELDA);
            if (m.fecha != null) {
                h.numero(fila, 3, serial(m.fecha), S_FECHA);
            } else {
                h.celda(fila, 3, m.fechaTexto, S_CELDA);
            }
            h.celda(fila, 4, m.usuario, S_CELDA);
            h.celda(fila, 5, m.de, S_CELDA);
            h.celda(fila, 6, m.a, S_CELDA);
            h.celda(fila, 7, m.nota, S_CELDA);
            fila++;
        }
        if (d.movimientos.isEmpty()) {
            h.celda(4, 1, "Este ticket aún no tiene movimientos registrados.", S_SUBTITULO);
            h.unir("A4:G4");
            fila = 4;
        }
        h.columnas(7, 16, 21, 26, 18, 18, 60);
        int ultima = Math.max(4, fila - 1);
        return h.construir("A3:G" + ultima, "A4", true);
    }

    static double serial(LocalDateTime f) {
        return ChronoUnit.SECONDS.between(ORIGEN_EXCEL, f) / 86400.0;
    }

    // ------------------------------------------------------------------ construcción de una hoja

    private static final class Hoja {
        private final List<int[]> posiciones = new ArrayList<>();
        private final List<String> celdas = new ArrayList<>();
        private final List<String> uniones = new ArrayList<>();
        private double[] anchos = new double[0];

        void celda(int fila, int col, String texto, int estilo) {
            posiciones.add(new int[]{fila, col});
            celdas.add("<c r=\"" + ref(fila, col) + "\" s=\"" + estilo + "\" t=\"inlineStr\"><is><t xml:space=\"preserve\">"
                    + xml(texto) + "</t></is></c>");
        }

        void numero(int fila, int col, double valor, int estilo) {
            posiciones.add(new int[]{fila, col});
            celdas.add("<c r=\"" + ref(fila, col) + "\" s=\"" + estilo + "\"><v>"
                    + String.format(Locale.ROOT, "%.10f", valor).replaceAll("0+$", "").replaceAll("\\.$", "") + "</v></c>");
        }

        void unir(String rango) {
            uniones.add(rango);
        }

        void columnas(double... a) {
            anchos = a;
        }

        String construir(String filtro, String congelarEn, boolean horizontal) {
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
                    .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
            sb.append("<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>");
            sb.append("<sheetViews><sheetView workbookViewId=\"0\" showGridLines=\"0\"");
            if (congelarEn == null) {
                sb.append("/>");
            } else {
                sb.append("><pane ySplit=\"3\" topLeftCell=\"").append(congelarEn)
                        .append("\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView>");
            }
            sb.append("</sheetViews><sheetFormatPr defaultRowHeight=\"15\"/>");
            if (anchos.length > 0) {
                sb.append("<cols>");
                for (int i = 0; i < anchos.length; i++) {
                    sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1).append("\" width=\"")
                            .append(anchos[i]).append("\" customWidth=\"1\"/>");
                }
                sb.append("</cols>");
            }
            sb.append("<sheetData>");
            int maxFila = 0;
            for (int[] pos : posiciones) {
                maxFila = Math.max(maxFila, pos[0]);
            }
            for (int fila = 1; fila <= maxFila; fila++) {
                StringBuilder fx = new StringBuilder();
                List<Integer> orden = new ArrayList<>();
                for (int i = 0; i < posiciones.size(); i++) {
                    if (posiciones.get(i)[0] == fila) {
                        orden.add(i);
                    }
                }
                if (orden.isEmpty()) {
                    continue;
                }
                orden.sort((a, b) -> Integer.compare(posiciones.get(a)[1], posiciones.get(b)[1]));
                for (int i : orden) {
                    fx.append(celdas.get(i));
                }
                String alto = fila == 1 ? " ht=\"26\" customHeight=\"1\"" : "";
                sb.append("<row r=\"").append(fila).append("\"").append(alto).append(">").append(fx).append("</row>");
            }
            sb.append("</sheetData>");
            if (filtro != null) {
                sb.append("<autoFilter ref=\"").append(filtro).append("\"/>");
            }
            if (!uniones.isEmpty()) {
                sb.append("<mergeCells count=\"").append(uniones.size()).append("\">");
                for (String u : uniones) {
                    sb.append("<mergeCell ref=\"").append(u).append("\"/>");
                }
                sb.append("</mergeCells>");
            }
            sb.append("<pageMargins left=\"0.5\" right=\"0.5\" top=\"0.6\" bottom=\"0.6\" header=\"0.3\" footer=\"0.3\"/>");
            sb.append("<pageSetup paperSize=\"9\" orientation=\"").append(horizontal ? "landscape" : "portrait")
                    .append("\" fitToWidth=\"1\" fitToHeight=\"0\"/>");
            sb.append("<headerFooter><oddFooter>&amp;LSistema de Tickets · UMG&amp;RPágina &amp;P de &amp;N</oddFooter></headerFooter>");
            sb.append("</worksheet>");
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------ partes fijas del paquete

    private static String tiposContenido() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                + "</Types>";
    }

    private static String relacionesRaiz() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private static String libro(DatosBitacora d) {
        int ultima = Math.max(4, d.movimientos.size() + 3);
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<bookViews><workbookView activeTab=\"0\"/></bookViews>"
                + "<sheets><sheet name=\"Resumen\" sheetId=\"1\" r:id=\"rId1\"/>"
                + "<sheet name=\"Bitácora\" sheetId=\"2\" r:id=\"rId2\"/></sheets>"
                + "<definedNames><definedName name=\"_xlnm._FilterDatabase\" localSheetId=\"1\" hidden=\"1\">"
                + "'Bitácora'!$A$3:$G$" + ultima + "</definedName></definedNames>"
                + "</workbook>";
    }

    private static String relacionesLibro() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>"
                + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
                + "</Relationships>";
    }

    private static String estilos() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<numFmts count=\"2\"><numFmt numFmtId=\"164\" formatCode=\"dd/mm/yyyy\\ hh:mm:ss\"/>"
                + "<numFmt numFmtId=\"165\" formatCode=\"0.0%\"/></numFmts>"
                + "<fonts count=\"5\">"
                + "<font><sz val=\"11\"/><color rgb=\"FF1F2933\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
                + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
                + "<font><b/><sz val=\"16\"/><color rgb=\"FF0F4C6B\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
                + "<font><i/><sz val=\"9\"/><color rgb=\"FF5B6B7A\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
                + "<font><b/><sz val=\"11\"/><color rgb=\"FF1F2933\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
                + "</fonts>"
                + "<fills count=\"4\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF16709F\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF3F6F9\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
                + "<borders count=\"2\"><border><left/><right/><top/><bottom/><diagonal/></border>"
                + "<border><left style=\"thin\"><color rgb=\"FFD5DCE3\"/></left><right style=\"thin\"><color rgb=\"FFD5DCE3\"/></right>"
                + "<top style=\"thin\"><color rgb=\"FFD5DCE3\"/></top><bottom style=\"thin\"><color rgb=\"FFD5DCE3\"/></bottom><diagonal/></border></borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"10\">"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyAlignment=\"1\"><alignment vertical=\"center\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"top\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"top\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"4\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"165\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"top\"/></xf>"
                + "</cellXfs>"
                + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
                + "</styleSheet>";
    }

    // ------------------------------------------------------------------ utilidades

    private static void entrada(ZipOutputStream zip, String nombre, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String ref(int fila, int col) {
        return (char) ('A' + col - 1) + String.valueOf(fila);
    }

    /** Escapa XML y elimina caracteres de control que harían inválido el archivo. */
    private static String xml(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') {
                continue;
            }
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
