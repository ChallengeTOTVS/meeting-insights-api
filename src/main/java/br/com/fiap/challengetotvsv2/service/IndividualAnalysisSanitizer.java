package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.insights.*;
import java.util.*;
import static br.com.fiap.challengetotvsv2.dto.insights.IndividualAnalysisDto.*;

/** Keep usable output without inventing missing evidence or paying for a repair call. */
public final class IndividualAnalysisSanitizer {
    private IndividualAnalysisSanitizer() {}
    private static <T> List<T> list(List<T> items) {
        return items == null ? List.of() : items.stream().filter(Objects::nonNull).toList();
    }
    private static boolean present(String s) { return s != null && !s.isBlank(); }
    private static boolean found(String transcript, String text) {
        return present(text) && transcript.contains(IndividualAnalysisValidator.normalize(text));
    }
    private static List<String> refs(List<String> ids, Map<String, Evidencia> evidence) {
        return list(ids).stream().filter(evidence::containsKey).distinct().toList();
    }
    private static Conclusao claim(Conclusao c, Map<String, Evidencia> evidence) {
        if (c == null || !present(c.texto())) return null;
        var ids = refs(c.referencias(), evidence);
        return ids.isEmpty() ? null : new Conclusao(c.texto(), ids);
    }
    private static Conclusao conclusion(Conclusao c, Map<String, Evidencia> evidence) {
        var valid = claim(c, evidence);
        return valid != null ? valid : new Conclusao("Não foi possível avaliar com as evidências disponíveis.", List.of());
    }
    private static List<Conclusao> claims(List<Conclusao> items, Map<String, Evidencia> evidence) {
        return list(items).stream().map(c -> claim(c, evidence)).filter(Objects::nonNull).toList();
    }

    private static FatosCompartilhaveis sanitizeSharedFacts(FatosCompartilhaveis facts,
            Map<String, Evidencia> evidence) {
        if (facts == null) return new FatosCompartilhaveis(Estado.EVIDENCIAS_INSUFICIENTES, List.of());
        var items = list(facts.itens()).stream().filter(item -> item.tipo() != null
                && item.situacao() != null && present(item.assunto()) && present(item.descricao())
                && item.descricao().length() <= 360)
                .map(item -> new FatoCompartilhavel(item.tipo(), item.assunto().strip(),
                        item.descricao().strip(), item.situacao(), refs(item.referencias(), evidence)))
                .filter(item -> !item.referencias().isEmpty())
                .distinct().toList();
        var state = !items.isEmpty() ? Estado.CONCLUIDO
                : facts.estado() == Estado.SEM_ACHADOS && list(facts.itens()).isEmpty()
                    ? Estado.SEM_ACHADOS : Estado.EVIDENCIAS_INSUFICIENTES;
        return new FatosCompartilhaveis(state, items);
    }

