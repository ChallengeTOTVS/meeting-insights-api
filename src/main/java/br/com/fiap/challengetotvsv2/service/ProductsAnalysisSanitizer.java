package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.insights.ProductsAnalysisDto;
import br.com.fiap.challengetotvsv2.dto.insights.ProductsAnalysisDto.*;
import java.util.*;

/** Validate literal evidence and catalog identity locally; never call AI to repair output. */
public final class ProductsAnalysisSanitizer {
    private ProductsAnalysisSanitizer() {}
    public record Source(String text, String date, String meetingId) {}
    private static <T> List<T> list(List<T> items) {
        return items == null ? List.of() : items.stream().filter(Objects::nonNull).toList();
    }
    private static boolean present(String s) { return s != null && !s.isBlank(); }
    private static List<String> refs(List<String> ids, Map<String, Evidencia> evidence) {
        return list(ids).stream().filter(evidence::containsKey).distinct().toList();
    }
    // Mantém o achado se sobrar ao menos uma evidência válida; referências inválidas são removidas.
    private static Achado claim(Achado c, Map<String, Evidencia> evidence) {
        if (c == null || !present(c.texto())) return null;
        var valid = refs(c.referencias(), evidence);
        return valid.isEmpty() ? null : new Achado(c.texto(), valid);
    }
    private static List<Achado> claims(List<Achado> items, Map<String, Evidencia> evidence) {
        return list(items).stream().map(c -> claim(c, evidence)).filter(Objects::nonNull).toList();
    }
    /*
     * O trecho fala deste produto?
     * Sim, se cita um apelido do produto ("ERP Sankhya"). Também sim se cita só o fornecedor
     * ("o Sankhya mandou proposta") E o produto foi nomeado explicitamente em alguma fonte da
     * mesma análise. Antes, só o nome completo valia, e falas naturais como "o Omie travou" eram
     * descartadas, deixando Produtos e Concorrentes vazios. Sem o produto nomeado em lugar
     * nenhum, "SAP" sozinho continua NÃO virando "SAP Business One" (regra do catálogo).
     */
    static boolean refersTo(Entrada entry, String quotes, String allText, Catalogo catalog) {
        if (entry.apelidos().stream().anyMatch(alias -> CommercialCatalogService.mentions(quotes, alias))) return true;
        if (!present(entry.fornecedor()) || !CommercialCatalogService.mentions(quotes, entry.fornecedor())) return false;
        // Só vale se este for o ÚNICO produto daquele fornecedor nomeado na conversa
        // (ex.: "a TOTVS" é ambíguo quando Winthor e Fluig aparecem).
        var namedFromSupplier = catalog.entradas().stream()
                .filter(e -> entry.fornecedor().equals(e.fornecedor()) && !"Produto não especificado".equals(e.nome()))
                .filter(e -> e.apelidos().stream().anyMatch(alias -> CommercialCatalogService.mentions(allText, alias)))
                .map(Entrada::id).toList();
        return namedFromSupplier.equals(List.of(entry.id()));
    }

