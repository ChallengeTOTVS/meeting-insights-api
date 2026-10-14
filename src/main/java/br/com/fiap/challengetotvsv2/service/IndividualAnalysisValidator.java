package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.insights.*;
import java.util.*;
import java.text.Normalizer;
import static br.com.fiap.challengetotvsv2.dto.insights.IndividualAnalysisDto.*;

/** Reject unverifiable quotes/references without issuing another model call. */
public final class IndividualAnalysisValidator {
    private IndividualAnalysisValidator() {}
    public static void validate(AnaliseReuniaoDto result, String transcript) {
        require(result != null && result.analiseIndividual() != null, "Análise individual ausente");
        require(transcript != null && !transcript.isBlank(), "Transcrição ausente");
        String normalizedTranscript = normalize(transcript);
        var a = result.analiseIndividual();
        require(a.versao() == 2 && a.estado() != null, "Versão/estado inválido");
        Set<String> evidence = new HashSet<>();
        int position = 0;
        for (var e : list(a.evidencias())) {
            position++;
            // Report the position, never transcript/customer content in logs.
            String location = " (evidência " + position + ")";
            require(e != null, "Evidência nula" + location);
            require(e.id() != null && !e.id().isBlank(), "ID de evidência ausente" + location);
            require(evidence.add(e.id()), "ID de evidência repetido" + location);
            require(e.trecho() != null && !normalize(e.trecho()).isBlank(), "Trecho de evidência vazio" + location);
            require(normalizedTranscript.contains(normalize(e.trecho())),
                    "Trecho não encontrado na transcrição após normalização de formatação" + location);
        }
        refs(a.evidenciasVendedor(), evidence, a.vendedor() != null);
        if (a.vendedor() != null) require(contains(normalizedTranscript, a.vendedor()), "Vendedor não identificado na transcrição");
        conclusion(a.resultadoReuniao(), evidence);
        conclusion(a.situacaoCliente(), evidence);
        Set<String> interactions = new HashSet<>();
        Set<String> pending = new HashSet<>();
        for (var i : list(a.interacoes())) {
            require(i != null && i.id() != null && interactions.add(i.id()) && i.resposta() != null
                    && i.situacao() != null, "Interação inválida");
            refs(i.referencias(), evidence, true);
            require(i.situacao() != Situacao.RESOLVIDO || (i.resposta() != Resposta.NAO_RESPONDIDA
                    && i.resposta() != Resposta.PARCIAL), "Resposta incompleta não comprova resolução");
            if (i.resposta() == Resposta.COMPLETA || i.resposta() == Resposta.PARCIAL)
                require(i.respostaVendedor() != null && !i.respostaVendedor().isBlank(), "Resposta ausente");
            if (i.situacao() == Situacao.PENDENTE || i.situacao() == Situacao.ACOMPANHAMENTO) pending.add(i.id());
        }
        for (var c : list(a.compromissos())) {
            require(c != null && c.situacao() != null, "Compromisso inválido");
            refs(c.referencias(), evidence, true);
            require(c.interacaoId() == null || interactions.contains(c.interacaoId()), "Interação inexistente");
            require(c.responsavel() == null || contains(normalizedTranscript, c.responsavel()), "Responsável sem evidência");
            require(c.prazo() == null || contains(normalizedTranscript, c.prazo()), "Prazo sem evidência");
        }
        require(a.relacionamento() != null && a.relacionamento().estado() != null, "Categoria ausente");
        list(a.relacionamento().itens()).forEach(c -> conclusion(c, evidence));
        require(a.relacionamento().estado() != Estado.SEM_ACHADOS || list(a.relacionamento().itens()).isEmpty(), "Estado contraditório");
        var d = a.desempenho();
        require(d != null && d.estado() != null, "Desempenho ausente");
        require(a.vendedor() != null || d.estado() == Estado.EVIDENCIAS_INSUFICIENTES, "Vendedor não identificado");
        list(d.analise()).forEach(c -> conclusion(c, evidence));
        list(d.pontosFortes()).forEach(c -> conclusion(c, evidence));
        list(d.melhorias()).forEach(c -> conclusion(c, evidence));
        list(d.antesDaProximaReuniao()).forEach(c -> conclusion(c, evidence));
        require(pending.containsAll(list(d.pendenciasPrioritarias())), "Prioridade sem pendência correspondente");
    }
    private static boolean contains(String normalizedTranscript, String value) {
        var normalized = normalize(value);
        return !normalized.isBlank() && normalizedTranscript.contains(normalized);
    }
    static String normalize(String text) {
        // Only presentation equivalents. Preserve words, accents, case, numbers and punctuation.
        // Never use fuzzy matching, remove negations, or join non-contiguous excerpts.
        return Normalizer.normalize(text, Normalizer.Form.NFC)
                .replace('\u201c', '"').replace('\u201d', '"')
                .replace('\u2018', '\'').replace('\u2019', '\'')
                .replaceAll("[\\p{Z}\\s]+", " ").strip();
    }
    private static void conclusion(Conclusao c, Set<String> evidence) {
        require(c != null && c.texto() != null && !c.texto().isBlank(), "Conclusão ausente");
        refs(c.referencias(), evidence, !c.texto().startsWith("Não foi possível avaliar"));
    }
    private static void refs(List<String> refs, Set<String> evidence, boolean required) {
        require(refs != null && (!required || !refs.isEmpty()) && evidence.containsAll(refs), "Referência inexistente ou ausente");
    }
    private static <T> List<T> list(List<T> list) {
        require(list != null, "Lista ausente"); return list;
    }
    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalStateException(message);
    }
}
