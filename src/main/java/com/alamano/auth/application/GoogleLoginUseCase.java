package com.alamano.auth.application;

import com.alamano.auth.application.exception.TokenGoogleInvalidoException;
import com.alamano.auth.domain.Usuario;
import com.alamano.auth.domain.UsuarioRepository;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * HU-Q2: login con Google. El id_token ya validado identifica al usuario; si no
 * existe se crea sin contrasena y, si ya existia con ese correo, se le vincula el
 * sub de Google. El JWT propio que se emite vive como maximo 30 minutos.
 */
@Service
public class GoogleLoginUseCase {
    static final long VIDA_MAXIMA_MINUTOS = 30;

    private final UsuarioRepository usuarioRepository;
    private final GoogleIdTokenVerifier verifier;
    private final TokenService tokenService;
    private final Duration vidaDelToken;

    public GoogleLoginUseCase(UsuarioRepository usuarioRepository, GoogleIdTokenVerifier verifier,
                              TokenService tokenService,
                              @Value("${app.google.expiration-minutes:30}") long expirationMinutes) {
        this.usuarioRepository = usuarioRepository;
        this.verifier = verifier;
        this.tokenService = tokenService;
        this.vidaDelToken = Duration.ofMinutes(Math.min(expirationMinutes, VIDA_MAXIMA_MINUTOS));
    }

    public LoginUseCase.Resultado ejecutar(String idToken) {
        GoogleIdentity identidad = verifier.verificar(idToken);
        if (!identidad.correoVerificado()) {
            throw new TokenGoogleInvalidoException("el correo no esta verificado por Google");
        }
        String correo = identidad.correo().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByGoogleSub(identidad.sub())
                .or(() -> usuarioRepository.findByCorreo(correo)
                        .map(existente -> usuarioRepository.save(existente.vinculadoAGoogle(identidad.sub()))))
                .orElseGet(() -> usuarioRepository.save(Usuario.deGoogle(nombreDe(identidad, correo), correo,
                        identidad.sub())));

        return new LoginUseCase.Resultado(tokenService.generarToken(usuario, vidaDelToken), usuario);
    }

    private static String nombreDe(GoogleIdentity identidad, String correo) {
        if (identidad.nombre() != null && !identidad.nombre().isBlank()) return identidad.nombre().trim();
        return correo.substring(0, correo.indexOf('@'));
    }
}
