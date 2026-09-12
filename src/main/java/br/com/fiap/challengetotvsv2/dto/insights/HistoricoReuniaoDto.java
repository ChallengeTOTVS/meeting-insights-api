package br.com.fiap.challengetotvsv2.dto.insights;

import java.time.LocalDateTime;
import java.util.UUID;

public record HistoricoReuniaoDto(
        UUID reuniaoId,
        LocalDateTime dataReuniao,
        Double scoreChurn,
        String sentimentoGeral
) {
}
