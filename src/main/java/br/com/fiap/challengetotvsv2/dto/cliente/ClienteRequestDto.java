package br.com.fiap.challengetotvsv2.dto.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ClienteRequestDto (
        @NotBlank
        String nome,

        @NotBlank
        String empresa,

        @NotBlank
        @Email
        String email,

        String segmento
){}
