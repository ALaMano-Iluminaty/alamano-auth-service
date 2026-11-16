package com.alamano.auth.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Politica del proyecto: con LOCAL_LOGIN_ENABLED apagado, el login casero no existe. */
@SpringBootTest(properties = "app.auth.local-login-enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginLocalDeshabilitadoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerNoExiste() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"correo\":\"ana@correo.com\",\"password\":\"Segura123\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void loginNoExiste() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@correo.com\",\"password\":\"Segura123\"}"))
                .andExpect(status().isNotFound());
    }
}
