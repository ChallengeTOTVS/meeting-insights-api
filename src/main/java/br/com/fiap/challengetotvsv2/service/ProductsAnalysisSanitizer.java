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
    private static Achado claim(Achado c, Map<String, Evidencia> evidence) {
        if (c == null || !present(c.texto()) || c.referencias() == null || c.referencias().isEmpty()
                || !evidence.keySet().containsAll(c.referencias())) return null;
        return new Achado(c.texto(), refs(c.referencias(), evidence));
    }
    private static List<Achado> claims(List<Achado> items, Map<String, Evidencia> evidence) {
        return list(items).stream().map(c -> claim(c, evidence)).filter(Objects::nonNull).toList();
    }
    public static ProductsAnalysisDto sanitize(ProductsAnalysisDto a, Catalogo catalog, Map<String, Source> sources) {
        if (a == null) return new ProductsAnalysisDto(Estado.EVIDENCIAS_INSUFICIENTES, catalog, List.of(), List.of(), List.of());
        var evidence = new LinkedHashMap<String, Evidencia>();
        for (var e : list(a.evidencias())) {
            var src = sources.get(e.referencia());
            if (src == null || !present(e.id()) || !present(e.trecho())
                    || !IndividualAnalysisValidator.normalize(src.text()).contains(IndividualAnalysisValidator.normalize(e.trecho()))) continue;
            String participant = present(e.participante()) && e.trecho().contains(e.participante()) ? e.participante() : null;
            evidence.putIfAbsent(e.id(), new Evidencia(e.id(), e.referencia(), e.trecho(), participant, src.date()));
        }
        var products = new ArrayList<Produto>();
        boolean partial = false;
        for (var p : list(a.produtos())) {
            var observations = new ArrayList<Observacao>();
            var entry = catalog.entradas().stream().filter(e -> e.id().equals(p.catalogoId())).findFirst().orElse(null);
            for (var o : list(p.observacoes())) {
                var ids = refs(o.referencias(), evidence);
                var context = claim(o.contexto(), evidence);
                if (ids.isEmpty() || context == null || !ids.containsAll(context.referencias())) continue;
                String quotes = ids.stream().map(id -> evidence.get(id).trecho()).reduce("", (x, y) -> x + "\n" + y);
                boolean mentioned = entry != null
                        ? entry.apelidos().stream().anyMatch(alias -> CommercialCatalogService.mentions(quotes, alias))
                        : ("Produto não especificado".equals(p.nome())
                            ? CommercialCatalogService.mentions(quotes, p.fornecedor())
                            : CommercialCatalogService.mentions(quotes, p.nome()));
                if (!mentioned || (entry == null && present(p.fornecedor())
                        && !CommercialCatalogService.mentions(quotes, p.fornecedor()))) continue;
                String participant = present(o.participante()) && ids.stream().anyMatch(id ->
                        Objects.equals(evidence.get(id).participante(), o.participante())) ? o.participante() : null;
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
                    return entry != null ? entry.apelidos().stream().anyMatch(alias -> CommercialCatalogService.mentions(quote, alias))
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

