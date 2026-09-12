package br.com.fiap.challengetotvsv2.dto.insights;

import java.util.List;

public record HistoricalAnalysisDto(
        List<HistoricalFindingDto> objecoesRecorrentes,
        HistoricalFindingDto evolucaoSentimento,
        HistoricalFindingDto evolucaoRiscoChurn,
        List<HistoricalFindingDto> assuntosNaoResolvidos,
        HistoricalFindingDto produtividadeReuniao,
        AvaliacaoVendedorDto qualidadeVendedor,
        List<HistoricalFindingDto> compromissos,
        List<HistoricalFindingDto> concorrentes,
        List<HistoricalFindingDto> prioridadesCliente,
        List<HistoricalFindingDto> perguntasRecorrentes,
        String observacoes,
        Integer versao,
        String resumoEvolucao,
        List<HistoricalFindingDto> satisfacaoCliente,
        List<String> categoriasSemEvidencia
) {
}
