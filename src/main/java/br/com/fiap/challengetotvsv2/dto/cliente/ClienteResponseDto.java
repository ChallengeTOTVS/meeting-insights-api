package br.com.fiap.challengetotvsv2.dto.cliente;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ClienteResponseDto(
        UUID id,
        String nome,
        String empresa,
        String email,
        String segmento
) { }
