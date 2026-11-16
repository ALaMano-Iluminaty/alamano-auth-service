package com.alamano.auth.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lee el JWT propio (Authorization: Bearer ...) y lo verifica con la llave PUBLICA:
 * firma RS256 y vencimiento. Si es valido deja la cuenta autenticada; si no, la
 * peticion sigue sin autenticar y las reglas de SecurityConfig responden 401.
 *
 * Solo identifica a la cuenta. Que pueda administrar o no lo decide el caso de uso
 * mirando su rol actual en la base de datos, no el que trae el token.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtDecoder decoder;

    public JwtAuthenticationFilter(RSAPublicKey llavePublica) {
        this.decoder = NimbusJwtDecoder.withPublicKey(llavePublica)
                .signatureAlgorithm(algoritmoPara(llavePublica))
                .build();
    }

    /** El mismo criterio con el que JJWT elige el algoritmo al firmar segun el tamano de la llave RSA. */
    private static SignatureAlgorithm algoritmoPara(RSAPublicKey llave) {
        int bits = llave.getModulus().bitLength();
        if (bits >= 4096) return SignatureAlgorithm.RS512;
        if (bits >= 3072) return SignatureAlgorithm.RS384;
        return SignatureAlgorithm.RS256;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            autenticar(cabecera.substring(PREFIJO.length()).trim());
        }
        chain.doFilter(request, response);
    }

    private void autenticar(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            String sub = jwt.getSubject();
            if (sub == null || !sub.chars().allMatch(Character::isDigit)) {
                return;
            }
            String rol = jwt.getClaimAsString("rol");
            List<SimpleGrantedAuthority> autoridades =
                    rol == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(sub, null, autoridades));
        } catch (JwtException e) {
            // Firma invalida, token vencido o mal formado: queda sin autenticar.
            logger.debug("JWT rechazado: " + e.getMessage());
        }
    }
}
