package br.com.fiap.challengetotvsv2.dto.usuario;

import br.com.fiap.challengetotvsv2.enums.Role;
import jakarta.validation.constraints.NotNull;

public record AlterarPerfilDto(@NotNull Role role) {
}
