package com.sutran.expedientes.repository;

import com.sutran.expedientes.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByUsuarioAndEstado(String usuario, String estado);
}
