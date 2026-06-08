package com.sutran.expedientes.service.ocr;

import com.sutran.expedientes.entity.Expediente;
import com.sutran.expedientes.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Orquesta el procesamiento "asistido" del PDF adjunto a un expediente:
 *  1) extrae el texto del PDF (capa de texto, sin OCR — los documentos de SUTRAN son digitales),
 *  2) reconoce datos puntuales por reglas (número de resolución, administrado, RUC, fecha de notificación),
 *  3) redacta un resumen en lenguaje natural (IA) o, si no está disponible, uno de respaldo con esos datos,
 *  4) guarda todo en el expediente para que el usuario decida si deriva o no — nunca deriva por sí solo.
 *
 * Un fallo aquí no debe impedir que el archivo quede adjuntado: se registra y el
 * expediente queda con procesadoOcr=false ("pendiente"), visible para el usuario.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcesamientoOcrIaService {

    private static final DateTimeFormatter FORMATO_FECHA_LEGIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FileStorageService fileStorageService;
    private final PdfTextExtractor pdfTextExtractor;
    private final ExtractorDatosExpediente extractorDatos;
    private final ResumenIaClient resumenIaClient;

    public void procesar(Expediente expediente) {
        try {
            String texto = pdfTextExtractor.extraerTexto(fileStorageService.resolverRutaAbsoluta(expediente.getRutaArchivo()));
            DatosExtraidos datos = extractorDatos.extraer(texto);

            aplicarSiAusente(expediente::getNumeroResolucion, expediente::setNumeroResolucion, datos.numeroResolucion());
            aplicarSiAusente(expediente::getAdministrado, expediente::setAdministrado, datos.administrado());
            aplicarSiAusente(expediente::getRucAdministrado, expediente::setRucAdministrado, datos.rucAdministrado());
            if (expediente.getFechaNotificacion() == null && datos.fechaNotificacionResolucion() != null) {
                expediente.setFechaNotificacion(datos.fechaNotificacionResolucion());
            }

            String resumen = resumenIaClient.generarResumen(construirPrompt(expediente, datos, texto));
            expediente.setResumenIa(resumen != null ? resumen : resumenDeRespaldo(expediente, datos));
            expediente.setProcesadoOcr(true);
        } catch (Exception e) {
            log.warn("No se pudo procesar OCR/IA del expediente {}: {}", expediente.getNumeroExpediente(), e.getMessage());
        }
    }

    private void aplicarSiAusente(java.util.function.Supplier<String> obtener, java.util.function.Consumer<String> asignar, String nuevoValor) {
        if ((obtener.get() == null || obtener.get().isBlank()) && nuevoValor != null && !nuevoValor.isBlank()) {
            asignar.accept(nuevoValor);
        }
    }

    private String construirPrompt(Expediente expediente, DatosExtraidos datos, String textoCompleto) {
        String textoRecortado = textoCompleto.length() > 6000 ? textoCompleto.substring(0, 6000) : textoCompleto;

        return """
                Eres un asistente de SUTRAN. Redacta un resumen breve (máximo 5 líneas, en español,
                en tono administrativo y neutral) del siguiente expediente sancionador, dirigido a un
                funcionario que debe decidir si lo deriva a Cobranza Coactiva. Menciona: el administrado
                sancionado, el número de resolución, la infracción y la sanción/multa impuesta, y si el
                texto lo indica, la fecha de notificación de la resolución. No inventes datos que no
                aparezcan en el texto; si algo no aparece, simplemente no lo menciones.

                Datos ya identificados automáticamente (pueden estar incompletos):
                - N° de expediente: %s
                - N° de resolución: %s
                - Administrado: %s
                - RUC/DNI: %s

                Texto del documento (puede estar truncado):
                ---
                %s
                ---
                """.formatted(
                expediente.getNumeroExpediente(),
                valorOTexto(datos.numeroResolucion()),
                valorOTexto(datos.administrado()),
                valorOTexto(datos.rucAdministrado()),
                textoRecortado
        );
    }

    /** Resumen simple armado con los datos extraídos, usado cuando la IA está deshabilitada o falla. */
    private String resumenDeRespaldo(Expediente expediente, DatosExtraidos datos) {
        StringBuilder resumen = new StringBuilder("Datos detectados automáticamente en el documento adjunto: ");
        resumen.append("expediente ").append(expediente.getNumeroExpediente()).append(". ");

        if (datos.numeroResolucion() != null) {
            resumen.append("Resolución N° ").append(datos.numeroResolucion()).append(". ");
        }
        if (datos.administrado() != null) {
            resumen.append("Administrado: ").append(datos.administrado());
            if (datos.rucAdministrado() != null) {
                resumen.append(" (RUC ").append(datos.rucAdministrado()).append(")");
            }
            resumen.append(". ");
        }
        if (datos.fechaNotificacionResolucion() != null) {
            resumen.append("Notificada el ")
                    .append(datos.fechaNotificacionResolucion().format(FORMATO_FECHA_LEGIBLE))
                    .append(". ");
        } else {
            resumen.append("No se identificó con certeza la fecha de notificación de la resolución; ")
                    .append("verifíquela manualmente antes de derivar. ");
        }
        return resumen.toString().strip();
    }

    private String valorOTexto(String valor) {
        return valor != null ? valor : "no identificado";
    }
}
