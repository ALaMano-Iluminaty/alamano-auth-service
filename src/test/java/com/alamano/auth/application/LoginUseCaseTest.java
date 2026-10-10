package com.alamano.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alamano.auth.application.exception.CredencialesInvalidasException;
import com.alamano.auth.domain.Rol;
import com.alamano.auth.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** QA 1.4 (AB#334): inicio de sesion y emision del token. */
class LoginUseCaseTest {
    private static final String PASSWORD = "Segura123";

    private UsuarioRepositoryFalso repositorio;
    private LoginUseCase iniciarSesion;
    private int tokensEmitidos;

    @BeforeEach
    void setUp() {
        repositorio = new UsuarioRepositoryFalso();
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        tokensEmitidos = 0;
        TokenService tokenService = usuario -> {
            tokensEmitidos++;
            return "token-de-" + usuario.correo();
        };
        // Un usuario ya registrado, con la contrasena hasheada igual que en produccion.
        new RegisterUseCase(repositorio, encoder).ejecutar("Ana Torres", "ana@example.com", PASSWORD, Rol.VENDEDOR);
        iniciarSesion = new LoginUseCase(repositorio, encoder, tokenService);
    }

    @Test
    void conLasCredencialesCorrectasDevuelveTokenYUsuario() {
        LoginUseCase.Resultado resultado = iniciarSesion.ejecutar("ana@example.com", PASSWORD);

        assertEquals("token-de-ana@example.com", resultado.token());
        assertEquals("ana@example.com", resultado.usuario().correo());
        assertEquals(Rol.VENDEDOR, resultado.usuario().rol());
        assertEquals(1, tokensEmitidos);
    }

    @Test
    void elCorreoNoDistingueMayusculasNiEspacios() {
        assertEquals("ana@example.com", iniciarSesion.ejecutar("  ANA@Example.COM  ", PASSWORD).usuario().correo());
    }

    @Test
    void unaContrasenaIncorrectaNoDejaEntrar() {
        assertThrows(CredencialesInvalidasException.class,
                () -> iniciarSesion.ejecutar("ana@example.com", "LaQueNoEs"));
        assertEquals(0, tokensEmitidos, "no se debe emitir token si fallan las credenciales");
    }

    @Test
    void unCorreoQueNoExisteNoDejaEntrar() {
        assertThrows(CredencialesInvalidasException.class,
                () -> iniciarSesion.ejecutar("nadie@example.com", PASSWORD));
        assertEquals(0, tokensEmitidos);
    }

    @Test
    void elMensajeNoRevelaSiElCorreoExiste() {
        // Si el mensaje cambiara, se podria averiguar que correos estan registrados.
        String correoMal = assertThrows(CredencialesInvalidasException.class,
                () -> iniciarSesion.ejecutar("nadie@example.com", PASSWORD)).getMessage();
        String passwordMal = assertThrows(CredencialesInvalidasException.class,
                () -> iniciarSesion.ejecutar("ana@example.com", "LaQueNoEs")).getMessage();

        assertEquals(correoMal, passwordMal);
        assertTrue(correoMal.toLowerCase().contains("incorrect"), "mensaje inesperado: " + correoMal);
    }
}
