package br.com.fiap.challengetotvsv2.dto.usuario;

import br.com.fiap.challengetotvsv2.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UsuarioRequestDto(
        String nome,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, message = "A senha precisa ter pelo menos 8 caracteres")
        String senha,

        // Opcional: quando ausente, o usuário é criado como FUNCIONARIO (vendedor).
        Role role
) {
}
