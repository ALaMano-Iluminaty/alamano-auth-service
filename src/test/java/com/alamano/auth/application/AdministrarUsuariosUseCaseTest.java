package com.alamano.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alamano.auth.application.exception.AccesoDenegadoException;
import com.alamano.auth.application.exception.OperacionNoPermitidaException;
import com.alamano.auth.application.exception.UsuarioNoEncontradoException;
import com.alamano.auth.domain.Rol;
import com.alamano.auth.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HU-Q3: solo un ADMIN lista cuentas y cambia roles; el rol se lee de la base, no del token. */
class AdministrarUsuariosUseCaseTest {
    private UsuarioRepositoryFalso repositorio;
    private AdministrarUsuariosUseCase casoDeUso;
    private Usuario admin;
    private Usuario cliente;

    @BeforeEach
    void setUp() {
        repositorio = new UsuarioRepositoryFalso();
        casoDeUso = new AdministrarUsuariosUseCase(repositorio);
        admin = repositorio.save(Usuario.nuevo("Admin", "admin@correo.com", "hash", Rol.ADMIN));
        cliente = repositorio.save(Usuario.nuevo("Cliente", "cliente@correo.com", "hash", Rol.USUARIO));
    }

    @Test
    void elAdminListaTodasLasCuentas() {
        assertEquals(2, casoDeUso.listar(admin.id()).size());
    }

    @Test
    void unClienteNoPuedeListar() {
        assertThrows(AccesoDenegadoException.class, () -> casoDeUso.listar(cliente.id()));
    }

    @Test
    void unBarberoNoPuedeCambiarRoles() {
        Usuario barbero = repositorio.save(Usuario.nuevo("Barbero", "barbero@correo.com", "hash", Rol.VENDEDOR));
        assertThrows(AccesoDenegadoException.class,
                () -> casoDeUso.cambiarRol(barbero.id(), cliente.id(), Rol.VENDEDOR));
    }

    @Test
    void unaCuentaQueYaNoExisteNoTieneAcceso() {
        assertThrows(AccesoDenegadoException.class, () -> casoDeUso.listar(999L));
    }

    @Test
    void elAdminConvierteAUnClienteEnBarbero() {
        Usuario actualizado = casoDeUso.cambiarRol(admin.id(), cliente.id(), Rol.VENDEDOR);

        assertEquals(Rol.VENDEDOR, actualizado.rol());
        assertEquals(Rol.VENDEDOR, repositorio.findById(cliente.id()).orElseThrow().rol());
    }

    @Test
    void elAdminPuedeHacerAdminAOtraCuenta() {
        assertEquals(Rol.ADMIN, casoDeUso.cambiarRol(admin.id(), cliente.id(), Rol.ADMIN).rol());
    }

    @Test
    void cambiarAlMismoRolNoGuardaNada() {
        int guardadosAntes = repositorio.guardados.size();

        Usuario resultado = casoDeUso.cambiarRol(admin.id(), cliente.id(), Rol.USUARIO);

        assertEquals(Rol.USUARIO, resultado.rol());
        assertEquals(guardadosAntes, repositorio.guardados.size());
    }

    @Test
    void elAdminNoPuedeCambiarSuPropioRol() {
        assertThrows(OperacionNoPermitidaException.class,
                () -> casoDeUso.cambiarRol(admin.id(), admin.id(), Rol.USUARIO));
        assertEquals(Rol.ADMIN, repositorio.findById(admin.id()).orElseThrow().rol());
    }

    @Test
    void cambiarElRolDeUnaCuentaInexistenteDa404() {
        assertThrows(UsuarioNoEncontradoException.class, () -> casoDeUso.cambiarRol(admin.id(), 999L, Rol.VENDEDOR));
    }

    @Test
    void siAAlguienLeQuitanElRolDeAdminPierdeElAccesoAlInstante() {
        Usuario otroAdmin = casoDeUso.cambiarRol(admin.id(), cliente.id(), Rol.ADMIN);
        casoDeUso.cambiarRol(otroAdmin.id(), admin.id(), Rol.USUARIO);

        assertThrows(AccesoDenegadoException.class, () -> casoDeUso.listar(admin.id()));
    }
}
