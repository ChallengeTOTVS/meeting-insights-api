package br.com.fiap.challengetotvsv2.dto.insights;

import java.time.LocalDateTime;
import java.util.UUID;

public record HistoricalEvidenceDto(
        String referencia,
        UUID reuniaoId,
        LocalDateTime dataReuniao,
        String tituloReuniao,
        String trecho
) {
}
