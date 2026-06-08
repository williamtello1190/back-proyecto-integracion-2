package com.sutran.expedientes.service.ocr;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ExtractorDatosExpedienteTest {

    private static final Path PDF_MUESTRA = Path.of("C:/TrabajoFinalP2/adjunto_expediente.pdf");
    private static final Path PDF_MUESTRA_V2 = Path.of("C:/TrabajoFinalP2/adjunto_expediente_V2.pdf");

    private final PdfTextExtractor pdfTextExtractor = new PdfTextExtractor();
    private final ExtractorDatosExpediente extractor = new ExtractorDatosExpediente();

    @Test
    void reconoceLosDatosEstructuradosDelExpedienteDeMuestra() throws Exception {
        assumeTrue(new File(PDF_MUESTRA.toUri()).exists(), "PDF de muestra no disponible en este entorno");

        String texto = pdfTextExtractor.extraerTexto(PDF_MUESTRA);
        DatosExtraidos datos = extractor.extraer(texto);

        assertThat(datos.numeroResolucion()).isEqualTo("4026015049-S-2026-SUTRAN/06.4.1");
        assertThat(datos.administrado()).isEqualTo("SERVICIOS LOGISTICOS A & L E.I.R.L.");
        assertThat(datos.rucAdministrado()).isEqualTo("20613756753");

        // El PDF de muestra NO contiene una fecha de notificación inequívocamente asociada
        // a la Resolución (solo a la Carta) — debe quedar en null en lugar de adivinar.
        assertThat(datos.fechaNotificacionResolucion()).isNull();
    }

    @Test
    void reconoceLaFechaDeNotificacionEstructuradaAgregadaEnLaConstanciaDeLaResolucion() throws Exception {
        assumeTrue(new File(PDF_MUESTRA_V2.toUri()).exists(), "PDF de muestra V2 no disponible en este entorno");

        String texto = pdfTextExtractor.extraerTexto(PDF_MUESTRA_V2);
        DatosExtraidos datos = extractor.extraer(texto);

        // El PDF V2 agrega "Fecha Notifica : 22/05/2026" dentro del bloque de la
        // Constancia que notifica la RESOLUCIÓN — debe leerse de forma confiable,
        // sin necesidad de adivinar a partir de frases en prosa.
        assertThat(datos.fechaNotificacionResolucion()).isEqualTo(LocalDate.of(2026, 5, 22));
    }

    @Test
    void reconoceLaFechaDeNotificacionSoloCuandoEstaClaramenteAsociadaALaResolucion() {
        String texto = """
                CONSTANCIA DE NOTIFICACION ELECTRONICA 11662973
                Estimado(a)  : EMPRESA DE PRUEBA S.A.C.
                Nro. Documento : 20100070970
                La presente constancia acredita el depósito de la notificación del "NOTIFICACIÓN DE
                RESOLUCIÓN N° 1234567890-S-2026-SUTRAN/06.4.1"

                RESOLUCIÓN DE SUBGERENCIA N° 1234567890-S-2026-SUTRAN/06.4.1
                Lima, 01 de abril del 2026
                Que, mediante la presente RESOLUCIÓN N° 1234567890-S-2026-SUTRAN/06.4.1,
                notificada con fecha 03 de abril del 2026, se sanciona al administrado...
                """;

        DatosExtraidos datos = extractor.extraer(texto);

        assertThat(datos.numeroResolucion()).isEqualTo("1234567890-S-2026-SUTRAN/06.4.1");
        assertThat(datos.administrado()).isEqualTo("EMPRESA DE PRUEBA S.A.C.");
        assertThat(datos.rucAdministrado()).isEqualTo("20100070970");
        assertThat(datos.fechaNotificacionResolucion()).isEqualTo(LocalDate.of(2026, 4, 3));
    }

    @Test
    void noConfundeLaFechaDeNotificacionDeLaCartaConLaDeLaResolucion() {
        String texto = """
                RESOLUCIÓN DE SUBGERENCIA N° 1234567890-S-2026-SUTRAN/06.4.1
                Que, a través de la Carta N° 0001-IC-2026-SUTRAN de fecha 06 de marzo del 2026,
                con la imputación de cargos, notificada con fecha 10 de marzo del 2026, se inició
                el procedimiento contra la empresa...
                """;

        DatosExtraidos datos = extractor.extraer(texto);

        assertThat(datos.fechaNotificacionResolucion()).isNull();
    }
}