    public static ProductsAnalysisDto sanitize(ProductsAnalysisDto a, Catalogo catalog, Map<String, Source> sources) {
        if (a == null) return new ProductsAnalysisDto(Estado.EVIDENCIAS_INSUFICIENTES, catalog, List.of(), List.of(), List.of());
        var evidence = new LinkedHashMap<String, Evidencia>();
        for (var e : list(a.evidencias())) {
            var src = sources.get(e.referencia());
            if (src == null || !present(e.id()) || !present(e.trecho())
                    || !IndividualAnalysisValidator.normalize(src.text()).contains(IndividualAnalysisValidator.normalize(e.trecho()))) continue;
            // Quem falou vem da transcrição (rótulo "Nome:" da linha), não do palpite da IA.
            String speaker = TranscriptSpeakers.speakerOf(src.text(), e.trecho());
            String participant = speaker != null ? speaker
                    : present(e.participante()) && e.trecho().contains(e.participante()) ? e.participante() : null;
            evidence.putIfAbsent(e.id(), new Evidencia(e.id(), e.referencia(), e.trecho(), participant, src.date()));
        }
        // Texto de todas as fontes: serve para saber se o produto foi nomeado em algum momento.
        String allText = sources.values().stream().map(Source::text).filter(Objects::nonNull)
                .reduce("", (x, y) -> x + "\n" + y);
        var products = new ArrayList<Produto>();
        boolean partial = false;
        for (var p : list(a.produtos())) {
            var observations = new ArrayList<Observacao>();
            var entry = catalog.entradas().stream().filter(e -> e.id().equals(p.catalogoId())).findFirst().orElse(null);
            for (var o : list(p.observacoes())) {
                var context = claim(o.contexto(), evidence);
                if (context == null) continue;
                // As evidências do contexto também sustentam a observação.
                var ids = new ArrayList<>(refs(o.referencias(), evidence));
                context.referencias().stream().filter(id -> !ids.contains(id)).forEach(ids::add);
                String quotes = ids.stream().map(id -> evidence.get(id).trecho()).reduce("", (x, y) -> x + "\n" + y);
                boolean mentioned = entry != null
                        ? refersTo(entry, quotes, allText, catalog)
                        : ("Produto não especificado".equals(p.nome())
                            ? CommercialCatalogService.mentions(quotes, p.fornecedor())
                            : CommercialCatalogService.mentions(quotes, p.nome()));
                if (!mentioned || (entry == null && present(p.fornecedor())
                        && !CommercialCatalogService.mentions(quotes, p.fornecedor()))) continue;
                var speakers = ids.stream().map(id -> evidence.get(id).participante()).filter(Objects::nonNull).distinct().toList();
                String participant = present(o.participante())
                        ? speakers.stream().filter(sp -> TranscriptSpeakers.same(sp, o.participante())).findFirst().orElse(null)
                        : null;
                // Todas as evidências da observação são da mesma pessoa: é ela.
                if (participant == null && speakers.size() == 1) participant = speakers.get(0);
                var observationEvidence = new LinkedHashMap<String, Evidencia>();
                ids.forEach(id -> observationEvidence.put(id, evidence.get(id)));
                observations.add(new Observacao(participant,
                        o.relacao() == null ? Relacao.NAO_IDENTIFICADA : o.relacao(),
                        o.percepcao() == null ? Percepcao.NAO_IDENTIFICADA : o.percepcao(),
                        o.experiencia() == null ? Experiencia.NAO_RELATADA : o.experiencia(),
                        context, claims(o.aspectos(), observationEvidence), ids));
            }
            if (observations.size() != list(p.observacoes()).size()) partial = true;
            if (observations.isEmpty()) continue;
            var productEvidence = new LinkedHashMap<String, Evidencia>();
            observations.forEach(o -> o.referencias().forEach(id -> productEvidence.put(id, evidence.get(id))));
            products.add(new Produto(entry == null ? p.nome() : entry.nome(),
                    entry == null ? p.fornecedor() : entry.fornecedor(), entry == null ? null : entry.id(),
                    entry == null ? (p.tipo() == null ? Tipo.OUTRO_FORNECEDOR : p.tipo()) : entry.tipo(), observations,
                    claims(p.motivosConsideracao(), productEvidence), claims(p.vantagens(), productEvidence),
                    claims(p.dificuldades(), productEvidence), claims(p.comparacoesCliente(), productEvidence),
                    claims(p.obstaculosOportunidades(), productEvidence), claims(p.recomendacoesVendedor(), productEvidence)));
        }
        var changes = claims(a.mudancasPercepcao(), evidence).stream().filter(c -> {
            var meetings = c.referencias().stream().map(evidence::get).map(e -> sources.get(e.referencia()))
                    .filter(s -> present(s.date())).map(Source::meetingId).distinct().toList();
            boolean sameProduct = products.stream().anyMatch(p -> {
                var entry = catalog.entradas().stream().filter(e -> e.id().equals(p.catalogoId())).findFirst().orElse(null);
                return c.referencias().stream().allMatch(id -> {
                    String quote = evidence.get(id).trecho();
                    return entry != null ? refersTo(entry, quote, allText, catalog)
                            : present(p.nome()) && !"Produto não especificado".equals(p.nome())
                                && CommercialCatalogService.mentions(quote, p.nome());
                });
            });
            return meetings.size() >= 2 && sameProduct;
        }).toList();
        boolean incomplete = partial || a.produtos() == null || a.evidencias() == null || a.estado() == null
                || products.size() != list(a.produtos()).size() || evidence.size() != list(a.evidencias()).size()
                || changes.size() != list(a.mudancasPercepcao()).size();
        Estado state = incomplete || a.estado() == Estado.EVIDENCIAS_INSUFICIENTES ? Estado.EVIDENCIAS_INSUFICIENTES
                : products.isEmpty() ? (a.estado() == Estado.SEM_MENCOES ? Estado.SEM_MENCOES : Estado.EVIDENCIAS_INSUFICIENTES)
                : Estado.CONCLUIDO;
        return new ProductsAnalysisDto(state, catalog, products, new ArrayList<>(evidence.values()), changes);
    }
}

