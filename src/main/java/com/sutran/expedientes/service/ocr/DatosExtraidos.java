package com.sutran.expedientes.service.ocr;

import java.time.LocalDate;

/**
 * Datos puntuales reconocidos en el texto del PDF adjunto mediante reglas
 * (expresiones regulares ancladas a las etiquetas propias de los documentos de SUTRAN).
 * Cualquier campo puede llegar en null cuando no se encontró con suficiente certeza:
 * en ese caso se prefiere dejarlo vacío (para que el usuario lo complete) antes que adivinar.
 */
public record DatosExtraidos(
        String numeroResolucion,
        String administrado,
        String rucAdministrado,
        LocalDate fechaNotificacionResolucion
) {
    public static DatosExtraidos vacio() {
        return new DatosExtraidos(null, null, null, null);
    }
}
