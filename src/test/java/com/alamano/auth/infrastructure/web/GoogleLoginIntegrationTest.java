package com.alamano.auth.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.alamano.auth.application.GoogleIdTokenVerifier;
import com.alamano.auth.application.GoogleIdentity;
import com.alamano.auth.application.exception.TokenGoogleInvalidoException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** HU-Q2 de punta a punta: POST /auth/google crea la cuenta sin contrasena y emite un JWT corto. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoogleLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    // El verificador real llama al JWKS de Google; aqui se reemplaza, que ya tiene su propia prueba.
    @MockitoBean
    private GoogleIdTokenVerifier verifier;

    @BeforeEach
    void limpiar() {
        jdbc.update("DELETE FROM auth.usuarios");
        when(verifier.verificar("tok-ana")).thenReturn(new GoogleIdentity("g-ana", "ana@gmail.com", true, "Ana Torres"));
        when(verifier.verificar("tok-malo")).thenThrow(new TokenGoogleInvalidoException("firma invalida"));
    }

    private MvcResult loginGoogle(String cuerpo) throws Exception {
        return mockMvc.perform(post("/auth/google").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andReturn();
    }

    @Test
    void conUnIdTokenValidoDevuelve200YCreaLaCuentaSinContrasena() throws Exception {
        MvcResult res = loginGoogle("{\"idToken\":\"tok-ana\"}");

        assertEquals(200, res.getResponse().getStatus(), res.getResponse().getContentAsString());
        String cuerpo = res.getResponse().getContentAsString();
        assertTrue(cuerpo.contains("\"correo\":\"ana@gmail.com\""), cuerpo);
        assertTrue(cuerpo.contains("\"rol\":\"USUARIO\""), cuerpo);

        assertNull(jdbc.queryForObject("SELECT password_hash FROM auth.usuarios WHERE correo = ?",
                String.class, "ana@gmail.com"), "no debe haber contrasena almacenada");
        assertEquals("g-ana", jdbc.queryForObject("SELECT google_sub FROM auth.usuarios WHERE correo = ?",
                String.class, "ana@gmail.com"));
    }

    @Test
    void elJwtPropioLlevaElRolYViveTreintaMinutosOMenos() throws Exception {
        String cuerpo = loginGoogle("{\"idToken\":\"tok-ana\"}").getResponse().getContentAsString();
        String token = entreComillas(cuerpo, "token");
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

        assertTrue(payload.contains("\"role\":\"CLIENT\""), payload);
        long iat = numero(payload, "iat");
        long exp = numero(payload, "exp");
        assertTrue(exp - iat <= 30 * 60, "vida del token en segundos: " + (exp - iat));
    }

    @Test
    void dosLoginsConElMismoSubNoDuplicanLaCuenta() throws Exception {
        loginGoogle("{\"idToken\":\"tok-ana\"}");
        loginGoogle("{\"idToken\":\"tok-ana\"}");

        assertEquals(1, (int) jdbc.queryForObject("SELECT COUNT(*) FROM auth.usuarios", Integer.class));
    }

    @Test
    void unaCuentaDeGoogleNoPuedeEntrarConContrasena() throws Exception {
        loginGoogle("{\"idToken\":\"tok-ana\"}");

        int status = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@gmail.com\",\"password\":\"cualquiera\"}"))
                .andReturn().getResponse().getStatus();
        assertEquals(401, status);
    }

    @Test
    void unIdTokenInvalidoDevuelve401() throws Exception {
        MvcResult res = loginGoogle("{\"idToken\":\"tok-malo\"}");

        assertEquals(401, res.getResponse().getStatus());
        assertEquals(0, (int) jdbc.queryForObject("SELECT COUNT(*) FROM auth.usuarios", Integer.class));
    }

    @Test
    void sinIdTokenDevuelve400() throws Exception {
        when(verifier.verificar(anyString())).thenThrow(new AssertionError("no deberia llegar al verificador"));
        assertEquals(400, loginGoogle("{}").getResponse().getStatus());
    }

    private static String entreComillas(String json, String campo) {
        int i = json.indexOf("\"" + campo + "\":\"");
        assertTrue(i >= 0, "no encontre el campo " + campo + " en: " + json);
        int desde = i + campo.length() + 4;
        return json.substring(desde, json.indexOf('"', desde));
    }

    private static long numero(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\":(\\d+)").matcher(json);
        assertTrue(m.find(), "no encontre " + campo + " en " + json);
        return Long.parseLong(m.group(1));
    }
}
