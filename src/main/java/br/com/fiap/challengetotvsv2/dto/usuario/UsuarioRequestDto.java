package br.com.fiap.challengetotvsv2.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UsuarioRequestDto(
        String nome,

        @NotBlank
        @Email
        String email,

        @NotBlank
        String senha

) {
}
