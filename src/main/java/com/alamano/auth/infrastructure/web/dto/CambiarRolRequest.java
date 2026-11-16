package com.alamano.auth.infrastructure.web.dto;

import com.alamano.auth.domain.Rol;
import jakarta.validation.constraints.NotNull;

public record CambiarRolRequest(
        @NotNull(message = "rol es obligatorio")
        Rol rol
) {}
