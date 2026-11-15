package com.alamano.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "idToken es obligatorio")
        String idToken
) {}
