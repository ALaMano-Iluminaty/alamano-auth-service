package com.alamano.auth.application.exception;

/** La cuenta esta autenticada pero su rol no le permite la operacion (403). */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
