package com.alamano.auth.infrastructure.web;

import com.alamano.auth.application.LoginUseCase;
import com.alamano.auth.application.RegisterUseCase;
import com.alamano.auth.domain.Usuario;
import com.alamano.auth.infrastructure.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;

    public AuthController(RegisterUseCase registerUseCase, LoginUseCase loginUseCase) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody RegisterRequest request) {
        Usuario usuario = registerUseCase.ejecutar(request.nombre(), request.correo(), request.password(), request.rol());
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(usuario));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginUseCase.Resultado resultado = loginUseCase.ejecutar(request.correo(), request.password());
        return ResponseEntity.ok(new LoginResponse(resultado.token(), UsuarioResponse.from(resultado.usuario())));
    }
}
