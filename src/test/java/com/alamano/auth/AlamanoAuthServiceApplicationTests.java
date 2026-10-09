package com.alamano.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Comprueba que el contexto completo arranca: Flyway aplica las migraciones, JPA valida
 * el esquema y la configuracion de seguridad y de llaves se carga. Usa H2 en modo
 * PostgreSQL para no depender de un contenedor levantado a mano.
 */
@SpringBootTest
@ActiveProfiles("test")
class AlamanoAuthServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
