package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.insights.*;
import java.util.*;

/** Preserve supported historical findings without another model call. */
public final class HistoricalAnalysisSanitizer {
    private final Set<String> references = new HashSet<>();
    private final Set<String> seen = new HashSet<>();
    private final Set<String> missing = new LinkedHashSet<>();
    private boolean partial;
    private HistoricalAnalysisSanitizer(List<HistoricalEvidenceDto> evidence) {
        if (evidence != null) evidence.stream().filter(Objects::nonNull).forEach(e -> references.add(e.referencia()));
    }
    /** Só as referências que existem de verdade (as inventadas ou copiadas errado saem). */
    private List<String> known(List<String> refs) {
        return refs == null ? List.of() : refs.stream().filter(r -> r != null && references.contains(r)).distinct().toList();
    }
    private boolean valid(List<String> refs) {
        return !known(refs).isEmpty();
    }
    private static boolean comparesPeriods(List<String> refs) {
        return refs.contains("ATUAL") && refs.stream().anyMatch(r -> !r.equals("ATUAL"));
    }
    /*
     * Antes, UMA referência inválida descartava a conclusão inteira. Agora a conclusão fica se
     * sobrar pelo menos uma referência real; as inválidas são removidas. Continua valendo: tudo
     * que aparece na tela aponta para um trecho que existe, e comparação exige atual + anterior.
     */
    private HistoricalFindingDto finding(HistoricalFindingDto f, String category, boolean comparison) {
        if (f == null || f.descricao() == null || f.descricao().isBlank()) {
            missing.add(category); partial = true; return null;
        }
        var refs = known(f.referencias());
        if (refs.isEmpty() || (comparison && !comparesPeriods(refs))) {
            missing.add(category); partial = true; return null;
        }
        return refs.equals(f.referencias()) ? f
                : new HistoricalFindingDto(f.titulo(), f.descricao(), refs, f.responsavel(), f.prazo(), f.situacao());
    }
    private List<HistoricalFindingDto> group(List<HistoricalFindingDto> items, String category) {
        if (items == null) { missing.add(category); partial = true; return List.of(); }
        var kept = new ArrayList<HistoricalFindingDto>();
        for (var item : items) {
            var f = finding(item, category, false);
            if (f == null) continue;
            String key = IndividualAnalysisValidator.normalize(f.descricao()).toLowerCase(Locale.ROOT);
            if (seen.add(key)) kept.add(f);
            else partial = true;
        }
        return kept;
    }
    public static HistoricalAnalysisDto sanitize(HistoricalAnalysisDto a, List<HistoricalEvidenceDto> evidence) {
        if (a == null) throw new IllegalStateException("A IA não retornou uma análise histórica.");
        var s = new HistoricalAnalysisSanitizer(evidence);
        if (a.categoriasSemEvidencia() != null) a.categoriasSemEvidencia().stream().filter(Objects::nonNull).forEach(s.missing::add);
        var objections = s.group(a.objecoesRecorrentes(), "objecoesRecorrentes");
        var unresolved = s.group(a.assuntosNaoResolvidos(), "assuntosNaoResolvidos");
        var commitments = s.group(a.compromissos(), "compromissos");
        var competitors = s.group(a.concorrentes(), "concorrentes");
        var priorities = s.group(a.prioridadesCliente(), "prioridadesCliente");
        var questions = s.group(a.perguntasRecorrentes(), "perguntasRecorrentes");
        var satisfaction = s.group(a.satisfacaoCliente(), "satisfacaoCliente");
        var sentiment = s.finding(a.evolucaoSentimento(), "evolucaoSentimento", true);
        var churn = s.finding(a.evolucaoRiscoChurn(), "evolucaoRiscoChurn", true);
        var productivity = s.finding(a.produtividadeReuniao(), "produtividadeReuniao", false);
        var seller = a.qualidadeVendedor();
        if (seller != null) {
            var refs = s.known(seller.referencias());
            // Basta uma referência real: a justificativa já compara as reuniões no texto, e
            // descartar tudo por faltar a citação de um dos períodos deixava o card vazio.
            if (refs.isEmpty()) {
                seller = null; s.missing.add("qualidadeVendedor"); s.partial = true;
            } else if (!refs.equals(seller.referencias())) {
                seller = new AvaliacaoVendedorDto(seller.qualidadeAtual(), seller.qualidadeAnterior(), seller.variacao(),
                        seller.justificativa(), seller.melhorias(), seller.pontosAtencao(), refs);
            }
        }
        String notes = s.partial ? "Análise histórica salva parcialmente. Itens sem referências válidas e duplicatas foram omitidos; os demais foram preservados. Comparações exigem evidências da reunião atual e de reuniões anteriores." : a.observacoes();
        return new HistoricalAnalysisDto(objections, sentiment, churn, unresolved, productivity, seller,
                commitments, competitors, priorities, questions, notes, 2, s.partial ? null : a.resumoEvolucao(), satisfaction, new ArrayList<>(s.missing)).withProducts(a.produtosExperiencias());
    }
}
