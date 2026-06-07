package com.sutran.expedientes.controller;

import com.sutran.expedientes.dto.ConstanciaResponse;
import com.sutran.expedientes.dto.PageResponse;
import com.sutran.expedientes.service.ConstanciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/constancias")
@RequiredArgsConstructor
public class ConstanciaController {

    private final ConstanciaService constanciaService;

    /** Bandeja coactiva: listado paginado de constancias (expedientes derivados a Cobranza Coactiva). */
    @GetMapping
    public PageResponse<ConstanciaResponse> listar(
            @RequestParam(required = false) String numeroConstancia,
            @RequestParam(required = false) String numeroExpediente,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {

        Pageable pageable = PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "fechaConstancia"));
        return constanciaService.listar(numeroConstancia, numeroExpediente, pageable);
    }
}
