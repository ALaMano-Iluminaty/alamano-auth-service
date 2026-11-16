package com.alamano.auth.infrastructure.web;

import com.alamano.auth.application.AdministrarUsuariosUseCase;
import com.alamano.auth.infrastructure.web.dto.CambiarRolRequest;
import com.alamano.auth.infrastructure.web.dto.UsuarioResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU-Q3: el administrador lista las cuentas y cambia su rol. */
@RestController
@RequestMapping("/auth/usuarios")
public class UsuarioAdminController {

    private final AdministrarUsuariosUseCase administrarUsuarios;

    public UsuarioAdminController(AdministrarUsuariosUseCase administrarUsuarios) {
        this.administrarUsuarios = administrarUsuarios;
    }

    @GetMapping
    public List<UsuarioResponse> listar(Authentication authentication) {
        return administrarUsuarios.listar(actorId(authentication)).stream().map(UsuarioResponse::from).toList();
    }

    @PatchMapping("/{id}/rol")
    public UsuarioResponse cambiarRol(@PathVariable Long id, @Valid @RequestBody CambiarRolRequest request,
                                      Authentication authentication) {
        return UsuarioResponse.from(administrarUsuarios.cambiarRol(actorId(authentication), id, request.rol()));
    }

    /** El filtro JWT deja el id de la cuenta (el "sub" del token) como nombre de la autenticacion. */
    private static Long actorId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
