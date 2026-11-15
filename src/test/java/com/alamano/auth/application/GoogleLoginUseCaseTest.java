package com.alamano.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alamano.auth.application.exception.TokenGoogleInvalidoException;
import com.alamano.auth.domain.Rol;
import com.alamano.auth.domain.Usuario;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** HU-Q2: login con Google crea o vincula la cuenta y emite un JWT corto. */
class GoogleLoginUseCaseTest {
    private UsuarioRepositoryFalso repositorio;
    private final Map<String, GoogleIdentity> tokensValidos = new HashMap<>();
    private Duration vidaEmitida;
    private GoogleLoginUseCase login;

    @BeforeEach
    void setUp() {
        repositorio = new UsuarioRepositoryFalso();
        GoogleIdTokenVerifier verifier = token -> {
            GoogleIdentity identidad = tokensValidos.get(token);
            if (identidad == null) throw new TokenGoogleInvalidoException("firma invalida");
            return identidad;
        };
        TokenService tokenService = new TokenService() {
            @Override public String generarToken(Usuario usuario) { return generarToken(usuario, Duration.ofHours(1)); }
            @Override public String generarToken(Usuario usuario, Duration vida) {
                vidaEmitida = vida;
                return "jwt-de-" + usuario.correo();
            }
        };
        login = new GoogleLoginUseCase(repositorio, verifier, tokenService, 30);
        tokensValidos.put("tok-ana", new GoogleIdentity("g-ana", "Ana@Gmail.com", true, "Ana Torres"));
    }

    @Test
    void unUsuarioNuevoSeCreaSinContrasenaYComoUsuario() {
        LoginUseCase.Resultado resultado = login.ejecutar("tok-ana");

        Usuario usuario = resultado.usuario();
        assertEquals("ana@gmail.com", usuario.correo());
        assertEquals("Ana Torres", usuario.nombre());
        assertEquals(Rol.USUARIO, usuario.rol());
        assertEquals("g-ana", usuario.googleSub());
        assertNull(usuario.passwordHash(), "una cuenta de Google no guarda contrasena");
        assertFalse(usuario.tieneContrasena());
        assertEquals("jwt-de-ana@gmail.com", resultado.token());
    }

    @Test
    void elTokenPropioViveTreintaMinutosOMenos() {
        login.ejecutar("tok-ana");
        assertTrue(vidaEmitida.compareTo(Duration.ofMinutes(30)) <= 0, "vida emitida: " + vidaEmitida);
    }

    @Test
    void unaVidaConfiguradaMayorSeRecortaATreinta() {
        new GoogleLoginUseCase(repositorio, t -> tokensValidos.get(t), new TokenService() {
            @Override public String generarToken(Usuario usuario) { return ""; }
            @Override public String generarToken(Usuario usuario, Duration vida) { vidaEmitida = vida; return ""; }
        }, 120).ejecutar("tok-ana");

        assertEquals(Duration.ofMinutes(30), vidaEmitida);
    }

    @Test
    void elSegundoLoginReusaLaMismaCuenta() {
        Usuario primero = login.ejecutar("tok-ana").usuario();
        Usuario segundo = login.ejecutar("tok-ana").usuario();

        assertEquals(primero.id(), segundo.id());
        assertEquals(1, repositorio.guardados.size());
    }

    @Test
    void unaCuentaConContrasenaYMismoCorreoSeVinculaAGoogle() {
        String hash = new BCryptPasswordEncoder().encode("Segura123");
        Usuario existente = repositorio.save(Usuario.nuevo("Ana", "ana@gmail.com", hash, Rol.VENDEDOR));

        Usuario vinculado = login.ejecutar("tok-ana").usuario();

        assertEquals(existente.id(), vinculado.id());
        assertEquals("g-ana", vinculado.googleSub());
        assertEquals(Rol.VENDEDOR, vinculado.rol(), "vincular no cambia el rol");
        assertEquals(hash, vinculado.passwordHash(), "vincular no borra la contrasena que ya tenia");
    }

    @Test
    void unCorreoNoVerificadoPorGoogleNoEntra() {
        tokensValidos.put("tok-dudoso", new GoogleIdentity("g-x", "x@gmail.com", false, "X"));

        assertThrows(TokenGoogleInvalidoException.class, () -> login.ejecutar("tok-dudoso"));
        assertTrue(repositorio.guardados.isEmpty());
    }

    @Test
    void unTokenInvalidoNoEntraNiCreaNada() {
        assertThrows(TokenGoogleInvalidoException.class, () -> login.ejecutar("tok-falso"));
        assertTrue(repositorio.guardados.isEmpty());
    }

    @Test
    void sinNombreEnGoogleSeUsaLaParteLocalDelCorreo() {
        tokensValidos.put("tok-sin-nombre", new GoogleIdentity("g-s", "pedro.gomez@gmail.com", true, " "));

        assertEquals("pedro.gomez", login.ejecutar("tok-sin-nombre").usuario().nombre());
    }
}
