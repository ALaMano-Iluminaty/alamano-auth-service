package com.alamano.auth.domain;

import java.time.Instant;

/**
 * Objeto de dominio puro: no conoce JPA, Spring ni ninguna otra tecnologia.
 * HU1: "Como usuario, quiero registrarme e iniciar sesion con correo y
 * contrasena, para acceder a la app".
 */
public record Usuario(
        Long id,
        String nombre,
        String correo,
        String passwordHash,
        Rol rol,
        Instant creadoEn
) {

    public static Usuario nuevo(String nombre, String correo, String passwordHash, Rol rol) {
        return new Usuario(null, nombre, correo.toLowerCase(), passwordHash, rol, Instant.now());
    }

    public Usuario conId(Long id) {
        return new Usuario(id, nombre, correo, passwordHash, rol, creadoEn);
    }
}
