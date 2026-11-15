package com.alamano.auth.infrastructure.security;

import com.alamano.auth.application.GoogleIdTokenVerifier;
import com.alamano.auth.application.GoogleIdentity;
import com.alamano.auth.application.exception.TokenGoogleInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
public class NimbusGoogleIdTokenVerifier implements GoogleIdTokenVerifier {
    private static final Logger log = LoggerFactory.getLogger(NimbusGoogleIdTokenVerifier.class);

    private final JwtDecoder decoder;

    public NimbusGoogleIdTokenVerifier(@Qualifier("googleIdTokenDecoder") JwtDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public GoogleIdentity verificar(String idToken) {
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException exception) {
            log.warn("google_token_rejected reason={}", exception.getMessage());
            throw new TokenGoogleInvalidoException(exception.getMessage(), exception);
        }
        String correo = jwt.getClaimAsString("email");
        if (correo == null || correo.isBlank()) {
            throw new TokenGoogleInvalidoException("no trae el correo");
        }
        // Google lo manda como booleano; algunos flujos antiguos lo mandan como texto.
        Object emailVerified = jwt.getClaim("email_verified");
        boolean verificado = Boolean.parseBoolean(String.valueOf(emailVerified));
        return new GoogleIdentity(jwt.getSubject(), correo, verificado, jwt.getClaimAsString("name"));
    }
}
