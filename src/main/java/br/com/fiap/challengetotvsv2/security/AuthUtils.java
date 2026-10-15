package br.com.fiap.challengetotvsv2.security;

import br.com.fiap.challengetotvsv2.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Atalhos para consultar quem está autenticado na requisição atual. */
public final class AuthUtils {

    private AuthUtils() {}

    public static String emailAtual() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public static boolean temPerfil(Role role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String esperado = "ROLE_" + role.name();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> esperado.equals(a.getAuthority()));
    }

    public static boolean isAdmin() {
        return temPerfil(Role.ADMIN);
    }
}
