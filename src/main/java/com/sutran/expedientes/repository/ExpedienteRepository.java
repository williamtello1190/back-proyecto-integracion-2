package com.sutran.expedientes.repository;

import com.sutran.expedientes.entity.Expediente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpedienteRepository extends JpaRepository<Expediente, Integer> {

    boolean existsByNumeroExpediente(String numeroExpediente);

    @Query("""
            SELECT e FROM Expediente e
            WHERE (:estado IS NULL OR e.estado = :estado)
              AND (:numero IS NULL OR LOWER(e.numeroExpediente) LIKE LOWER(CONCAT('%', :numero, '%')))
            """)
    Page<Expediente> buscar(@Param("estado") String estado,
                            @Param("numero") String numero,
                            Pageable pageable);
}
