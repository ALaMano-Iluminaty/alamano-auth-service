package com.alamano.auth.application;

/** Lo que Auth necesita del id_token de Google una vez verificado. */
public record GoogleIdentity(String sub, String correo, boolean correoVerificado, String nombre) {}
