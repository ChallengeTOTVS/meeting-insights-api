package br.com.fiap.challengetotvsv2.security;

import br.com.fiap.challengetotvsv2.repository.IUsuarioRepository;
import br.com.fiap.challengetotvsv2.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lê o token "Bearer", confere a assinatura e autentica o usuário com o perfil
 * que está NO BANCO (não no token). Assim, trocar o perfil de alguém vale na hora.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final IUsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String email = jwtService.extrairEmail(authHeader.substring(7));
            usuarioRepository.findByEmail(email).ifPresentOrElse(usuario -> {
                var perfil = usuario.getRole() == null ? "FUNCIONARIO" : usuario.getRole().name();
                var authentication = new UsernamePasswordAuthenticationToken(
                        usuario.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_" + perfil)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }, SecurityContextHolder::clearContext);
        } catch (Exception exception) {
            // Token inválido ou expirado: segue sem autenticação e o Spring devolve 401.
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}
