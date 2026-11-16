package com.alamano.auth.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alamano.auth.domain.Rol;
import com.alamano.auth.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** HU-Q3: el administrador viaja como ADMIN en los dos claims de rol. */
class JwtTokenServiceAdminTest {

    @Test
    void elAdminViajaComoAdminEnElClaimCompartido() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        KeyPair par = generador.generateKeyPair();
        Usuario admin = new Usuario(7L, "Admin", "admin@example.com", null, Rol.ADMIN, Instant.now(), "g-1");

        String token = new JwtTokenService(par.getPrivate(), 60).generarToken(admin);
        Claims claims = Jwts.parser().verifyWith(par.getPublic()).build().parseSignedClaims(token).getPayload();

        assertEquals("ADMIN", claims.get("role", String.class));
        assertEquals("ADMIN", claims.get("rol", String.class));
        assertEquals("7", claims.getSubject());
    }
}
