package com.sutran.expedientes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExpedienteRequest(
        @NotBlank @Size(max = 30) String numeroExpediente,
        @Size(max = 500) String observacion,
        @NotNull Integer idAreaActual
) {
}
