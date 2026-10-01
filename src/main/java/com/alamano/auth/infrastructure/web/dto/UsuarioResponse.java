package com.alamano.auth.infrastructure.web.dto;

import com.alamano.auth.domain.Rol;
import com.alamano.auth.domain.Usuario;

import java.time.Instant;

public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        Rol rol,
        Instant creadoEn
) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.id(), usuario.nombre(), usuario.correo(), usuario.rol(), usuario.creadoEn());
    }
}
