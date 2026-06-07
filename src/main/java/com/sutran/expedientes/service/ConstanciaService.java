package com.sutran.expedientes.service;

import com.sutran.expedientes.dto.ConstanciaResponse;
import com.sutran.expedientes.dto.DerivarCoactivoRequest;
import com.sutran.expedientes.dto.PageResponse;
import com.sutran.expedientes.entity.Area;
import com.sutran.expedientes.entity.Constancia;
import com.sutran.expedientes.entity.Expediente;
import com.sutran.expedientes.entity.Usuario;
import com.sutran.expedientes.exception.ApiException;
import com.sutran.expedientes.repository.AreaRepository;
import com.sutran.expedientes.repository.ConstanciaRepository;
import com.sutran.expedientes.repository.ExpedienteRepository;
import com.sutran.expedientes.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConstanciaService {

    private static final String FRAGMENTO_AREA_COACTIVA = "Coactiv";
    private static final String ESTADO_CONSTANCIA_REGISTRADA = "REGISTRADA";
    private static final String ESTADO_EXPEDIENTE_DERIVADO = "DERIVADO";

    private final ConstanciaRepository constanciaRepository;
    private final ExpedienteRepository expedienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;

    @Transactional(readOnly = true)
    public PageResponse<ConstanciaResponse> listar(String numeroConstancia, String numeroExpediente, Pageable pageable) {
        var pagina = constanciaRepository.buscar(
                (numeroConstancia == null || numeroConstancia.isBlank()) ? null : numeroConstancia,
                (numeroExpediente == null || numeroExpediente.isBlank()) ? null : numeroExpediente,
                pageable
        );
        return PageResponse.from(pagina.map(ConstanciaResponse::from));
    }

    @Transactional
    public ConstanciaResponse derivarACoactivo(Integer idExpediente, DerivarCoactivoRequest request, Authentication authentication) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expediente no encontrado"));

        if (Boolean.TRUE.equals(expediente.getDerivado())) {
            throw new ApiException(HttpStatus.CONFLICT, "El expediente ya fue derivado a Cobranza Coactiva");
        }

        if (constanciaRepository.existsByNumeroConstancia(request.numeroConstancia())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Ya existe una constancia registrada con el número " + request.numeroConstancia());
        }

        Usuario usuarioActual = obtenerUsuarioAutenticado(authentication);
        Area areaCoactiva = areaRepository.findFirstByNombreAreaContainingIgnoreCaseAndEstado(FRAGMENTO_AREA_COACTIVA, "A")
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "No se encontró configurada el área de Cobranza Coactiva"));

        LocalDateTime ahora = LocalDateTime.now();

        Constancia constancia = new Constancia();
        constancia.setExpediente(expediente);
        constancia.setNumeroConstancia(request.numeroConstancia());
        constancia.setFechaConstancia(ahora);
        constancia.setUsuarioRegistro(usuarioActual);
        constancia.setFechaRegistro(ahora);
        constancia.setObservacion(request.observacion());
        constancia.setEstado(ESTADO_CONSTANCIA_REGISTRADA);
        constanciaRepository.save(constancia);

        expediente.setDerivado(true);
        expediente.setEstado(ESTADO_EXPEDIENTE_DERIVADO);
        expediente.setAreaActual(areaCoactiva);
        expediente.setUsuarioModifico(usuarioActual);
        expediente.setFechaModificacion(ahora);
        expedienteRepository.save(expediente);

        return ConstanciaResponse.from(constancia);
    }

    private Usuario obtenerUsuarioAutenticado(Authentication authentication) {
        return usuarioRepository.findByUsuarioAndEstado(authentication.getName(), "A")
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }
}
