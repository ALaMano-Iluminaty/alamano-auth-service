package com.alamano.auth.application.exception;

public class TokenGoogleInvalidoException extends RuntimeException {
    public TokenGoogleInvalidoException(String detalle) {
        super("El token de Google no es valido: " + detalle);
    }

    public TokenGoogleInvalidoException(String detalle, Throwable causa) {
        super("El token de Google no es valido: " + detalle, causa);
    }
}
