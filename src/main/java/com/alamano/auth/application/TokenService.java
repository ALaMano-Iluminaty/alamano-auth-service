package com.alamano.auth.application;

import com.alamano.auth.domain.Usuario;
import java.time.Duration;

/**
 * Puerto para la generacion de tokens de sesion. La implementacion real
 * (JWT firmado con RS256) vive en infrastructure/security/JwtTokenService.
 */
public interface TokenService {
    String generarToken(Usuario usuario);

    default String generarToken(Usuario usuario, Duration vida) {
        return generarToken(usuario);
    }
}
