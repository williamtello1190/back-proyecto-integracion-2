package com.sutran.expedientes.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Constancia de transferencia de un expediente al área de Cobranza Coactiva. */
@Entity
@Table(name = "Constancia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Constancia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_constancia")
    private Integer idConstancia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_expediente", nullable = false)
    private Expediente expediente;

    @Column(name = "numero_constancia", nullable = false, unique = true, length = 30)
    private String numeroConstancia;

    @Column(name = "fecha_constancia", nullable = false)
    private LocalDateTime fechaConstancia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    private Usuario usuarioRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_modifico")
    private Usuario usuarioModifico;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;
}
