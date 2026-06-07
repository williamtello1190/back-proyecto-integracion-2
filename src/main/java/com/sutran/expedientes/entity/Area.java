package com.sutran.expedientes.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Area", schema = "dbo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_area")
    private Integer idArea;

    @Column(name = "nombre_area", nullable = false, length = 100)
    private String nombreArea;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "estado", nullable = false, length = 1)
    private String estado;
}
