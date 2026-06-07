package com.sutran.expedientes.repository;

import com.sutran.expedientes.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AreaRepository extends JpaRepository<Area, Integer> {

    List<Area> findByEstadoOrderByNombreArea(String estado);
}
