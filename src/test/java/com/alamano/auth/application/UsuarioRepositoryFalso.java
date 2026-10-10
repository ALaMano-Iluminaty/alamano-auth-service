package com.alamano.auth.application;

import com.alamano.auth.domain.Usuario;
import com.alamano.auth.domain.UsuarioRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositorio en memoria para las pruebas de los casos de uso. Se escribe a mano
 * en vez de usar un mock para que las pruebas comprueben comportamiento real:
 * guardar de verdad y luego buscar, en lugar de verificar llamadas.
 */
class UsuarioRepositoryFalso implements UsuarioRepository {
    private final Map<String, Usuario> porCorreo = new LinkedHashMap<>();
    final List<Usuario> guardados = new ArrayList<>();
    private long siguienteId = 1;

    @Override
    public boolean existsByCorreo(String correo) {
        return porCorreo.containsKey(correo);
    }

    @Override
    public Usuario save(Usuario usuario) {
        Usuario conId = usuario.id() == null ? usuario.conId(siguienteId++) : usuario;
        porCorreo.put(conId.correo(), conId);
        guardados.add(conId);
        return conId;
    }

    @Override
    public Optional<Usuario> findByCorreo(String correo) {
        return Optional.ofNullable(porCorreo.get(correo));
    }
}
