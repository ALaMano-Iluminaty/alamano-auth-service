package com.alamano.auth.infrastructure.web;

import com.alamano.auth.application.GoogleLoginUseCase;
import com.alamano.auth.application.LoginUseCase;
import com.alamano.auth.infrastructure.web.dto.GoogleLoginRequest;
import com.alamano.auth.infrastructure.web.dto.LoginResponse;
import com.alamano.auth.infrastructure.web.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Unica puerta de entrada de identidad: el proveedor externo (Google). */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final GoogleLoginUseCase googleLoginUseCase;

    public AuthController(GoogleLoginUseCase googleLoginUseCase) {
        this.googleLoginUseCase = googleLoginUseCase;
    }

    /** HU-Q2: el frontend manda aqui el id_token que le entrega Google Identity Services. */
    @PostMapping("/google")
    public ResponseEntity<LoginResponse> google(@Valid @RequestBody GoogleLoginRequest request) {
        LoginUseCase.Resultado resultado = googleLoginUseCase.ejecutar(request.idToken());
        return ResponseEntity.ok(new LoginResponse(resultado.token(), UsuarioResponse.from(resultado.usuario())));
    }
}
