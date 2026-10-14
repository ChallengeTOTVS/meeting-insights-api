package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.insights.ProductsAnalysisDto.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class CommercialCatalogService {
    private final Catalogo catalog;
    private final ObjectMapper mapper;
    public CommercialCatalogService(ObjectMapper mapper) {
        this.mapper = mapper;
        try (var input = getClass().getResourceAsStream("/catalogo-comercial.json")) {
            if (input == null) throw new IllegalStateException("Catálogo comercial ausente");
            catalog = mapper.readValue(input, Catalogo.class);
        } catch (java.io.IOException e) { throw new IllegalStateException("Não foi possível ler o catálogo", e); }
    }
    static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
    static boolean mentions(String text, String alias) {
        if (alias == null || alias.isBlank()) return false;
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(normalize(alias)) + "(?![\\p{L}\\p{N}])")
                .matcher(normalize(text)).find();
    }
    public Catalogo relevant(String text) {
        var entries = catalog.entradas().stream()
                .filter(e -> e.apelidos().stream().anyMatch(a -> mentions(text, a))).toList();
        var ids = entries.stream().map(Entrada::id).toList();
        return new Catalogo(catalog.versao(), entries, catalog.concorrencias().stream()
                .filter(c -> ids.contains(c.produtoId()) && ids.contains(c.concorrenteId())).toList());
    }
    public String prompt(Catalogo selected) { return mapper.writeValueAsString(selected); }
}

