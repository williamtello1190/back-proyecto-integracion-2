package com.sutran.expedientes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DerivarCoactivoRequest(
        @NotBlank @Size(max = 30) String numeroConstancia,
        @Size(max = 500) String observacion
) {
}
