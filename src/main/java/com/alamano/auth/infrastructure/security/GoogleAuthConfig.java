package com.alamano.auth.infrastructure.security;

import java.util.Collection;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Decodificador del id_token de Google. La firma se verifica contra el JWKS publico de
 * Google (se descarga y cachea solo), y ademas se exige emisor, audiencia y vigencia.
 */
@Configuration
public class GoogleAuthConfig {
    // Google firma con cualquiera de los dos emisores, segun el flujo.
    static final Set<String> EMISORES = Set.of("https://accounts.google.com", "accounts.google.com");

    @Bean
    JwtDecoder googleIdTokenDecoder(@Value("${app.google.jwk-set-uri}") String jwkSetUri,
                                    @Value("${app.google.client-id:}") String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(validadores(clientId));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> validadores(String clientId) {
        OAuth2TokenValidator<Jwt> emisor = new JwtClaimValidator<Object>(JwtClaimNames.ISS,
                iss -> iss != null && EMISORES.contains(iss.toString()));
        OAuth2TokenValidator<Jwt> audiencia = new JwtClaimValidator<Collection<String>>(JwtClaimNames.AUD,
                aud -> aud != null && !clientId.isBlank() && aud.contains(clientId));
        return new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), emisor, audiencia);
    }
}
