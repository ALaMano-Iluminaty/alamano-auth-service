package com.alamano.auth.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** HU-Q3: el filtro solo autentica JWT propios, firmados con nuestra llave y vigentes. */
class JwtAuthenticationFilterTest {
    private static KeyPair par;
    private static KeyPair otroPar;

    @BeforeAll
    static void generarLlaves() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        par = generador.generateKeyPair();
        otroPar = generador.generateKeyPair();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static String token(KeyPair llaves, String sub, Instant expira) {
        return Jwts.builder()
                .subject(sub)
                .claim("rol", "ADMIN")
                .issuedAt(Date.from(Instant.now().minusSeconds(10)))
                .expiration(Date.from(expira))
                .signWith(llaves.getPrivate())
                .compact();
    }

    private static Authentication filtrar(String cabecera) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (cabecera != null) request.addHeader("Authorization", cabecera);
        new JwtAuthenticationFilter((RSAPublicKey) par.getPublic())
                .doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void unTokenValidoDejaLaCuentaAutenticada() throws Exception {
        Authentication autenticacion = filtrar("Bearer " + token(par, "42", Instant.now().plusSeconds(600)));

        assertNotNull(autenticacion);
        assertEquals("42", autenticacion.getName());
        assertEquals("ROLE_ADMIN", autenticacion.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void sinCabeceraNoAutentica() throws Exception {
        assertNull(filtrar(null));
    }

    @Test
    void unTokenVencidoNoAutentica() throws Exception {
        assertNull(filtrar("Bearer " + token(par, "42", Instant.now().minusSeconds(600))));
    }

    @Test
    void unTokenFirmadoConOtraLlaveNoAutentica() throws Exception {
        assertNull(filtrar("Bearer " + token(otroPar, "42", Instant.now().plusSeconds(600))));
    }

    @Test
    void unTokenBasuraNoAutentica() throws Exception {
        assertNull(filtrar("Bearer esto-no-es-un-jwt"));
    }

    @Test
    void unSubQueNoEsNumericoNoAutentica() throws Exception {
        assertNull(filtrar("Bearer " + token(par, "admin", Instant.now().plusSeconds(600))));
    }

    @Test
    void otroEsquemaDeAutorizacionSeIgnora() throws Exception {
        assertNull(filtrar("Basic " + token(par, "42", Instant.now().plusSeconds(600))));
    }
}
