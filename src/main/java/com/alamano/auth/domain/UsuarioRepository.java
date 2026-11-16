package com.alamano.auth.domain;

import java.util.List;
import java.util.Optional;

/**
 * Puerto (interfaz) que el dominio necesita para persistir usuarios.
 * La implementacion real (adaptador contra Postgres) vive en infrastructure/persistence.
 */
public interface UsuarioRepository {

    boolean existsByCorreo(String correo);

    Usuario save(Usuario usuario);

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByGoogleSub(String googleSub);

    Optional<Usuario> findById(Long id);

    /** Todas las cuentas, de la mas antigua a la mas nueva. */
    List<Usuario> findAll();
}
