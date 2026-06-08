package com.sutran.expedientes.dto;

import com.sutran.expedientes.entity.Expediente;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpedienteResponse(
        Integer idExpediente,
        String numeroExpediente,
        LocalDateTime fechaRegistro,
        String usuarioRegistro,
        String usuarioModifico,
        LocalDateTime fechaModificacion,
        String estado,
        String observacion,
        String nombreArchivo,
        boolean tieneArchivo,
        String areaActual,
        LocalDate fechaNotificacion,
        Integer diasPlazo,
        LocalDate fechaLimiteDerivacion,
        boolean derivado,
        boolean procesadoOcr,
        boolean aptoParaDerivar,
        String resumenIa,
        String numeroResolucion,
        String administrado,
        String rucAdministrado
) {
    public static ExpedienteResponse from(Expediente e) {
        boolean derivado = Boolean.TRUE.equals(e.getDerivado());
        boolean aptoParaDerivar = Boolean.TRUE.equals(e.getProcesadoOcr())
                && !derivado
                && e.getFechaLimiteDerivacion() != null
                && !e.getFechaLimiteDerivacion().isAfter(LocalDate.now());

        return new ExpedienteResponse(
                e.getIdExpediente(),
                e.getNumeroExpediente(),
                e.getFechaRegistro(),
                e.getUsuarioRegistro() != null
                        ? e.getUsuarioRegistro().getNombres() + " " + e.getUsuarioRegistro().getApellidos()
                        : null,
                e.getUsuarioModifico() != null
                        ? e.getUsuarioModifico().getNombres() + " " + e.getUsuarioModifico().getApellidos()
                        : null,
                e.getFechaModificacion(),
                e.getEstado(),
                e.getObservacion(),
                e.getNombreArchivo(),
                e.getRutaArchivo() != null && !e.getRutaArchivo().isBlank(),
                e.getAreaActual() != null ? e.getAreaActual().getNombreArea() : null,
                e.getFechaNotificacion(),
                e.getDiasPlazo(),
                e.getFechaLimiteDerivacion(),
                derivado,
                Boolean.TRUE.equals(e.getProcesadoOcr()),
                aptoParaDerivar,
                e.getResumenIa(),
                e.getNumeroResolucion(),
                e.getAdministrado(),
                e.getRucAdministrado()
        );
    }
}
