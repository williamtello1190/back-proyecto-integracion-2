package com.sutran.expedientes.controller;

import com.sutran.expedientes.dto.ConstanciaResponse;
import com.sutran.expedientes.dto.DerivarCoactivoRequest;
import com.sutran.expedientes.dto.ExpedienteRequest;
import com.sutran.expedientes.dto.ExpedienteResponse;
import com.sutran.expedientes.dto.PageResponse;
import com.sutran.expedientes.entity.Expediente;
import com.sutran.expedientes.exception.ApiException;
import com.sutran.expedientes.service.ConstanciaService;
import com.sutran.expedientes.service.ExpedienteService;
import com.sutran.expedientes.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/expedientes")
@RequiredArgsConstructor
public class ExpedienteController {

    private final ExpedienteService expedienteService;
    private final FileStorageService fileStorageService;
    private final ConstanciaService constanciaService;

    /** Bandeja de expedientes registrados (listado paginado con filtros). */
    @GetMapping
    public PageResponse<ExpedienteResponse> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String numero,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {

        Pageable pageable = PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "fechaRegistro"));
        return expedienteService.listar(estado, numero, pageable);
    }

    /** Registro de un nuevo expediente. */
    @PostMapping
    public ResponseEntity<ExpedienteResponse> registrar(@Valid @RequestBody ExpedienteRequest request,
                                                         Authentication authentication) {
        ExpedienteResponse creado = expedienteService.registrar(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /** Adjuntar archivo PDF al expediente: se guarda en disco y se registra la ruta en la BD. */
    @PostMapping(path = "/{id}/archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExpedienteResponse adjuntarArchivo(@PathVariable("id") Integer idExpediente,
                                               @RequestParam("archivo") MultipartFile archivo,
                                               Authentication authentication) {
        return expedienteService.adjuntarArchivo(idExpediente, archivo, authentication);
    }

    /** Deriva (transfiere) el expediente al área de Cobranza Coactiva, generando su constancia. */
    @PostMapping("/{id}/derivar")
    public ConstanciaResponse derivar(@PathVariable("id") Integer idExpediente,
                                       @Valid @RequestBody DerivarCoactivoRequest request,
                                       Authentication authentication) {
        return constanciaService.derivarACoactivo(idExpediente, request, authentication);
    }

    /** Ver / descargar el archivo adjunto de un expediente. */
    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> descargarArchivo(@PathVariable("id") Integer idExpediente) {
        Expediente expediente = expedienteService.obtenerExpediente(idExpediente);

        if (expediente.getRutaArchivo() == null || expediente.getRutaArchivo().isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "El expediente no tiene un archivo adjunto");
        }

        Path rutaAbsoluta = fileStorageService.resolverRutaAbsoluta(expediente.getRutaArchivo());
        Resource recurso;
        try {
            recurso = new UrlResource(rutaAbsoluta.toUri());
        } catch (MalformedURLException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer el archivo");
        }

        if (!recurso.exists() || !recurso.isReadable()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "El archivo ya no existe en el almacenamiento local");
        }

        String nombreDescarga = expediente.getNombreArchivo() != null ? expediente.getNombreArchivo() : "documento.pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombreDescarga + "\"")
                .body(recurso);
    }
}
