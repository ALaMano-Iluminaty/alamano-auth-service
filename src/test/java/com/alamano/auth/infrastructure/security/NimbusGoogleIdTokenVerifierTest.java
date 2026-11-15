package com.alamano.auth.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alamano.auth.application.GoogleIdentity;
import com.alamano.auth.application.exception.TokenGoogleInvalidoException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * HU-Q2: el id_token de Google se acepta solo si la firma, el emisor, la audiencia y la
 * vigencia son correctos. Se usa un par de llaves local en lugar del JWKS de Google, con los
 * mismos validadores que usa la configuracion real.
 */
class NimbusGoogleIdTokenVerifierTest {
    private static final String CLIENT_ID = "1234-alamano.apps.googleusercontent.com";
    private static KeyPair google;
    private static KeyPair otraLlave;
    private static NimbusGoogleIdTokenVerifier verifier;

    @BeforeAll
    static void setUp() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        google = generador.generateKeyPair();
        otraLlave = generador.generateKeyPair();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) google.getPublic()).build();
        decoder.setJwtValidator(GoogleAuthConfig.validadores(CLIENT_ID));
        verifier = new NimbusGoogleIdTokenVerifier(decoder);
    }

    private static String idToken(UnaryOperator<JwtBuilder> ajuste) {
        Instant ahora = Instant.now();
        JwtBuilder builder = Jwts.builder()
                .issuer("https://accounts.google.com")
                .audience().add(CLIENT_ID).and()
                .subject("10769150350006150715113082367")
                .claim("email", "ana@gmail.com")
                .claim("email_verified", true)
                .claim("name", "Ana Torres")
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(Duration.ofHours(1))))
                .signWith(google.getPrivate());
        return ajuste.apply(builder).compact();
    }

    @Test
    void unTokenCorrectoDevuelveLaIdentidad() {
        GoogleIdentity identidad = verifier.verificar(idToken(b -> b));

        assertEquals("10769150350006150715113082367", identidad.sub());
        assertEquals("ana@gmail.com", identidad.correo());
        assertTrue(identidad.correoVerificado());
        assertEquals("Ana Torres", identidad.nombre());
    }

    @Test
    void elEmisorSinEsquemaTambienEsGoogle() {
        assertEquals("ana@gmail.com", verifier.verificar(idToken(b -> b.issuer("accounts.google.com"))).correo());
    }

    @Test
    void firmadoConOtraLlaveSeRechaza() {
        assertThrows(TokenGoogleInvalidoException.class,
                () -> verifier.verificar(idToken(b -> b.signWith(otraLlave.getPrivate()))));
    }

    @Test
    void otroEmisorSeRechaza() {
        assertThrows(TokenGoogleInvalidoException.class,
                () -> verifier.verificar(idToken(b -> b.issuer("https://evil.example.com"))));
    }

    @Test
    void otraAudienciaSeRechaza() {
        assertThrows(TokenGoogleInvalidoException.class,
                () -> verifier.verificar(idToken(b -> b.audience().remove(CLIENT_ID).add("otra-app").and())));
    }

    @Test
    void vencidoSeRechaza() {
        // 5 minutos vencido: fuera de la tolerancia de reloj de 60 s del decodificador.
        Instant hace = Instant.now().minus(Duration.ofMinutes(5));
        assertThrows(TokenGoogleInvalidoException.class,
                () -> verifier.verificar(idToken(b -> b.expiration(Date.from(hace)))));
    }

    @Test
    void manipuladoSeRechaza() {
        String[] partes = idToken(b -> b).split("\\.");
        String json = "{\"iss\":\"https://accounts.google.com\",\"aud\":\"" + CLIENT_ID
                + "\",\"sub\":\"otro\",\"email\":\"otro@gmail.com\"}";
        String payloadAjeno = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
        String manipulado = partes[0] + "." + payloadAjeno + "." + partes[2];

        assertThrows(TokenGoogleInvalidoException.class, () -> verifier.verificar(manipulado));
    }

    @Test
    void unaBasuraCualquieraSeRechaza() {
        assertThrows(TokenGoogleInvalidoException.class, () -> verifier.verificar("no-es-un-jwt"));
    }

    @Test
    void sinCorreoSeRechaza() {
        assertThrows(TokenGoogleInvalidoException.class,
                () -> verifier.verificar(idToken(b -> b.claim("email", null))));
    }

    @Test
    void elCorreoVerificadoComoTextoTambienCuenta() {
        assertTrue(verifier.verificar(idToken(b -> b.claim("email_verified", "true"))).correoVerificado());
        assertFalse(verifier.verificar(idToken(b -> b.claim("email_verified", null))).correoVerificado());
    }

    @Test
    void sinClientIdConfiguradoNadaPasa() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) google.getPublic()).build();
        decoder.setJwtValidator(GoogleAuthConfig.validadores(""));

        assertThrows(TokenGoogleInvalidoException.class,
                () -> new NimbusGoogleIdTokenVerifier(decoder).verificar(idToken(b -> b)));
    }
}
