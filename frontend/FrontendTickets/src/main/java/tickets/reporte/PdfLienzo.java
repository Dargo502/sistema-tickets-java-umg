package tickets.reporte;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Deflater;
import javax.imageio.ImageIO;

/**
 * Motor mínimo de PDF (A4) sin librerías externas: texto Helvetica (WinAnsi), rectángulos, líneas,
 * una imagen PNG con transparencia (el logo), varias páginas, metadatos y compresión Flate.
 * El origen de coordenadas es la esquina inferior izquierda de la página (en puntos).
 */
public final class PdfLienzo {

    public static final double ANCHO = 595.28;
    public static final double ALTO = 841.89;

    private static final Charset WIN_ANSI = Charset.forName("windows-1252");

    /** Anchos Helvetica (AFM) de los caracteres ASCII 32..126, en milésimas de em. */
    private static final int[] W_NORMAL = {
        278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
        1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
        667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
        333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
        556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584};
    /** Anchos Helvetica-Bold (AFM) de los caracteres ASCII 32..126. */
    private static final int[] W_NEGRITA = {
        278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 333, 333, 584, 584, 584, 611,
        975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833, 722, 778,
        667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556,
        333, 556, 611, 556, 611, 556, 333, 611, 611, 278, 278, 556, 278, 889, 611, 611,
        611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500, 389, 280, 389, 584};

    private final List<StringBuilder> paginas = new ArrayList<>();
    private StringBuilder actual;
    private int indiceActual;

    private byte[] imagenRgb;
    private byte[] imagenAlfa;
    private int imagenAncho;
    private int imagenAlto;

    private String titulo = "";
    private String autor = "";
    private String asunto = "";
    private String fechaCreacion = "";

    public PdfLienzo() {
        nuevaPagina();
    }

    // ------------------------------------------------------------------ páginas y metadatos

    public int nuevaPagina() {
        actual = new StringBuilder(4096);
        paginas.add(actual);
        indiceActual = paginas.size() - 1;
        return indiceActual;
    }

    public int totalPaginas() {
        return paginas.size();
    }

    /** Vuelve a una página ya creada (0 = primera), por ejemplo para dibujar el "Página X de N" al final. */
    public void irAPagina(int indice) {
        actual = paginas.get(indice);
        indiceActual = indice;
    }

    public int paginaActual() {
        return indiceActual;
    }

    public void metadatos(String titulo, String autor, String asunto, String fechaPdf) {
        this.titulo = titulo == null ? "" : titulo;
        this.autor = autor == null ? "" : autor;
        this.asunto = asunto == null ? "" : asunto;
        this.fechaCreacion = fechaPdf == null ? "" : fechaPdf;
    }

    // ------------------------------------------------------------------ dibujo

    public void rectRelleno(double x, double y, double w, double h, int rgb) {
        actual.append("q ").append(color(rgb)).append(" rg ")
                .append(n(x)).append(' ').append(n(y)).append(' ').append(n(w)).append(' ').append(n(h))
                .append(" re f Q\n");
    }

    public void rectBorde(double x, double y, double w, double h, double grosor, int rgb) {
        actual.append("q ").append(color(rgb)).append(" RG ").append(n(grosor)).append(" w ")
                .append(n(x)).append(' ').append(n(y)).append(' ').append(n(w)).append(' ').append(n(h))
                .append(" re S Q\n");
    }

    public void linea(double x1, double y1, double x2, double y2, double grosor, int rgb) {
        actual.append("q ").append(color(rgb)).append(" RG ").append(n(grosor)).append(" w ")
                .append(n(x1)).append(' ').append(n(y1)).append(" m ")
                .append(n(x2)).append(' ').append(n(y2)).append(" l S Q\n");
    }

    public void texto(double x, double y, String texto, boolean negrita, double tamano, int rgb) {
        if (texto == null || texto.isEmpty()) {
            return;
        }
        actual.append("q BT ").append(color(rgb)).append(" rg /").append(negrita ? "F2" : "F1").append(' ')
                .append(n(tamano)).append(" Tf ").append(n(x)).append(' ').append(n(y)).append(" Td (")
                .append(literal(texto)).append(") Tj ET Q\n");
    }

    public void textoDerecha(double xDerecha, double y, String texto, boolean negrita, double tamano, int rgb) {
        texto(xDerecha - ancho(texto, negrita, tamano), y, texto, negrita, tamano, rgb);
    }

    public void textoCentro(double xCentro, double y, String texto, boolean negrita, double tamano, int rgb) {
        texto(xCentro - ancho(texto, negrita, tamano) / 2, y, texto, negrita, tamano, rgb);
    }

    /** Dibuja el logo (si se cargó) dentro del rectángulo indicado. */
    public void imagen(double x, double y, double w, double h) {
        if (imagenRgb == null) {
            return;
        }
        actual.append("q ").append(n(w)).append(" 0 0 ").append(n(h)).append(' ')
                .append(n(x)).append(' ').append(n(y)).append(" cm /Im1 Do Q\n");
    }

