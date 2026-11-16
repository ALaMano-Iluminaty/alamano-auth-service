package com.alamano.auth.infrastructure.web;

import com.alamano.auth.application.LoginUseCase;
import com.alamano.auth.application.RegisterUseCase;
import com.alamano.auth.domain.Usuario;
import com.alamano.auth.infrastructure.web.dto.LoginRequest;
import com.alamano.auth.infrastructure.web.dto.LoginResponse;
import com.alamano.auth.infrastructure.web.dto.RegisterRequest;
import com.alamano.auth.infrastructure.web.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro y login con correo y contrasena. Apagado por defecto: la politica del
 * proyecto es no tener logins caseros y delegar la identidad en Google. Si
 * LOCAL_LOGIN_ENABLED no es "true", estas rutas no existen (404).
 */
@RestController
@RequestMapping("/auth")
@ConditionalOnProperty(prefix = "app.auth", name = "local-login-enabled", havingValue = "true")
public class AuthLocalController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;

    public AuthLocalController(RegisterUseCase registerUseCase, LoginUseCase loginUseCase) {
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
