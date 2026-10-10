package com.alamano.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alamano.auth.application.exception.CorreoYaRegistradoException;
import com.alamano.auth.domain.Rol;
import com.alamano.auth.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** QA 1.4 (AB#334): registro de usuarios con validacion y hash bcrypt. */
class RegisterUseCaseTest {
    private static final String PASSWORD = "Segura123";

    private UsuarioRepositoryFalso repositorio;
    private PasswordEncoder encoder;
    private RegisterUseCase registrar;

    @BeforeEach
    void setUp() {
        repositorio = new UsuarioRepositoryFalso();
        // El encoder real, no un doble: asi se comprueba que el hash sea bcrypt de verdad.
        encoder = new BCryptPasswordEncoder();
        registrar = new RegisterUseCase(repositorio, encoder);
    }

    @Test
    void laContrasenaSeGuardaHasheadaYNuncaEnClaro() {
        Usuario usuario = registrar.ejecutar("Ana Torres", "ana@example.com", PASSWORD, Rol.USUARIO);

        assertNotEquals(PASSWORD, usuario.passwordHash());
        assertTrue(usuario.passwordHash().startsWith("$2"), "deberia ser un hash bcrypt");
        assertTrue(encoder.matches(PASSWORD, usuario.passwordHash()), "el hash debe validar la contrasena original");
    }

    @Test
    void elCorreoSeNormalizaAMinusculasYSinEspacios() {
        Usuario usuario = registrar.ejecutar("Ana", "  ANA@Example.COM  ", PASSWORD, Rol.USUARIO);

        assertEquals("ana@example.com", usuario.correo());
        assertTrue(repositorio.existsByCorreo("ana@example.com"));
    }

    @Test
    void elNombreSeRecorta() {
        assertEquals("Ana Torres", registrar.ejecutar("  Ana Torres  ", "a@b.com", PASSWORD, Rol.USUARIO).nombre());
    }

    @Test
    void sinRolQuedaComoUsuario() {
        assertEquals(Rol.USUARIO, registrar.ejecutar("Ana", "a@b.com", PASSWORD, null).rol());
    }

    @Test
    void elRolDeVendedorSeRespeta() {
        assertEquals(Rol.VENDEDOR, registrar.ejecutar("Pedro", "p@b.com", PASSWORD, Rol.VENDEDOR).rol());
    }

    @Test
    void unCorreoRepetidoNoSeGuardaDosVeces() {
        registrar.ejecutar("Ana", "ana@example.com", PASSWORD, Rol.USUARIO);

        assertThrows(CorreoYaRegistradoException.class,
                () -> registrar.ejecutar("Otra Ana", "ana@example.com", "Otra456", Rol.USUARIO));
        assertEquals(1, repositorio.guardados.size(), "el segundo registro no debe escribir nada");
    }

    @Test
    void elDuplicadoSeDetectaAunqueCambieLaCapitalizacion() {
        registrar.ejecutar("Ana", "ana@example.com", PASSWORD, Rol.USUARIO);

        assertThrows(CorreoYaRegistradoException.class,
                () -> registrar.ejecutar("Ana", "ANA@EXAMPLE.COM", PASSWORD, Rol.USUARIO));
    }

    @Test
    void dosUsuariosConLaMismaContrasenaTienenHashesDistintos() {
        String uno = registrar.ejecutar("Ana", "a@b.com", PASSWORD, Rol.USUARIO).passwordHash();
        String otro = registrar.ejecutar("Pedro", "p@b.com", PASSWORD, Rol.USUARIO).passwordHash();

        // bcrypt usa una sal distinta cada vez; si fueran iguales, no se estaria salando.
        assertNotEquals(uno, otro);
    }
}
