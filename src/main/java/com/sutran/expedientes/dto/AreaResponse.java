package com.sutran.expedientes.dto;

import com.sutran.expedientes.entity.Area;

public record AreaResponse(
        Integer idArea,
        String nombreArea
) {
    public static AreaResponse from(Area area) {
        return new AreaResponse(area.getIdArea(), area.getNombreArea());
    }
}
