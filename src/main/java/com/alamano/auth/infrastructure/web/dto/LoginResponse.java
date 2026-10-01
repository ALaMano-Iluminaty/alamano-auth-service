package com.alamano.auth.infrastructure.web.dto;

public record LoginResponse(String token, UsuarioResponse usuario) {}
