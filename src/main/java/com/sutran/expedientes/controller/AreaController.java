package com.sutran.expedientes.controller;

import com.sutran.expedientes.dto.AreaResponse;
import com.sutran.expedientes.repository.AreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/areas")
@RequiredArgsConstructor
public class AreaController {

    private final AreaRepository areaRepository;

    /** Lista de áreas activas, usada para poblar el combo del formulario de registro. */
    @GetMapping
    public List<AreaResponse> listar() {
        return areaRepository.findByEstadoOrderByNombreArea("A").stream()
                .map(AreaResponse::from)
                .toList();
    }
}
