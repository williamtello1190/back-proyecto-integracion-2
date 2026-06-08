package com.sutran.expedientes.service.ocr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sutran.expedientes.config.IaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Cliente para la API de Anthropic (Claude) usada únicamente para redactar, en
 * lenguaje natural, el resumen del expediente que se muestra al usuario
 * (campo resumenIa). No participa en ninguna decisión: la elegibilidad para
 * derivar se calcula con datos estructurados (fechas), nunca con la respuesta del LLM.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResumenIaClient {

    private static final String URL_MENSAJES = "https://api.anthropic.com/v1/messages";
    private static final String VERSION_API = "2023-06-01";

    private final IaProperties iaProperties;
    private final RestClient.Builder restClientBuilder;

    /**
     * Devuelve el resumen generado por el modelo, o null si la IA está deshabilitada
     * o la llamada falla (el llamador decide qué resumen de respaldo mostrar).
     */
    public String generarResumen(String prompt) {
        if (!iaProperties.isHabilitado() || iaProperties.getApiKey() == null || iaProperties.getApiKey().isBlank()) {
            return null;
        }

        try {
            RestClient cliente = restClientBuilder.baseUrl(URL_MENSAJES).build();

            SolicitudMensaje solicitud = new SolicitudMensaje(
                    iaProperties.getModelo(),
                    iaProperties.getMaxTokens(),
                    List.of(new Mensaje("user", prompt))
            );

            RespuestaMensaje respuesta = cliente.post()
                    .header("x-api-key", iaProperties.getApiKey())
                    .header("anthropic-version", VERSION_API)
                    .body(solicitud)
                    .retrieve()
                    .body(RespuestaMensaje.class);

            if (respuesta == null || respuesta.content() == null || respuesta.content().isEmpty()) {
                return null;
            }
            return respuesta.content().stream()
                    .map(BloqueContenido::text)
                    .filter(texto -> texto != null && !texto.isBlank())
                    .findFirst()
                    .map(String::strip)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("No se pudo generar el resumen por IA: {}", e.getMessage());
            return null;
        }
    }

    private record SolicitudMensaje(String model, int max_tokens, List<Mensaje> messages) {
    }

    private record Mensaje(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaMensaje(List<BloqueContenido> content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BloqueContenido(String type, String text) {
    }
}
