package com.sutran.expedientes.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.storage")
@Getter
@Setter
public class StorageProperties {
    /** Carpeta local raíz donde se guardan los archivos adjuntos de los expedientes. */
    private String basePath;
    private int maxFileSizeMb;
}
