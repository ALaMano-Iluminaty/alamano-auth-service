package com.alamano.auth.application;

import com.alamano.auth.domain.Usuario;

/**
 * Puerto para la generacion de tokens de sesion. La implementacion real
 * (JWT firmado con RS256) vive en infrastructure/security/JwtTokenService.
 */
public interface TokenService {
    String generarToken(Usuario usuario);
}
