package com.sutran.expedientes.dto;

public record LoginResponse(
        String token,
        String usuario,
        String nombres,
        String apellidos,
        String rol
) {
}
