package com.sutran.expedientes.repository;

import com.sutran.expedientes.entity.Constancia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConstanciaRepository extends JpaRepository<Constancia, Integer> {

    boolean existsByNumeroConstancia(String numeroConstancia);

    boolean existsByExpediente_IdExpediente(Integer idExpediente);

    @Query("""
            SELECT c FROM Constancia c
            WHERE (:numeroConstancia IS NULL OR LOWER(c.numeroConstancia) LIKE LOWER(CONCAT('%', :numeroConstancia, '%')))
              AND (:numeroExpediente IS NULL OR LOWER(c.expediente.numeroExpediente) LIKE LOWER(CONCAT('%', :numeroExpediente, '%')))
            """)
    Page<Constancia> buscar(@Param("numeroConstancia") String numeroConstancia,
                            @Param("numeroExpediente") String numeroExpediente,
                            Pageable pageable);
}
