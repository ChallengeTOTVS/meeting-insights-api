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
        List<String> categoriasSemEvidencia,
        ProductsAnalysisDto produtosExperiencias
) {
    public HistoricalAnalysisDto withProducts(ProductsAnalysisDto products) {
        return new HistoricalAnalysisDto(objecoesRecorrentes, evolucaoSentimento, evolucaoRiscoChurn,
                assuntosNaoResolvidos, produtividadeReuniao, qualidadeVendedor, compromissos, concorrentes,
                prioridadesCliente, perguntasRecorrentes, observacoes, versao, resumoEvolucao,
                satisfacaoCliente, categoriasSemEvidencia, products);
    }
    public HistoricalAnalysisDto(List<HistoricalFindingDto> objecoesRecorrentes,
            HistoricalFindingDto evolucaoSentimento, HistoricalFindingDto evolucaoRiscoChurn,
            List<HistoricalFindingDto> assuntosNaoResolvidos, HistoricalFindingDto produtividadeReuniao,
            AvaliacaoVendedorDto qualidadeVendedor, List<HistoricalFindingDto> compromissos,
            List<HistoricalFindingDto> concorrentes, List<HistoricalFindingDto> prioridadesCliente,
            List<HistoricalFindingDto> perguntasRecorrentes, String observacoes, Integer versao,
            String resumoEvolucao, List<HistoricalFindingDto> satisfacaoCliente, List<String> categoriasSemEvidencia) {
        this(objecoesRecorrentes, evolucaoSentimento, evolucaoRiscoChurn, assuntosNaoResolvidos,
                produtividadeReuniao, qualidadeVendedor, compromissos, concorrentes, prioridadesCliente,
                perguntasRecorrentes, observacoes, versao, resumoEvolucao, satisfacaoCliente, categoriasSemEvidencia, null);
    }
}
