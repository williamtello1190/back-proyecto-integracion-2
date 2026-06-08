package com.sutran.expedientes.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ia")
@Getter
@Setter
public class IaProperties {
    /** Si es false, no se llama a la API externa (se usa un resumen de respaldo armado con los datos extraídos). */
    private boolean habilitado;
    private String apiKey;
    private String modelo = "claude-haiku-4-5";
    private int maxTokens = 400;
}
