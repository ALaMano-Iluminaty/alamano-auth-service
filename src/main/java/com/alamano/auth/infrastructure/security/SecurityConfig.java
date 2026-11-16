package com.alamano.auth.infrastructure.security;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, PublicKey jwtPublicKey) throws Exception {
        // Las rutas de identidad (google y, si se activa, register/login) son publicas.
        // La administracion de usuarios (/auth/usuarios/**) exige un JWT propio valido.
        http
                // CSRF se desactiva a proposito: la API es stateless y los tokens
                // viajan en la cabecera Authorization, no en cookies de sesion. Sonar
                // S4502 marca este patron por defecto, pero no aplica a JWT Bearer.
                .csrf(csrf -> csrf.disable()) // NOSONAR S4502
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthenticationFilter((RSAPublicKey) jwtPublicKey),
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(eh -> eh.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register", "/auth/login", "/auth/google").permitAll()
                        .requestMatchers(
                                "/actuator/health", "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/metrics", "/actuator/metrics/**",
                                "/actuator/prometheus").permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}
