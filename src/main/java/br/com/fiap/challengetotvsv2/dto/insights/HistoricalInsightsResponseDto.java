package br.com.fiap.challengetotvsv2.dto.insights;

import java.util.List;
import java.util.UUID;

public record HistoricalInsightsResponseDto(
        UUID reuniaoId,
        UUID clienteId,
        boolean historicoDisponivel,
        String mensagem,
        List<HistoricoReuniaoDto> evolucaoScores,
        HistoricalAnalysisDto analise,
        List<HistoricalEvidenceDto> evidencias
) {
}