    public boolean tieneImagen() {
        return imagenRgb != null;
    }

    /** Carga un PNG/JPG para usarlo como logo; si no se puede leer, el reporte sale sin logo. */
    public void cargarImagen(byte[] datos) {
        imagenRgb = null;
        imagenAlfa = null;
        if (datos == null || datos.length == 0) {
            return;
        }
        try {
            BufferedImage origen = ImageIO.read(new ByteArrayInputStream(datos));
            if (origen == null) {
                return;
            }
            int max = 260;
            double escala = Math.min(1.0, (double) max / Math.max(origen.getWidth(), origen.getHeight()));
            int w = Math.max(1, (int) Math.round(origen.getWidth() * escala));
            int h = Math.max(1, (int) Math.round(origen.getHeight() * escala));
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(origen, 0, 0, w, h, null);
            g.dispose();
            byte[] rgb = new byte[w * h * 3];
            byte[] alfa = new byte[w * h];
            int i = 0;
            int a = 0;
            for (int yy = 0; yy < h; yy++) {
                for (int xx = 0; xx < w; xx++) {
                    int p = img.getRGB(xx, yy);
                    alfa[a++] = (byte) (p >>> 24);
                    rgb[i++] = (byte) (p >> 16);
                    rgb[i++] = (byte) (p >> 8);
                    rgb[i++] = (byte) p;
                }
            }
            imagenRgb = rgb;
            imagenAlfa = alfa;
            imagenAncho = w;
            imagenAlto = h;
        } catch (IOException | RuntimeException ex) {
            imagenRgb = null;     // sin logo, pero el reporte se genera igual
        }
    }

    // ------------------------------------------------------------------ medición de texto

    public static double ancho(String texto, boolean negrita, double tamano) {
        if (texto == null) {
            return 0;
        }
        int[] tabla = negrita ? W_NEGRITA : W_NORMAL;
        long total = 0;
        for (int i = 0; i < texto.length(); i++) {
            total += anchoCaracter(texto.charAt(i), tabla);
        }
        return total * tamano / 1000.0;
    }

    private static final Map<Character, Character> BASE = new ConcurrentHashMap<>();

    private static int anchoCaracter(char c, int[] tabla) {
        if (c >= 32 && c <= 126) {
            return tabla[c - 32];
        }
        switch (c) {
            case '¿': return 611;
            case '¡': return 333;
            case '—': return 1000;
            case '–': return 556;
            case '·': return 278;
            case '•': return 350;
            case 'º': return 365;
            case 'ª': return 370;
            case ' ': return 278;
            default: break;
        }
        Character base = BASE.get(c);
        if (base == null) {
            String d = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
            base = d.isEmpty() || d.charAt(0) < 32 || d.charAt(0) > 126 ? '?' : d.charAt(0);
            BASE.put(c, base);
        }
        return tabla[base - 32];
    }

    /** Parte un texto en líneas que caben en el ancho dado (por palabras; corta palabras muy largas). */
    public static List<String> envolver(String texto, double anchoMax, boolean negrita, double tamano) {
        List<String> lineas = new ArrayList<>();
        String t = texto == null ? "" : texto.replace("\r", "").replace("\t", " ");
        for (String parrafo : t.split("\n", -1)) {
            StringBuilder linea = new StringBuilder();
            for (String palabra : parrafo.split(" ")) {
                if (palabra.isEmpty()) {
                    continue;
                }
                String candidata = linea.length() == 0 ? palabra : linea + " " + palabra;
                if (ancho(candidata, negrita, tamano) <= anchoMax) {
                    linea.setLength(0);
                    linea.append(candidata);
                    continue;
                }
                if (linea.length() > 0) {
                    lineas.add(linea.toString());
                    linea.setLength(0);
                }
                String resto = palabra;
                while (ancho(resto, negrita, tamano) > anchoMax && resto.length() > 1) {
                    int corte = resto.length() - 1;
                    while (corte > 1 && ancho(resto.substring(0, corte), negrita, tamano) > anchoMax) {
                        corte--;
                    }
                    lineas.add(resto.substring(0, corte));
                    resto = resto.substring(corte);
                }
                linea.append(resto);
            }
            lineas.add(linea.toString());
        }
        return lineas;
    }

    /** Recorta un texto con "..." para que quepa en una sola línea. */
    public static String recortar(String texto, double anchoMax, boolean negrita, double tamano) {
        String t = texto == null ? "" : texto;
        if (ancho(t, negrita, tamano) <= anchoMax) {
            return t;
        }
        String puntos = "...";
        while (t.length() > 1 && ancho(t + puntos, negrita, tamano) > anchoMax) {
            t = t.substring(0, t.length() - 1);
        }
        return t + puntos;
    }

    // ------------------------------------------------------------------ salida

