package com.alamano.auth.infrastructure.security;

import com.alamano.auth.application.TokenService;
import com.alamano.auth.domain.Rol;
import com.alamano.auth.domain.Usuario;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Firma el JWT con la llave PRIVADA (RS256). Core y el Gateway verifican
 * con la llave PUBLICA correspondiente - nunca comparten la privada.
 *
 * El token lleva el rol dos veces a proposito: "rol" con el nombre del dominio
 * (USUARIO/VENDEDOR), que usa el frontend, y "role" con el nombre acordado entre
 * servicios (CLIENT/PROFESSIONAL), que es el que leen Core y el Gateway.
 */
@Component
public class JwtTokenService implements TokenService {

    private final PrivateKey privateKey;
    private final long expirationMinutes;

    public JwtTokenService(PrivateKey jwtPrivateKey,
                            @Value("${app.jwt.expiration-minutes:60}") long expirationMinutes) {
        this.privateKey = jwtPrivateKey;
        this.expirationMinutes = expirationMinutes;
    }

    @Override
    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(Duration.ofMinutes(expirationMinutes));

        return Jwts.builder()
                .subject(String.valueOf(usuario.id()))
                .claim("correo", usuario.correo())
                .claim("nombre", usuario.nombre())
                .claim("rol", usuario.rol().name())
                .claim("role", nombreCompartido(usuario.rol()))
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(privateKey)
                .compact();
    }

    /** Nombre del rol en el contrato entre servicios. */
    private static String nombreCompartido(Rol rol) {
        return switch (rol) {
            case VENDEDOR -> "PROFESSIONAL";
            case USUARIO -> "CLIENT";
        };
    }
}
