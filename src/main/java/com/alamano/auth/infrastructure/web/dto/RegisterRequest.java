package com.alamano.auth.infrastructure.web.dto;

import com.alamano.auth.domain.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "nombre es obligatorio")
        String nombre,

        @NotBlank(message = "correo es obligatorio")
        @Email(message = "el correo no tiene un formato valido")
        String correo,

        @NotBlank(message = "password es obligatorio")
        @Size(min = 6, message = "la contrasena debe tener al menos 6 caracteres")
        String password,

        Rol rol
) {}
