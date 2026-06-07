package com.sutran.expedientes.service;

import com.sutran.expedientes.config.StorageProperties;
import com.sutran.expedientes.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final String CONTENT_TYPE_PDF = "application/pdf";

    private final StorageProperties storageProperties;

    /**
     * Guarda el archivo dentro de una subcarpeta nombrada con el número de expediente
     * y devuelve la ruta relativa que se persiste en la BD (campo ruta_archivo).
     */
    public String guardar(String numeroExpediente, MultipartFile archivo) {
        validar(archivo);

        try {
            Path carpetaExpediente = Path.of(storageProperties.getBasePath(), numeroExpediente);
            Files.createDirectories(carpetaExpediente);

            String nombreUnico = UUID.randomUUID() + "_" + sanitizar(archivo.getOriginalFilename());
            Path destino = carpetaExpediente.resolve(nombreUnico);
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            // ruta relativa al base-path, para no acoplar la BD a una ubicación absoluta
            return numeroExpediente + "/" + nombreUnico;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el archivo: " + e.getMessage());
        }
    }

    public Path resolverRutaAbsoluta(String rutaRelativa) {
        Path ruta = Path.of(storageProperties.getBasePath()).resolve(rutaRelativa).normalize();
        if (!ruta.startsWith(Path.of(storageProperties.getBasePath()).normalize())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ruta de archivo inválida");
        }
        return ruta;
    }

    private void validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debe seleccionar un archivo");
        }
        if (!CONTENT_TYPE_PDF.equalsIgnoreCase(archivo.getContentType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Solo se permiten archivos PDF");
        }
        long maxBytes = (long) storageProperties.getMaxFileSizeMb() * 1024 * 1024;
        if (archivo.getSize() > maxBytes) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "El archivo supera el tamaño máximo permitido (" + storageProperties.getMaxFileSizeMb() + " MB)");
        }
    }

    private String sanitizar(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            return "archivo.pdf";
        }
        return nombreOriginal.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
