package br.com.fiap.challengetotvsv2.config;

import br.com.fiap.challengetotvsv2.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Quem acessa o quê:
 *  - /auth/**            → público (login).
 *  - /assistente/**      → todos os perfis logados (o chatbot é a "porta" das outras áreas).
 *  - /usuarios/me        → todos os perfis logados.
 *  - /usuarios/**, /gestao/** → só ADMIN (cadastro de usuários é fechado).
 *  - todo o resto        → ADMIN ou FUNCIONARIO. O perfil CONSULTA não vê as telas do sistema.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Encaminhamentos internos para /error não podem exigir login de novo:
                        // sem isso, qualquer erro 500 virava 401 e o front deslogava o usuário.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/assistente/**", "/usuarios/me").authenticated()
                        .requestMatchers("/usuarios/**", "/gestao/**").hasRole("ADMIN")
                        .anyRequest().hasAnyRole("ADMIN", "FUNCIONARIO")
                )
                // Sem token (ou token inválido) → 401; autenticado sem permissão → 403.
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
