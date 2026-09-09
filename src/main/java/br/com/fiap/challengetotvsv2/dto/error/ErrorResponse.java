package br.com.fiap.challengetotvsv2.dto.error;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ErrorResponse(
        Integer status,
        String message,
        LocalDateTime timestamp
) {
}
