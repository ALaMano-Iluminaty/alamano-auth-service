package com.alamano.auth.application;

import com.alamano.auth.application.exception.CorreoYaRegistradoException;
import com.alamano.auth.domain.Rol;
import com.alamano.auth.domain.Usuario;
import com.alamano.auth.domain.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Tarea 1.1: [Auth] Registro de usuarios con validacion y hash bcrypt.
 * AC de HU1 que cubre:
 * - Registrarme con nombre, correo y contrasena.
 * - El sistema valida que el correo no esté ya registrado (409 si lo está).
 */
@Service
public class RegisterUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterUseCase(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario ejecutar(String nombre, String correo, String passwordPlano, Rol rol) {
        String correoNormalizado = correo.trim().toLowerCase();

        if (usuarioRepository.existsByCorreo(correoNormalizado)) {
            throw new CorreoYaRegistradoException(correoNormalizado);
        }

        String hash = passwordEncoder.encode(passwordPlano);
        Rol rolFinal = rol != null ? rol : Rol.USUARIO;

        Usuario nuevoUsuario = Usuario.nuevo(nombre.trim(), correoNormalizado, hash, rolFinal);
        return usuarioRepository.save(nuevoUsuario);
    }
}
