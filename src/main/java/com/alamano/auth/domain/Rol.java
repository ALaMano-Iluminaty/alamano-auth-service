package com.alamano.auth.domain;

public enum Rol {
    USUARIO,
    VENDEDOR,
    /** Administra roles. Nunca se obtiene registrandose: solo lo asigna otro ADMIN o ADMIN_EMAILS. */
    ADMIN
}
