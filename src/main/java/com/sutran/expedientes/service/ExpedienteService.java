package com.sutran.expedientes.service;

import com.sutran.expedientes.dto.ExpedienteRequest;
import com.sutran.expedientes.dto.ExpedienteResponse;
import com.sutran.expedientes.dto.PageResponse;
import com.sutran.expedientes.entity.Area;
import com.sutran.expedientes.entity.Expediente;
import com.sutran.expedientes.entity.Usuario;
import com.sutran.expedientes.exception.ApiException;
import com.sutran.expedientes.repository.AreaRepository;
import com.sutran.expedientes.repository.ExpedienteRepository;
import com.sutran.expedientes.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExpedienteService {

    private static final String ESTADO_REGISTRADO = "REGISTRADO";

    private final ExpedienteRepository expedienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public PageResponse<ExpedienteResponse> listar(String estado, String numero, Pageable pageable) {
        var pagina = expedienteRepository.buscar(
                (estado == null || estado.isBlank()) ? null : estado,
                (numero == null || numero.isBlank()) ? null : numero,
                pageable
        );
        return PageResponse.from(pagina.map(ExpedienteResponse::from));
    }

    @Transactional
    public ExpedienteResponse registrar(ExpedienteRequest request, Authentication authentication) {
        if (expedienteRepository.existsByNumeroExpediente(request.numeroExpediente())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Ya existe un expediente registrado con el número " + request.numeroExpediente());
        }

        Usuario usuarioActual = obtenerUsuarioAutenticado(authentication);
        Area area = areaRepository.findById(request.idAreaActual())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "El área indicada no existe"));

        Expediente expediente = new Expediente();
        expediente.setNumeroExpediente(request.numeroExpediente());
        expediente.setObservacion(request.observacion());
        expediente.setFechaRegistro(LocalDateTime.now());
        expediente.setUsuarioRegistro(usuarioActual);
        expediente.setEstado(ESTADO_REGISTRADO);
        expediente.setAreaActual(area);
        expediente.setDiasPlazo(15);
        expediente.setDerivado(false);
        expediente.setProcesadoOcr(false);

        return ExpedienteResponse.from(expedienteRepository.save(expediente));
    }

    @Transactional
    public ExpedienteResponse adjuntarArchivo(Integer idExpediente, MultipartFile archivo, Authentication authentication) {
        Expediente expediente = obtenerExpediente(idExpediente);
        Usuario usuarioActual = obtenerUsuarioAutenticado(authentication);

        String rutaRelativa = fileStorageService.guardar(expediente.getNumeroExpediente(), archivo);

        expediente.setRutaArchivo(rutaRelativa);
        expediente.setNombreArchivo(archivo.getOriginalFilename());
        expediente.setUsuarioModifico(usuarioActual);
        expediente.setFechaModificacion(LocalDateTime.now());
        if (ESTADO_REGISTRADO.equals(expediente.getEstado())) {
            expediente.setEstado("EN_PROCESO");
        }

        return ExpedienteResponse.from(expedienteRepository.save(expediente));
    }

    @Transactional(readOnly = true)
    public Expediente obtenerExpediente(Integer idExpediente) {
        return expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expediente no encontrado"));
    }

    private Usuario obtenerUsuarioAutenticado(Authentication authentication) {
        String username = authentication.getName();
        return usuarioRepository.findByUsuarioAndEstado(username, "A")
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }
}
