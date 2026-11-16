package com.alamano.auth.application.exception;

/** La operacion choca con una regla del negocio (409), por ejemplo cambiarse el propio rol. */
public class OperacionNoPermitidaException extends RuntimeException {
    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
