package com.alamano.auth.application;

import com.alamano.auth.application.exception.TokenGoogleInvalidoException;

/**
 * Puerto que valida un id_token de Google: firma, emisor, audiencia y expiracion.
 * La implementacion real (Nimbus contra las llaves publicas de Google) vive en
 * infrastructure/security.
 */
public interface GoogleIdTokenVerifier {
    GoogleIdentity verificar(String idToken) throws TokenGoogleInvalidoException;
}
