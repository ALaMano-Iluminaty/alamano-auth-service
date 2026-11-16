package com.alamano.auth.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alamano.auth.application.TokenService;
import com.alamano.auth.domain.Rol;
import com.alamano.auth.domain.Usuario;
import com.alamano.auth.domain.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** HU-Q3 de punta a punta: JWT real, filtro, caso de uso, JPA y H2. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuarioAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private TokenService tokens;

    private Usuario admin;
    private Usuario cliente;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM auth.usuarios");
        admin = usuarios.save(Usuario.nuevo("Admin", "admin@correo.com", "hash", Rol.ADMIN));
        cliente = usuarios.save(Usuario.nuevo("Cliente", "cliente@correo.com", "hash", Rol.USUARIO));
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + tokens.generarToken(usuario);
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/auth/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void conUnTokenManipuladoResponde401() throws Exception {
        mockMvc.perform(get("/auth/usuarios").header("Authorization", bearer(admin) + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unClienteConTokenValidoRecibe403() throws Exception {
        mockMvc.perform(get("/auth/usuarios").header("Authorization", bearer(cliente)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unClienteNoPuedeAscenderseASiMismo() throws Exception {
        mockMvc.perform(patch("/auth/usuarios/" + cliente.id() + "/rol")
                        .header("Authorization", bearer(cliente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elAdminListaLasCuentas() throws Exception {
        mockMvc.perform(get("/auth/usuarios").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].correo", is("admin@correo.com")))
                .andExpect(jsonPath("$[0].rol", is("ADMIN")))
                .andExpect(jsonPath("$[1].rol", is("USUARIO")));
    }

    @Test
    void elAdminConvierteAUnClienteEnBarbero() throws Exception {
        mockMvc.perform(patch("/auth/usuarios/" + cliente.id() + "/rol")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\":\"VENDEDOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("VENDEDOR")))
                .andExpect(jsonPath("$.correo", is("cliente@correo.com")));

        mockMvc.perform(get("/auth/usuarios").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[1].rol", is("VENDEDOR")));
    }

    @Test
    void elAdminNoPuedeCambiarSuPropioRol() throws Exception {
        mockMvc.perform(patch("/auth/usuarios/" + admin.id() + "/rol")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\":\"USUARIO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void unaCuentaInexistenteDa404() throws Exception {
        mockMvc.perform(patch("/auth/usuarios/99999/rol")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\":\"VENDEDOR\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unRolQueNoExisteDa400() throws Exception {
        mockMvc.perform(patch("/auth/usuarios/" + cliente.id() + "/rol")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\":\"SUPERUSUARIO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinRolEnElCuerpoDa400() throws Exception {
        mockMvc.perform(patch("/auth/usuarios/" + cliente.id() + "/rol")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
