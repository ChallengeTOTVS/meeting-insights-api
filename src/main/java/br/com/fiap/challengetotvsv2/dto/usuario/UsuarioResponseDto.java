package br.com.fiap.challengetotvsv2.dto.usuario;

import br.com.fiap.challengetotvsv2.enums.Role;
import lombok.Builder;
import java.util.UUID;

@Builder
public record UsuarioResponseDto(
        UUID id,
        String nome,
        String email,
        Role role
) {}