    public byte[] generar() throws IOException {
        int n = paginas.size();
        boolean conImagen = imagenRgb != null;
        int idImagen = 6;
        int idMascara = 7;
        int primeraPagina = conImagen ? 8 : 6;
        int totalObjetos = primeraPagina + n * 2 - 1;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int[] offsets = new int[totalObjetos + 1];
        out.write("%PDF-1.4\n%".getBytes(StandardCharsets.ISO_8859_1));
        out.write(new byte[]{(byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});

        objeto(out, offsets, 1, "<< /Type /Catalog /Pages 2 0 R /Lang (es-GT) >>");
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < n; i++) {
            kids.append(primeraPagina + i * 2).append(" 0 R ");
        }
        objeto(out, offsets, 2, "<< /Type /Pages /Kids [" + kids.toString().trim() + "] /Count " + n + " >>");
        objeto(out, offsets, 3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        objeto(out, offsets, 4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        objeto(out, offsets, 5, "<< /Title (" + literal(titulo) + ") /Author (" + literal(autor) + ") /Subject ("
                + literal(asunto) + ") /Creator (Sistema de Tickets - UMG) /Producer (Sistema de Tickets - UMG)"
                + (fechaCreacion.isEmpty() ? "" : " /CreationDate (" + fechaCreacion + ")") + " >>");

        if (conImagen) {
            flujo(out, offsets, idImagen, "/Type /XObject /Subtype /Image /Width " + imagenAncho + " /Height " + imagenAlto
                    + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /SMask " + idMascara + " 0 R", imagenRgb);
            flujo(out, offsets, idMascara, "/Type /XObject /Subtype /Image /Width " + imagenAncho + " /Height " + imagenAlto
                    + " /ColorSpace /DeviceGray /BitsPerComponent 8", imagenAlfa);
        }

        String recursos = "/Resources << /Font << /F1 3 0 R /F2 4 0 R >>"
                + (conImagen ? " /XObject << /Im1 " + idImagen + " 0 R >>" : "") + " >>";
        for (int i = 0; i < n; i++) {
            int idPagina = primeraPagina + i * 2;
            objeto(out, offsets, idPagina, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + n(ANCHO) + " " + n(ALTO)
                    + "] " + recursos + " /Contents " + (idPagina + 1) + " 0 R >>");
            flujo(out, offsets, idPagina + 1, "", paginas.get(i).toString().getBytes(StandardCharsets.ISO_8859_1));
        }

        int inicioXref = out.size();
        StringBuilder xref = new StringBuilder("xref\n0 " + (totalObjetos + 1) + "\n0000000000 65535 f \n");
        for (int id = 1; id <= totalObjetos; id++) {
            xref.append(String.format(Locale.ROOT, "%010d 00000 n \n", offsets[id]));
        }
        xref.append("trailer\n<< /Size ").append(totalObjetos + 1).append(" /Root 1 0 R /Info 5 0 R >>\nstartxref\n")
                .append(inicioXref).append("\n%%EOF\n");
        out.write(xref.toString().getBytes(StandardCharsets.ISO_8859_1));
        return out.toByteArray();
    }

    private static void objeto(ByteArrayOutputStream out, int[] offsets, int id, String cuerpo) throws IOException {
        offsets[id] = out.size();
        out.write((id + " 0 obj\n" + cuerpo + "\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
    }

    private static void flujo(ByteArrayOutputStream out, int[] offsets, int id, String diccionario, byte[] datos)
            throws IOException {
        byte[] comprimido = comprimir(datos);
        offsets[id] = out.size();
        out.write((id + " 0 obj\n<< " + diccionario + " /Filter /FlateDecode /Length " + comprimido.length
                + " >>\nstream\n").getBytes(StandardCharsets.ISO_8859_1));
        out.write(comprimido);
        out.write("\nendstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
    }

    private static byte[] comprimir(byte[] datos) {
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        d.setInput(datos);
        d.finish();
        ByteArrayOutputStream b = new ByteArrayOutputStream(Math.max(64, datos.length / 2));
        byte[] buf = new byte[8192];
        while (!d.finished()) {
            b.write(buf, 0, d.deflate(buf));
        }
        d.end();
        return b.toByteArray();
    }

    // ------------------------------------------------------------------ utilidades

    private static String n(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String color(int rgb) {
        return String.format(Locale.ROOT, "%.3f %.3f %.3f",
                ((rgb >> 16) & 0xFF) / 255.0, ((rgb >> 8) & 0xFF) / 255.0, (rgb & 0xFF) / 255.0);
    }

    /** Codifica en WinAnsi y escapa \ ( ) para un literal de cadena PDF; el resultado se maneja como Latin-1. */
    private static String literal(String s) {
        String limpio = s == null ? "" : s.replace("\r", " ").replace("\n", " ").replace("\t", " ");
        byte[] bytes = limpio.getBytes(WIN_ANSI);
        StringBuilder sb = new StringBuilder(bytes.length + 8);
        for (byte b : bytes) {
            char c = (char) (b & 0xFF);
            if (c == '\\' || c == '(' || c == ')') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
