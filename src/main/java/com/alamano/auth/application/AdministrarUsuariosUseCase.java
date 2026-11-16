package com.alamano.auth.application;

import com.alamano.auth.application.exception.AccesoDenegadoException;
import com.alamano.auth.application.exception.OperacionNoPermitidaException;
import com.alamano.auth.application.exception.UsuarioNoEncontradoException;
import com.alamano.auth.domain.Rol;
import com.alamano.auth.domain.Usuario;
import com.alamano.auth.domain.UsuarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * HU-Q3: administracion de roles. La regla "solo un ADMIN puede hacerlo" vive aqui,
 * en la capa de aplicacion, y no en el controlador: asi se cumple sin importar por
 * que puerta de entrada llegue la peticion.
 *
 * El rol del que llama se lee de la base de datos y no del token: si a un
 * administrador le quitan el rol, pierde el acceso al instante y no cuando venza su JWT.
 */
@Service
public class AdministrarUsuariosUseCase {

    private final UsuarioRepository usuarioRepository;

    public AdministrarUsuariosUseCase(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<Usuario> listar(Long actorId) {
        exigirAdmin(actorId);
        return usuarioRepository.findAll();
    }

    public Usuario cambiarRol(Long actorId, Long usuarioId, Rol nuevoRol) {
        exigirAdmin(actorId);
        // Un administrador no se cambia su propio rol: asi el sistema nunca se queda sin administradores.
        if (actorId.equals(usuarioId)) {
            throw new OperacionNoPermitidaException("Un administrador no puede cambiar su propio rol");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNoEncontradoException(usuarioId));
        if (usuario.rol() == nuevoRol) {
            return usuario;
        }
        return usuarioRepository.save(usuario.conRol(nuevoRol));
    }

    private void exigirAdmin(Long actorId) {
        Usuario actor = usuarioRepository.findById(actorId)
                .orElseThrow(() -> new AccesoDenegadoException("La cuenta ya no existe"));
        if (actor.rol() != Rol.ADMIN) {
            throw new AccesoDenegadoException("Se requiere el rol ADMIN");
        }
    }
}
