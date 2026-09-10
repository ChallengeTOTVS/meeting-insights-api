package br.com.fiap.challengetotvsv2.dto.reuniao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record ReuniaoRequestDto(

        @NotBlank
        String titulo,

        @NotNull
        UUID clienteId,

        @NotNull
        LocalDateTime dataReuniao

) {
}
