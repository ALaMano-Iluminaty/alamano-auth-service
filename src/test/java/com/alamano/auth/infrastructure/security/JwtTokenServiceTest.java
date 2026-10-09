package com.alamano.auth.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alamano.auth.domain.Rol;
import com.alamano.auth.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** QA 1.4: el token lleva el rol en los dos nombres y lo firma con la llave privada. */
class JwtTokenServiceTest {
    private static KeyPair par;

    @BeforeAll
    static void generarLlaves() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        par = generador.generateKeyPair();
    }

    private Claims claimsDe(Rol rol) {
        Usuario usuario = new Usuario(42L, "Ana Torres", "ana@example.com", "hash", rol, Instant.now());
        String token = new JwtTokenService(par.getPrivate(), 60).generarToken(usuario);
        // Se verifica con la llave PUBLICA, igual que hacen Core y el Gateway.
        return Jwts.parser().verifyWith(par.getPublic()).build().parseSignedClaims(token).getPayload();
    }

    @Test
    void elVendedorViajaComoProfessionalEnElClaimCompartido() {
        Claims claims = claimsDe(Rol.VENDEDOR);

        assertEquals("PROFESSIONAL", claims.get("role", String.class));
        assertEquals("VENDEDOR", claims.get("rol", String.class));
    }

    @Test
    void elUsuarioViajaComoClientEnElClaimCompartido() {
        Claims claims = claimsDe(Rol.USUARIO);

        assertEquals("CLIENT", claims.get("role", String.class));
        assertEquals("USUARIO", claims.get("rol", String.class));
    }

    @Test
    void elSubEsElIdDelUsuarioYElTokenNoNaceVencido() {
        Claims claims = claimsDe(Rol.VENDEDOR);

        assertEquals("42", claims.getSubject());
        assertEquals("ana@example.com", claims.get("correo", String.class));
        assertTrue(claims.getExpiration().toInstant().isAfter(Instant.now()));
    }
}
