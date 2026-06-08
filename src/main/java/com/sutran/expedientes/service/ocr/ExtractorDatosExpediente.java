package com.sutran.expedientes.service.ocr;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reconoce, mediante reglas (regex ancladas a las etiquetas que usan los documentos
 * de SUTRAN), los datos puntuales que el usuario necesita ver antes de derivar un
 * expediente: número de resolución, administrado sancionado, su RUC/DNI y la fecha
 * en que se notificó ESA resolución (no la de otros documentos del mismo expediente,
 * como la Carta de imputación de cargos — de ahí la lógica de desambiguación).
 *
 * Se prefiere dejar un campo en null antes que adivinar: una fecha equivocada podría
 * habilitar incorrectamente la derivación de un expediente (acción difícil de revertir).
 */
@Component
public class ExtractorDatosExpediente {

    private static final Pattern PATRON_NUMERO_RESOLUCION = Pattern.compile(
            "RESOLUCI[ÓO]N\\s+(?:DE\\s+[A-ZÁÉÍÓÚÑ]+\\s+)?N[°ºª.]?\\s*([0-9][0-9A-Z\\-./]{5,})",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATRON_ADMINISTRADO_CONSTANCIA = Pattern.compile(
            "Estimado\\(a\\)\\s*:\\s*([^\\r\\n]+)");

    private static final Pattern PATRON_RUC_CONSTANCIA = Pattern.compile(
            "Nro\\.\\s*Documento\\s*:\\s*(\\d{8,11})");

    private static final Pattern PATRON_ADMINISTRADO_RESOLUCION = Pattern.compile(
            "responsabilidad administrativa (?:de|contra)\\s+([A-ZÁÉÍÓÚÑ0-9&.,\\s]{4,80}?),\\s*(?:identificado|en su condici[óo]n)");

    private static final Pattern PATRON_RUC_GENERICO = Pattern.compile(
            "(?:RUC\\s*/\\s*DNI|DNI\\s*/\\s*RUC|RUC)\\s*N[°ºª.]?\\s*(\\d{8,11})",
            Pattern.CASE_INSENSITIVE);

    /** "10 de marzo del 2026" / "10 de marzo de 2026" */
    private static final Pattern PATRON_FECHA_TEXTUAL = Pattern.compile(
            "(\\d{1,2})\\s+de\\s+([a-záéíóúñ]+)\\s+de[l]?\\s+(\\d{4})",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATRON_NOTIFICACION = Pattern.compile(
            "notificad[ao]\\s+(?:con\\s+fecha|el)\\s+(\\d{1,2}\\s+de\\s+[a-záéíóúñ]+\\s+de[l]?\\s+\\d{4})",
            Pattern.CASE_INSENSITIVE);

    /** Encabezado que delimita cada bloque "Constancia de Notificación Electrónica" dentro del PDF. */
    private static final Pattern PATRON_ENCABEZADO_CONSTANCIA = Pattern.compile(
            "CONSTANCIA\\s+DE\\s+NOTIFICACI[ÓO]N\\s+ELECTR[ÓO]NICA", Pattern.CASE_INSENSITIVE);

    /** Indica a qué documento notifica el bloque: "...del 'NOTIFICACIÓN DE RESOLUCIÓN N° ...'" */
    private static final Pattern PATRON_TIPO_NOTIFICACION = Pattern.compile(
            "NOTIFICACI[ÓO]N\\s+DE\\s+(RESOLUCI[ÓO]N|CARTA|ACTA)", Pattern.CASE_INSENSITIVE);

    /** Campo estructurado que el usuario puede agregar en la Constancia: "Fecha Notifica : 22/05/2026". */
    private static final Pattern PATRON_FECHA_NOTIFICA_ESTRUCTURADA = Pattern.compile(
            "Fecha\\s+(?:de\\s+)?[Nn]otifica(?:ci[óo]n)?\\s*:?\\s*(\\d{1,2})[/.\\-](\\d{1,2})[/.\\-](\\d{4})",
            Pattern.CASE_INSENSITIVE);

    private static final int VENTANA_CONTEXTO = 280;

    private static final Map<String, Month> MESES = Map.ofEntries(
            Map.entry("enero", Month.JANUARY),
            Map.entry("febrero", Month.FEBRUARY),
            Map.entry("marzo", Month.MARCH),
            Map.entry("abril", Month.APRIL),
            Map.entry("mayo", Month.MAY),
            Map.entry("junio", Month.JUNE),
            Map.entry("julio", Month.JULY),
            Map.entry("agosto", Month.AUGUST),
            Map.entry("setiembre", Month.SEPTEMBER),
            Map.entry("septiembre", Month.SEPTEMBER),
            Map.entry("octubre", Month.OCTOBER),
            Map.entry("noviembre", Month.NOVEMBER),
            Map.entry("diciembre", Month.DECEMBER)
    );

    public DatosExtraidos extraer(String texto) {
        if (texto == null || texto.isBlank()) {
            return DatosExtraidos.vacio();
        }

        String numeroResolucion = buscar(PATRON_NUMERO_RESOLUCION, texto, 1);
        String administrado = extraerAdministrado(texto);
        String ruc = extraerRuc(texto);
        LocalDate fechaNotificacion = extraerFechaNotificacionResolucion(texto);

        return new DatosExtraidos(numeroResolucion, administrado, ruc, fechaNotificacion);
    }

    private String extraerAdministrado(String texto) {
        String porConstancia = buscar(PATRON_ADMINISTRADO_CONSTANCIA, texto, 1);
        if (porConstancia != null) {
            return limpiar(porConstancia);
        }
        String porResolucion = buscar(PATRON_ADMINISTRADO_RESOLUCION, texto, 1);
        return porResolucion != null ? limpiar(porResolucion) : null;
    }

    private String extraerRuc(String texto) {
        String porConstancia = buscar(PATRON_RUC_CONSTANCIA, texto, 1);
        if (porConstancia != null) {
            return porConstancia;
        }
        return buscar(PATRON_RUC_GENERICO, texto, 1);
    }

    /**
     * Determina la fecha de notificación de LA RESOLUCIÓN (no la de otros documentos
     * del expediente, como la Carta de imputación de cargos). Primero intenta leer un
     * campo estructurado (p. ej. "Fecha Notifica : 22/05/2026") dentro del bloque de la
     * Constancia de Notificación Electrónica que corresponde a la resolución — es la
     * fuente más confiable porque está anclada a una etiqueta y a un documento concreto.
     * Si no existe ese campo (PDFs sin ese agregado), recurre al reconocimiento por
     * frases en prosa con desambiguación de contexto, como respaldo.
     */
    private LocalDate extraerFechaNotificacionResolucion(String texto) {
        LocalDate porCampoEstructurado = extraerFechaDesdeConstanciaDeResolucion(texto);
        if (porCampoEstructurado != null) {
            return porCampoEstructurado;
        }
        return extraerFechaPorFraseEnProsa(texto);
    }

    /**
     * Recorre los bloques delimitados por "CONSTANCIA DE NOTIFICACIÓN ELECTRÓNICA",
     * identifica el que notifica la RESOLUCIÓN (por la frase
     * "...del 'NOTIFICACIÓN DE RESOLUCIÓN N° ...'") y, dentro de ese bloque, busca el
     * campo "Fecha Notifica : DD/MM/AAAA". Si hay más de un bloque de Resolución con
     * fechas distintas, se prefiere no decidir (null) antes que elegir al azar.
     */
    private LocalDate extraerFechaDesdeConstanciaDeResolucion(String texto) {
        List<Integer> inicios = new ArrayList<>();
        Matcher encabezados = PATRON_ENCABEZADO_CONSTANCIA.matcher(texto);
        while (encabezados.find()) {
            inicios.add(encabezados.start());
        }
        if (inicios.isEmpty()) {
            return null;
        }

        LocalDate candidataUnica = null;
        for (int i = 0; i < inicios.size(); i++) {
            int inicioBloque = inicios.get(i);
            int finBloque = (i + 1 < inicios.size()) ? inicios.get(i + 1) : texto.length();
            String bloque = texto.substring(inicioBloque, finBloque);

            Matcher tipo = PATRON_TIPO_NOTIFICACION.matcher(bloque);
            if (!tipo.find() || !tipo.group(1).toUpperCase(Locale.ROOT).startsWith("RESOLUCI")) {
                continue;
            }

            Matcher fecha = PATRON_FECHA_NOTIFICA_ESTRUCTURADA.matcher(bloque);
            if (!fecha.find()) {
                continue;
            }
            LocalDate fechaEncontrada = construirFecha(fecha.group(1), fecha.group(2), fecha.group(3));
            if (fechaEncontrada == null) {
                continue;
            }
            if (candidataUnica != null && !candidataUnica.equals(fechaEncontrada)) {
                return null;
            }
            candidataUnica = fechaEncontrada;
        }
        return candidataUnica;
    }

    private LocalDate construirFecha(String dia, String mes, String anio) {
        try {
            return LocalDate.of(Integer.parseInt(anio), Integer.parseInt(mes), Integer.parseInt(dia));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Respaldo: busca frases "notificad[ao] (con fecha|el) <fecha>" y, para cada una,
     * examina el contexto inmediatamente anterior para decidir a qué documento se
     * refiere (si menciona "RESOLUCIÓN" más cerca que "CARTA"/"ACTA"/"INFORME"). Solo
     * se acepta como fecha de notificación de la resolución cuando hay exactamente un
     * candidato inequívoco; de lo contrario null, para que el usuario la verifique.
     */
    private LocalDate extraerFechaPorFraseEnProsa(String texto) {
        Matcher coincidencias = PATRON_NOTIFICACION.matcher(texto);
        LocalDate candidataUnica = null;

        while (coincidencias.find()) {
            int inicioContexto = Math.max(0, coincidencias.start() - VENTANA_CONTEXTO);
            String contexto = texto.substring(inicioContexto, coincidencias.start()).toUpperCase(Locale.ROOT);

            if (refiereseAResolucion(contexto)) {
                LocalDate fecha = parsearFechaTextual(coincidencias.group(1));
                if (fecha == null) {
                    continue;
                }
                if (candidataUnica != null && !candidataUnica.equals(fecha)) {
                    return null; // ambigüedad real: dos fechas distintas asociadas a la resolución
                }
                candidataUnica = fecha;
            }
        }

        return candidataUnica;
    }

    /**
     * Decide a qué tipo de documento se refiere una frase "notificad[ao] ... <fecha>"
     * mirando cuál mención de tipo de documento (RESOLUCIÓN / CARTA / ACTA) aparece
     * MÁS CERCA, inmediatamente antes, de esa frase — es la que rige la cláusula.
     * Buscar solo si el número de resolución aparece en la ventana NO basta: ese
     * número suele repetirse en encabezados lejanos que no son los que se notifican aquí.
     */
    private boolean refiereseAResolucion(String contextoEnMayusculas) {
        int posResolucion = ultimaPosicion(contextoEnMayusculas, "RESOLUCI");
        if (posResolucion < 0) {
            return false;
        }
        int posCarta = ultimaPosicion(contextoEnMayusculas, "CARTA");
        int posActa = ultimaPosicion(contextoEnMayusculas, "ACTA DE CONTROL");
        int posInforme = ultimaPosicion(contextoEnMayusculas, "INFORME FINAL");

        return posResolucion > posCarta && posResolucion > posActa && posResolucion > posInforme;
    }

    private int ultimaPosicion(String texto, String buscado) {
        return texto.lastIndexOf(buscado);
    }

    private LocalDate parsearFechaTextual(String fechaTextual) {
        Matcher m = PATRON_FECHA_TEXTUAL.matcher(fechaTextual);
        if (!m.find()) {
            return null;
        }
        Month mes = MESES.get(m.group(2).toLowerCase(Locale.ROOT));
        if (mes == null) {
            return null;
        }
        try {
            return LocalDate.of(Integer.parseInt(m.group(3)), mes, Integer.parseInt(m.group(1)));
        } catch (Exception e) {
            return null;
        }
    }

    private String buscar(Pattern patron, String texto, int grupo) {
        Matcher m = patron.matcher(texto);
        return m.find() ? m.group(grupo) : null;
    }

    private String limpiar(String valor) {
        String resultado = valor.strip().replaceAll("\\s+", " ");
        return resultado.isBlank() ? null : resultado;
    }
}
