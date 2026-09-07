package br.com.fiap.challengetotvsv2.dto.reuniao;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record ReuniaoRequestDto(

        @NotBlank
        String titulo,

        String transcricao,

        @NotBlank
        UUID clienteId,

        @NotBlank
        LocalDateTime dataReuniao

) {
}
