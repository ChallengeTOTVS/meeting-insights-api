package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.insights.*;
import java.util.*;

public final class HistoricalAnalysisValidator {
    private HistoricalAnalysisValidator() {}
    public static void validate(HistoricalAnalysisDto a, List<HistoricalEvidenceDto> evidence) {
        if (a == null || !Integer.valueOf(2).equals(a.versao()) || a.categoriasSemEvidencia() == null)
            throw new IllegalStateException("Análise histórica incompleta");
        Set<String> refs = new HashSet<>();
        evidence.forEach(e -> refs.add(e.referencia()));
        Set<String> seen = new HashSet<>();
        for (var group : List.of(a.objecoesRecorrentes(), a.assuntosNaoResolvidos(), a.compromissos(),
                a.concorrentes(), a.prioridadesCliente(), a.perguntasRecorrentes(), a.satisfacaoCliente())) {
            for (var f : group) {
                finding(f, refs);
                String key = f.descricao().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
                if (!seen.add(key)) throw new IllegalStateException("Achado duplicado entre categorias");
            }
        }
        comparison(a.evolucaoSentimento(), refs);
        comparison(a.evolucaoRiscoChurn(), refs);
        finding(a.produtividadeReuniao(), refs);
        if (a.qualidadeVendedor() != null) references(a.qualidadeVendedor().referencias(), refs);
    }
    private static void comparison(HistoricalFindingDto finding, Set<String> refs) {
        finding(finding, refs);
        if (finding != null && !finding.referencias().isEmpty()
                && (!finding.referencias().contains("ATUAL") || finding.referencias().stream().noneMatch(r -> !r.equals("ATUAL"))))
            throw new IllegalStateException("Mudança sem evidências das duas épocas");
    }
    private static void finding(HistoricalFindingDto f, Set<String> refs) {
        if (f == null) return;
        references(f.referencias(), refs);
        if (f.referencias().isEmpty() && !f.descricao().startsWith("Não foi possível avaliar"))
            throw new IllegalStateException("Conclusão sem evidência");
    }
    private static void references(List<String> values, Set<String> refs) {
        if (values == null || !refs.containsAll(values)) throw new IllegalStateException("Referência histórica inexistente");
    }
}
