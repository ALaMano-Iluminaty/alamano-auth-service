package com.alamano.auth.application;

import com.alamano.auth.application.exception.CredencialesInvalidasException;
import com.alamano.auth.domain.Usuario;
import com.alamano.auth.domain.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Tarea 1.2: [Auth] Login, emision de JWT RS256 con rol y filtro de verificacion.
 * AC de HU1 que cubre:
 * - Iniciar sesion con correo y contrasena.
 * - El sistema devuelve un token de sesion (JWT) válido al iniciar sesion correctamente.
 * - Si las credenciales son incorrectas, se muestra un mensaje de error claro (401).
 */
@Service
public class LoginUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public LoginUseCase(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public record Resultado(String token, Usuario usuario) {}

    public Resultado ejecutar(String correo, String passwordPlano) {
        String correoNormalizado = correo.trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByCorreo(correoNormalizado)
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordEncoder.matches(passwordPlano, usuario.passwordHash())) {
            throw new CredencialesInvalidasException();
        }

        String token = tokenService.generarToken(usuario);
        return new Resultado(token, usuario);
    }
}
