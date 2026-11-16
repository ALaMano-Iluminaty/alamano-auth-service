package com.alamano.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alamano.auth.domain.Rol;
import com.alamano.auth.domain.Usuario;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HU-Q3: los correos de ADMIN_EMAILS entran como administradores; los demas, como cliente. */
class GoogleLoginAdminTest {
    private UsuarioRepositoryFalso repositorio;

    @BeforeEach
    void setUp() {
        repositorio = new UsuarioRepositoryFalso();
    }

    private GoogleLoginUseCase casoDeUso(String correosAdmin) {
        GoogleIdTokenVerifier verifier = token -> new GoogleIdentity("g-" + token, token + "@gmail.com", true, token);
        TokenService tokenService = new TokenService() {
            @Override public String generarToken(Usuario usuario) { return generarToken(usuario, Duration.ofMinutes(30)); }
            @Override public String generarToken(Usuario usuario, Duration vida) { return "jwt-" + usuario.rol(); }
        };
        return new GoogleLoginUseCase(repositorio, verifier, tokenService, 30, correosAdmin);
    }

    @Test
    void unCorreoDeLaListaEntraComoAdmin() {
        Usuario usuario = casoDeUso("ana@gmail.com, otro@gmail.com").ejecutar("ana").usuario();

        assertEquals(Rol.ADMIN, usuario.rol());
        assertEquals("jwt-ADMIN", casoDeUso("ana@gmail.com").ejecutar("ana").token());
    }

    @Test
    void laListaIgnoraMayusculasYEspacios() {
        assertEquals(Rol.ADMIN, casoDeUso("  ANA@Gmail.com  ").ejecutar("ana").usuario().rol());
    }

    @Test
    void unCorreoQueNoEstaEnLaListaEntraComoCliente() {
        assertEquals(Rol.USUARIO, casoDeUso("ana@gmail.com").ejecutar("luis").usuario().rol());
    }

    @Test
    void sinListaNadieEsAdmin() {
        assertEquals(Rol.USUARIO, casoDeUso("").ejecutar("ana").usuario().rol());
    }

    @Test
    void unaCuentaExistenteDeLaListaSePromueve() {
        casoDeUso("").ejecutar("ana");

        Usuario promovido = casoDeUso("ana@gmail.com").ejecutar("ana").usuario();

        assertEquals(Rol.ADMIN, promovido.rol());
        assertEquals(Rol.ADMIN, repositorio.findByCorreo("ana@gmail.com").orElseThrow().rol());
    }
}
