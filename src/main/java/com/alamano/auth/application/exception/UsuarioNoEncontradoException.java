package com.alamano.auth.application.exception;

public class UsuarioNoEncontradoException extends RuntimeException {
    public UsuarioNoEncontradoException(Long id) {
        super("No existe el usuario " + id);
    }
}
