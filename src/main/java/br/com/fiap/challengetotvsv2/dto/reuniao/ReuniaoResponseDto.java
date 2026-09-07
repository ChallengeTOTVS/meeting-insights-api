package br.com.fiap.challengetotvsv2.dto.reuniao;

import br.com.fiap.challengetotvsv2.enums.StatusReuniao;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record ReuniaoResponseDto(
        UUID id,
        String titulo,
        LocalDateTime dataReuniao,
        String nomeArquivo,
        String transcricao,
        UUID clienteId,
        String clienteNome,
        UUID usuarioId,
        String usuarioNome,
        StatusReuniao status,
        LocalDateTime dataCriacao
) {
}
