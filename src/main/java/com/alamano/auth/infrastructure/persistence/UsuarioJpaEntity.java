package com.alamano.auth.infrastructure.persistence;

import com.alamano.auth.domain.Rol;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "usuarios")
public class UsuarioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 160)
    private String correo;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected UsuarioJpaEntity() {
        // requerido por JPA
    }

    public UsuarioJpaEntity(Long id, String nombre, String correo, String passwordHash, Rol rol, Instant creadoEn) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.creadoEn = creadoEn;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
    public Instant getCreadoEn() { return creadoEn; }
}
