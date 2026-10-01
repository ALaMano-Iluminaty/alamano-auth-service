package com.alamano.auth.application.exception;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Correo o contrasena incorrectos");
    }
}
