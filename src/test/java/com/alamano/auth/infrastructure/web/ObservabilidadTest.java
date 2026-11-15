package com.alamano.auth.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** HU-Q10: metricas para Prometheus y correlationId en cada peticion. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObservabilidadTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void prometheusExponeLasMetricasSinAutenticacion() throws Exception {
        MvcResult res = mockMvc.perform(get("/actuator/prometheus")).andReturn();

        assertEquals(200, res.getResponse().getStatus());
        String cuerpo = res.getResponse().getContentAsString();
        assertTrue(cuerpo.contains("jvm_memory_used_bytes"), cuerpo.substring(0, Math.min(300, cuerpo.length())));
        assertTrue(cuerpo.contains("application=\"alamano-auth-service\""), "las metricas llevan la etiqueta del servicio");
    }

    @Test
    void healthSiguePublico() throws Exception {
        assertEquals(200, mockMvc.perform(get("/actuator/health")).andReturn().getResponse().getStatus());
    }

    @Test
    void todaRespuestaLlevaUnCorrelationIdGenerado() throws Exception {
        String id = mockMvc.perform(get("/actuator/health")).andReturn().getResponse().getHeader("X-Correlation-Id");

        assertNotNull(id);
        assertEquals(36, id.length(), "sin cabecera entrante se genera un UUID: " + id);
    }

    @Test
    void elCorrelationIdEntranteSeConserva() throws Exception {
        String id = mockMvc.perform(post("/auth/login")
                        .header("X-Correlation-Id", "corr-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"nadie@example.com\",\"password\":\"x\"}"))
                .andReturn().getResponse().getHeader("X-Correlation-Id");

        assertEquals("corr-123", id);
    }
}
