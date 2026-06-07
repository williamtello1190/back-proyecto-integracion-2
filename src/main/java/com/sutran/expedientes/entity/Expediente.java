package com.sutran.expedientes.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Expediente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_expediente")
    private Integer idExpediente;

    @Column(name = "numero_expediente", nullable = false, unique = true, length = 30)
    private String numeroExpediente;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    private Usuario usuarioRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_modifico")
    private Usuario usuarioModifico;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Column(name = "ruta_archivo", length = 400)
    private String rutaArchivo;

    @Column(name = "nombre_archivo", length = 200)
    private String nombreArchivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_area_actual", nullable = false)
    private Area areaActual;

    @Column(name = "fecha_notificacion")
    private LocalDate fechaNotificacion;

    @Column(name = "dias_plazo", nullable = false)
    private Integer diasPlazo;

    @Column(name = "derivado", nullable = false)
    private Boolean derivado;

    @Column(name = "procesado_ocr", nullable = false)
    private Boolean procesadoOcr;

    /** Columna calculada en BD (PERSISTED) — solo lectura desde la app. */
    @Column(name = "fecha_limite_derivacion", insertable = false, updatable = false)
    private LocalDate fechaLimiteDerivacion;
}
