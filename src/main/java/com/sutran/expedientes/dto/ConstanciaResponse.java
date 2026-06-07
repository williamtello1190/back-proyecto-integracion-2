package com.sutran.expedientes.dto;

import com.sutran.expedientes.entity.Constancia;
import com.sutran.expedientes.entity.Expediente;

import java.time.LocalDateTime;

public record ConstanciaResponse(
        Integer idConstancia,
        String numeroConstancia,
        LocalDateTime fechaConstancia,
        Integer idExpediente,
        String numeroExpediente,
        String observacionExpediente,
        String estadoExpediente,
        boolean tieneArchivo,
        String nombreArchivo,
        String usuarioRegistro,
        String usuarioModifico,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaModificacion,
        String observacion,
        String estado
) {
    public static ConstanciaResponse from(Constancia c) {
        Expediente e = c.getExpediente();
        return new ConstanciaResponse(
                c.getIdConstancia(),
                c.getNumeroConstancia(),
                c.getFechaConstancia(),
                e.getIdExpediente(),
                e.getNumeroExpediente(),
                e.getObservacion(),
                e.getEstado(),
                e.getRutaArchivo() != null && !e.getRutaArchivo().isBlank(),
                e.getNombreArchivo(),
                c.getUsuarioRegistro() != null
                        ? c.getUsuarioRegistro().getNombres() + " " + c.getUsuarioRegistro().getApellidos()
                        : null,
                c.getUsuarioModifico() != null
                        ? c.getUsuarioModifico().getNombres() + " " + c.getUsuarioModifico().getApellidos()
                        : null,
                c.getFechaRegistro(),
                c.getFechaModificacion(),
                c.getObservacion(),
                c.getEstado()
        );
    }
}
