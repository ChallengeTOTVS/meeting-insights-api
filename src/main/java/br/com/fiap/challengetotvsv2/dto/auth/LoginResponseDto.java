package br.com.fiap.challengetotvsv2.dto.auth;

import lombok.Builder;

@Builder
public record LoginResponseDto(
        String token,
        String tipo
) {
}
