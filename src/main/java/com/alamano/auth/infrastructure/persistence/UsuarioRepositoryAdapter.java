package com.alamano.auth.infrastructure.persistence;

import com.alamano.auth.domain.Usuario;
import com.alamano.auth.domain.UsuarioRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final SpringDataUsuarioRepository springDataRepository;

    public UsuarioRepositoryAdapter(SpringDataUsuarioRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public boolean existsByCorreo(String correo) {
        return springDataRepository.existsByCorreo(correo);
    }

    @Override
    public Usuario save(Usuario usuario) {
        UsuarioJpaEntity entity = new UsuarioJpaEntity(
                usuario.id(),
                usuario.nombre(),
                usuario.correo(),
                usuario.passwordHash(),
                usuario.rol(),
                usuario.creadoEn()
        );
        UsuarioJpaEntity guardado = springDataRepository.save(entity);
        return toDomain(guardado);
    }

    @Override
    public Optional<Usuario> findByCorreo(String correo) {
        return springDataRepository.findByCorreo(correo).map(this::toDomain);
    }

    private Usuario toDomain(UsuarioJpaEntity entity) {
        return new Usuario(
                entity.getId(),
                entity.getNombre(),
                entity.getCorreo(),
                entity.getPasswordHash(),
                entity.getRol(),
                entity.getCreadoEn()
        );
    }
}
