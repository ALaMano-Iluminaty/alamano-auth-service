package com.alamano.auth.application.exception;

public class CorreoYaRegistradoException extends RuntimeException {
    public CorreoYaRegistradoException(String correo) {
        super("Ese correo ya esta registrado: " + correo);
    }
}
