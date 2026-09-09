package br.com.fiap.challengetotvsv2.dto.insights;

public record AnaliseReuniaoDto(
        Double scoreChurn,
        String nivelRisco,
        String justificativaRisco,
        String sentimentoGeral,
        String resumo,
        String topicosPrincipais,
        String objecoes,
        String acoesRecomendadas
) {
}