    public static AnaliseReuniaoDto sanitize(AnaliseReuniaoDto result, String transcript) {
        if (result == null) throw new IllegalStateException("A IA não retornou uma análise.");
        String text = IndividualAnalysisValidator.normalize(transcript == null ? "" : transcript);
        var a = result.analiseIndividual();
        var evidence = new LinkedHashMap<String, Evidencia>();
        if (a != null) for (var e : list(a.evidencias())) {
            if (present(e.id()) && found(text, e.trecho())) evidence.putIfAbsent(e.id(), e);
        }
        var sellerRefs = a == null ? List.<String>of() : refs(a.evidenciasVendedor(), evidence);
        String seller = a == null ? null : a.vendedor();
        if (!found(text, seller) || sellerRefs.isEmpty()) seller = null;
        if (seller != null) {
            final String name = seller;
            if (sellerRefs.stream().noneMatch(id -> found(IndividualAnalysisValidator.normalize(evidence.get(id).trecho()), name))) seller = null;
        }
        if (seller == null) sellerRefs = List.of();
        var interactions = new ArrayList<Interacao>();
        var ids = new HashSet<String>();
        if (a != null) for (var i : list(a.interacoes())) {
            var references = refs(i.referencias(), evidence);
            if (!present(i.id()) || !present(i.assuntoOuPergunta()) || references.isEmpty() || !ids.add(i.id())) continue;
            var response = i.resposta() == null ? Resposta.NAO_FOI_POSSIVEL_AVALIAR : i.resposta();
            if (!present(i.respostaVendedor()) && (response == Resposta.COMPLETA || response == Resposta.PARCIAL)) response = Resposta.NAO_FOI_POSSIVEL_AVALIAR;
            var status = i.situacao() == null ? Situacao.NAO_FOI_POSSIVEL_AVALIAR : i.situacao();
            if (status == Situacao.RESOLVIDO && response != Resposta.COMPLETA && response != Resposta.NAO_SE_APLICA) status = Situacao.NAO_FOI_POSSIVEL_AVALIAR;
            interactions.add(new Interacao(i.id(), i.assuntoOuPergunta(), i.respostaVendedor(), response, status, i.tratamentoObjecao(), references));
        }
        var commitments = new ArrayList<Compromisso>();
        if (a != null) for (var c : list(a.compromissos())) {
            var references = refs(c.referencias(), evidence);
            if (!present(c.descricao()) || references.isEmpty()) continue;
            commitments.add(new Compromisso(c.descricao(), found(text, c.responsavel()) ? c.responsavel() : null,
                    found(text, c.prazo()) ? c.prazo() : null, c.situacao() == null ? Situacao.NAO_FOI_POSSIVEL_AVALIAR : c.situacao(),
                    ids.contains(c.interacaoId()) ? c.interacaoId() : null, references));
        }
        var relationship = a == null ? null : a.relacionamento();
        var relationshipItems = relationship == null ? List.<Conclusao>of() : claims(relationship.itens(), evidence);
        var category = new Categoria(relationshipItems.isEmpty() ?
                (relationship != null && relationship.estado() == Estado.SEM_ACHADOS && relationship.itens() != null && relationship.itens().isEmpty() ? Estado.SEM_ACHADOS : Estado.EVIDENCIAS_INSUFICIENTES) : Estado.CONCLUIDO, relationshipItems);
        var d = a == null ? null : a.desempenho();
        var performance = new Desempenho(Estado.EVIDENCIAS_INSUFICIENTES, List.of(), List.of(), List.of(), List.of(), List.of());
        if (seller != null && d != null) {
            var analysis = claims(d.analise(), evidence);
            var pending = interactions.stream().filter(i -> i.situacao() == Situacao.PENDENTE || i.situacao() == Situacao.ACOMPANHAMENTO).map(Interacao::id).toList();
            performance = new Desempenho(analysis.isEmpty() || d.estado() == Estado.EVIDENCIAS_INSUFICIENTES ? Estado.EVIDENCIAS_INSUFICIENTES : Estado.CONCLUIDO,
                    analysis, claims(d.pontosFortes(), evidence), claims(d.melhorias(), evidence), claims(d.antesDaProximaReuniao(), evidence), list(d.pendenciasPrioritarias()).stream().filter(pending::contains).distinct().toList());
        }
        var candidate = new IndividualAnalysisDto(2, a == null || a.estado() == null ? Estado.EVIDENCIAS_INSUFICIENTES : a.estado(), seller, sellerRefs,
                conclusion(a == null ? null : a.resultadoReuniao(), evidence), conclusion(a == null ? null : a.situacaoCliente(), evidence),
                interactions, commitments, category, performance, new ArrayList<>(evidence.values()));
        if (!candidate.withProducts(a == null ? null : a.produtosExperiencias()).equals(a)) candidate = new IndividualAnalysisDto(2, Estado.EVIDENCIAS_INSUFICIENTES, seller, sellerRefs,
                candidate.resultadoReuniao(), candidate.situacaoCliente(), interactions, commitments, category, performance, candidate.evidencias());
        candidate = candidate.withProducts(a == null ? null : a.produtosExperiencias());
        candidate = candidate.withFatosCompartilhaveis(sanitizeSharedFacts(a == null ? null : a.fatosCompartilhaveis(), evidence));
        return new AnaliseReuniaoDto(result.scoreChurn(), result.nivelRisco(), result.justificativaRisco(), result.sentimentoGeral(),
                result.resumo(), result.topicosPrincipais(), result.objecoes(), result.acoesRecomendadas(), candidate);
    }
}
