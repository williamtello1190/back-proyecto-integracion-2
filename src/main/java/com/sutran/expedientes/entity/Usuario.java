package com.sutran.expedientes.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "usuario", nullable = false, unique = true, length = 50)
    private String usuario;

    /** Hash BCrypt de la contraseña — nunca texto plano. */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "correo", length = 100)
    private String correo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_area")
    private Area area;

    @Column(name = "rol", nullable = false, length = 30)
    private String rol;

    @Column(name = "estado", nullable = false, length = 1)
    private String estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
