package com.alamano.auth.domain;

import java.time.Instant;

/**
 * Objeto de dominio puro: no conoce JPA, Spring ni ninguna otra tecnologia.
 * HU1: "Como usuario, quiero registrarme e iniciar sesion con correo y
 * contrasena, para acceder a la app".
 * HU-Q2: una cuenta creada con Google no tiene contrasena (passwordHash nulo).
 */
public record Usuario(
        Long id,
        String nombre,
        String correo,
        String passwordHash,
        Rol rol,
        Instant creadoEn,
        String googleSub
) {

    public static Usuario nuevo(String nombre, String correo, String passwordHash, Rol rol) {
        return new Usuario(null, nombre, correo.toLowerCase(), passwordHash, rol, Instant.now(), null);
    }

    public static Usuario deGoogle(String nombre, String correo, String googleSub) {
        return new Usuario(null, nombre, correo.toLowerCase(), null, Rol.USUARIO, Instant.now(), googleSub);
    }

    public Usuario conId(Long id) {
        return new Usuario(id, nombre, correo, passwordHash, rol, creadoEn, googleSub);
    }

    public Usuario vinculadoAGoogle(String googleSub) {
        return new Usuario(id, nombre, correo, passwordHash, rol, creadoEn, googleSub);
    }

    public boolean tieneContrasena() {
        return passwordHash != null;
    }
}
