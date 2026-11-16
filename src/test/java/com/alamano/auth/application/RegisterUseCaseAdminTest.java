package com.alamano.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alamano.auth.application.exception.AccesoDenegadoException;
import com.alamano.auth.domain.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** HU-Q3: nadie se hace administrador registrandose, ni siquiera mandando rol ADMIN a mano. */
class RegisterUseCaseAdminTest {

    @Test
    void registrarseConRolAdminSeRechazaYNoGuardaNada() {
        UsuarioRepositoryFalso repositorio = new UsuarioRepositoryFalso();
        RegisterUseCase registro = new RegisterUseCase(repositorio, new BCryptPasswordEncoder());

        assertThrows(AccesoDenegadoException.class,
                () -> registro.ejecutar("Ana", "ana@correo.com", "Segura123", Rol.ADMIN));

        assertTrue(repositorio.guardados.isEmpty());
    }

    @Test
    void registrarseComoBarberoSigueFuncionando() {
        RegisterUseCase registro = new RegisterUseCase(new UsuarioRepositoryFalso(), new BCryptPasswordEncoder());

        assertEquals(Rol.VENDEDOR, registro.ejecutar("Ana", "ana@correo.com", "Segura123", Rol.VENDEDOR).rol());
    }
}
