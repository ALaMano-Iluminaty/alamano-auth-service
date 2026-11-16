package com.alamano.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataUsuarioRepository extends JpaRepository<UsuarioJpaEntity, Long> {

    boolean existsByCorreo(String correo);

    Optional<UsuarioJpaEntity> findByCorreo(String correo);

    Optional<UsuarioJpaEntity> findByGoogleSub(String googleSub);

    List<UsuarioJpaEntity> findAllByOrderByIdAsc();
}
