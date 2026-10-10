package com.alamano.auth.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * QA 1.4 (AB#334): prueba de integracion de HU1 de punta a punta, pasando por el
 * controlador, la validacion, el manejador de errores, JPA y la base de datos.
 * Usa H2, asi que no necesita Postgres levantado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {
    private static final String PASSWORD = "Segura123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limpiar() {
        // El esquema es 'auth', no el que trae la conexion por defecto.
        jdbc.update("DELETE FROM auth.usuarios");
    }

    private MvcResult registrar(String cuerpo) throws Exception {
        return mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andReturn();
    }

    private MvcResult iniciarSesion(String cuerpo) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andReturn();
    }

    private static String cuerpoRegistro(String correo, String password, String rol) {
        String campoRol = rol == null ? "" : ",\"rol\":\"" + rol + "\"";
        return "{\"nombre\":\"Ana Torres\",\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"" + campoRol + "}";
    }

    @Test
    void registroDevuelve201ConElUsuarioYSinElHash() throws Exception {
        MvcResult res = registrar(cuerpoRegistro("ana@example.com", PASSWORD, "VENDEDOR"));

        assertEquals(201, res.getResponse().getStatus());
        String cuerpo = res.getResponse().getContentAsString();
        assertTrue(cuerpo.contains("\"correo\":\"ana@example.com\""), cuerpo);
        assertTrue(cuerpo.contains("\"rol\":\"VENDEDOR\""), cuerpo);
        assertFalse(cuerpo.contains("passwordHash"), "la respuesta nunca debe traer el hash: " + cuerpo);
        assertFalse(cuerpo.contains(PASSWORD), "la respuesta nunca debe traer la contrasena: " + cuerpo);

        assertEquals(1, (int) jdbc.queryForObject(
                "SELECT COUNT(*) FROM auth.usuarios WHERE correo = ?", Integer.class, "ana@example.com"));
    }

    @Test
    void laContrasenaSeGuardaHasheadaEnLaBase() throws Exception {
        registrar(cuerpoRegistro("ana@example.com", PASSWORD, null));

        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM auth.usuarios WHERE correo = ?", String.class, "ana@example.com");
        assertTrue(hash.startsWith("$2"), "deberia ser bcrypt, llego: " + hash);
        assertFalse(hash.contains(PASSWORD));
    }

    @Test
    void unCorreoRepetidoDevuelve409() throws Exception {
        registrar(cuerpoRegistro("ana@example.com", PASSWORD, null));

        assertEquals(409, registrar(cuerpoRegistro("ana@example.com", PASSWORD, null)).getResponse().getStatus());
        assertEquals(1, (int) jdbc.queryForObject("SELECT COUNT(*) FROM auth.usuarios", Integer.class));
    }

    @Test
    void unaContrasenaCortaDevuelve400() throws Exception {
        assertEquals(400, registrar(cuerpoRegistro("ana@example.com", "corta", null)).getResponse().getStatus());
        assertEquals(0, (int) jdbc.queryForObject("SELECT COUNT(*) FROM auth.usuarios", Integer.class));
    }

    @Test
    void unCorreoConFormatoInvalidoDevuelve400() throws Exception {
        assertEquals(400, registrar(cuerpoRegistro("no-es-un-correo", PASSWORD, null)).getResponse().getStatus());
    }

    @Test
    void faltarUnCampoObligatorioDevuelve400() throws Exception {
        assertEquals(400, mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@example.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andReturn().getResponse().getStatus());
    }

    @Test
    void loginDevuelve200ConUnTokenUsable() throws Exception {
        registrar(cuerpoRegistro("ana@example.com", PASSWORD, "VENDEDOR"));

        MvcResult res = iniciarSesion("{\"correo\":\"ana@example.com\",\"password\":\"" + PASSWORD + "\"}");
        assertEquals(200, res.getResponse().getStatus());

        String token = entreComillas(res.getResponse().getContentAsString(), "token");
        String[] partes = token.split("\\.");
        assertEquals(3, partes.length, "un JWT tiene tres partes: " + token);

        // El rol viaja dos veces: 'rol' para el frontend y 'role' para Core y el Gateway.
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        assertTrue(payload.contains("\"rol\":\"VENDEDOR\""), payload);
        assertTrue(payload.contains("\"role\":\"PROFESSIONAL\""), payload);
        assertTrue(payload.contains("\"correo\":\"ana@example.com\""), payload);
    }

    @Test
    void elUsuarioCorrienteViajaComoClientEnElToken() throws Exception {
        registrar(cuerpoRegistro("cliente@example.com", PASSWORD, "USUARIO"));

        MvcResult res = iniciarSesion("{\"correo\":\"cliente@example.com\",\"password\":\"" + PASSWORD + "\"}");
        String payload = new String(Base64.getUrlDecoder().decode(
                entreComillas(res.getResponse().getContentAsString(), "token").split("\\.")[1]), StandardCharsets.UTF_8);

        assertTrue(payload.contains("\"role\":\"CLIENT\""), payload);
    }

    @Test
    void unaContrasenaIncorrectaDevuelve401() throws Exception {
        registrar(cuerpoRegistro("ana@example.com", PASSWORD, null));

        assertEquals(401, iniciarSesion(
                "{\"correo\":\"ana@example.com\",\"password\":\"LaQueNoEs\"}").getResponse().getStatus());
    }

    @Test
    void unCorreoQueNoExisteDevuelve401() throws Exception {
        assertEquals(401, iniciarSesion(
                "{\"correo\":\"nadie@example.com\",\"password\":\"" + PASSWORD + "\"}").getResponse().getStatus());
    }

    /** Saca el valor de un campo de texto del JSON sin depender de una libreria extra. */
    private static String entreComillas(String json, String campo) {
        int i = json.indexOf("\"" + campo + "\":\"");
        assertTrue(i >= 0, "no encontre el campo " + campo + " en: " + json);
        int desde = i + campo.length() + 4;
        return json.substring(desde, json.indexOf('"', desde));
    }
}
