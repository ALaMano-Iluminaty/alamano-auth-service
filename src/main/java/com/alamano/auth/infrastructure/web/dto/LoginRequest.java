package com.alamano.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "correo es obligatorio")
        String correo,

        @NotBlank(message = "password es obligatorio")
        String password
) {}
