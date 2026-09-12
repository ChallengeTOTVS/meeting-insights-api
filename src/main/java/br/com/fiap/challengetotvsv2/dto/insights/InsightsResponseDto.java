package br.com.fiap.challengetotvsv2.dto.insights;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record InsightsResponseDto(
        UUID id,

        UUID reuniaoId,

        Double scoreChurn,

        String nivelRisco,

        String justificativaRisco,

        String sentimentoGeral,

        String resumo,

        String topicosPrincipais,

        String objecoes,

        String acoesRecomendadas,

        LocalDateTime dataCriacao,
        IndividualAnalysisDto analiseIndividual,
        String estadoAvaliacao
) {

}
