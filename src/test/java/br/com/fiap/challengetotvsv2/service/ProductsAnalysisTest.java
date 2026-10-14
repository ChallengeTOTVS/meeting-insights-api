package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.insights.*;
import br.com.fiap.challengetotvsv2.dto.insights.ProductsAnalysisDto.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ProductsAnalysisTest {
    private final CommercialCatalogService catalog = new CommercialCatalogService(JsonMapper.builder().build());
    private static final String QUOTE = "Ana: O Protheus parece uma boa solução; estamos avaliando. Pedro: usamos o Protheus e o suporte resolveu rapidamente.";
    private Achado claim(String text, String... refs) { return new Achado(text, List.of(refs)); }
    private Observacao observation(Percepcao perception, Experiencia experience) {
        return new Observacao("Ana", Relacao.AVALIANDO, perception, experience,
                claim("Avaliação de solução", "P1"), List.of(), List.of("P1"));
    }
    private Produto product(String id, String name, Observacao... observations) {
        return new Produto(name, id.equals("sap-b1") ? "SAP" : "TOTVS", id, Tipo.TOTVS,
                List.of(observations), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
    private ProductsAnalysisDto input(List<Produto> products, List<Evidencia> evidence, List<Achado> changes) {
        return new ProductsAnalysisDto(Estado.CONCLUIDO, null, products, evidence, changes);
    }
    private ProductsAnalysisDto sanitize(ProductsAnalysisDto a, String transcript) {
        return ProductsAnalysisSanitizer.sanitize(a, catalog.relevant(transcript),
                Map.of("ATUAL", new ProductsAnalysisSanitizer.Source(transcript, "2026-10-06", "current")));
    }
    @Test void catalogIsFilteredAndCompanyDoesNotSelectAnySpecificProduct() {
        var selected = catalog.relevant("Ana: Recebemos proposta da SAP. Senior Sistemas também foi citada.");
        assertThat(selected.entradas()).extracting(Entrada::id).containsExactly("sap", "senior");
        assertThat(selected.concorrencias()).isEmpty();
        assertThat(catalog.relevant("Sem produtos nesta conversa").entradas()).isEmpty();
        assertThat(catalog.relevant("confirmamos amanhã").entradas()).isEmpty();
        assertThat(catalog.relevant("ERP Protheus e SAP Business One").concorrencias()).isNotEmpty();
        assertThat(selected.entradas()).allSatisfy(e -> assertThat(e.fontes()).allSatisfy(f -> {
            assertThat(f.url()).startsWith("https://"); assertThat(f.revisadoEm()).isEqualTo("2026-10-06");
        }));
    }
    @Test void expectationAndRealExperienceRemainSeparatePerParticipant() {
        var ana = observation(Percepcao.POSITIVA, Experiencia.NAO_RELATADA);
        var pedro = new Observacao("Pedro", Relacao.UTILIZA_ATUALMENTE, Percepcao.POSITIVA, Experiencia.BOA,
                claim("Suporte resolveu", "P2"), List.of(), List.of("P2"));
        var output = sanitize(input(List.of(product("totvs-protheus", "Protheus", ana, pedro)),
                List.of(new Evidencia("P1", "ATUAL", "Ana: O Protheus parece uma boa solução; estamos avaliando.", "Ana", null),
                        new Evidencia("P2", "ATUAL", "Pedro: usamos o Protheus e o suporte resolveu rapidamente.", "Pedro", null)), List.of()), QUOTE);
        assertThat(output.produtos().getFirst().observacoes()).extracting(Observacao::experiencia)
                .containsExactly(Experiencia.NAO_RELATADA, Experiencia.BOA);
        assertThat(output.produtos().getFirst().nome()).isEqualTo("TOTVS Backoffice – Linha Protheus");
        assertThat(output.evidencias()).allSatisfy(e -> assertThat(e.dataReuniao()).isEqualTo("2026-10-06"));
        assertThat(output.catalogo().versao()).isEqualTo("2026-10-06.1");
    }
    @Test void catalogEntryCannotProveAnUnmentionedProduct() {
        var output = sanitize(input(List.of(product("sap-b1", "SAP Business One", observation(Percepcao.NEUTRA, Experiencia.NAO_RELATADA))),
                List.of(new Evidencia("P1", "ATUAL", "Ana: Recebemos proposta da SAP.", "Ana", null)), List.of()),
                "Ana: Recebemos proposta da SAP.");
        assertThat(output.produtos()).isEmpty();
        assertThat(output.estado()).isEqualTo(Estado.EVIDENCIAS_INSUFICIENTES);
    }
    @Test void fabricatedQuotesCannotProduceFindingsOrNoMentionsState() {
        var output = sanitize(input(List.of(product("totvs-protheus", "Protheus", observation(Percepcao.NEGATIVA, Experiencia.RUIM))),
                List.of(new Evidencia("P1", "ATUAL", "Ana: Protheus não funciona.", "Ana", null)), List.of()), QUOTE);
        assertThat(output.evidencias()).isEmpty(); assertThat(output.produtos()).isEmpty();
        assertThat(output.estado()).isEqualTo(Estado.EVIDENCIAS_INSUFICIENTES);
    }
    @Test void changesNeedDistinctMeetingsNotDifferentChunksOfTheSameMeeting() {
        var evidence = List.of(new Evidencia("P1", "R1", QUOTE, "Ana", null), new Evidencia("P2", "R2", QUOTE, "Ana", null));
        var input = input(List.of(product("totvs-protheus", "Protheus", observation(Percepcao.MISTA, Experiencia.MISTA))),
                evidence, List.of(claim("Mudança em datas distintas", "P1", "P2")));
        var output = ProductsAnalysisSanitizer.sanitize(input, catalog.relevant(QUOTE), Map.of(
                "R1", new ProductsAnalysisSanitizer.Source(QUOTE, "2026-09-01", "same"),
                "R2", new ProductsAnalysisSanitizer.Source(QUOTE, "2026-09-01", "same")));
        assertThat(output.mudancasPercepcao()).isEmpty();
        var changed = ProductsAnalysisSanitizer.sanitize(input, catalog.relevant(QUOTE), Map.of(
                "R1", new ProductsAnalysisSanitizer.Source(QUOTE, "2026-09-01", "previous"),
                "R2", new ProductsAnalysisSanitizer.Source(QUOTE, "2026-10-06", "current")));
        assertThat(changed.mudancasPercepcao()).hasSize(1);
    }
    @Test void catalogAndEvidenceRoundTripAndLegacyIsNull() {
        var mapper = JsonMapper.builder().build();
        var a = IndividualFixtures.analysis().analiseIndividual();
        var products = sanitize(input(List.of(product("totvs-protheus", "Protheus", observation(Percepcao.POSITIVA, Experiencia.NAO_RELATADA))),
                List.of(new Evidencia("P1", "ATUAL", QUOTE, "Ana", null)), List.of()), QUOTE);
        var read = mapper.readValue(mapper.writeValueAsString(a.withProducts(products)), IndividualAnalysisDto.class);
        assertThat(read.produtosExperiencias()).isEqualTo(products);
        var old = mapper.readValue(mapper.writeValueAsString(a).replace(",\"produtosExperiencias\":null", ""), IndividualAnalysisDto.class);
        assertThat(old.produtosExperiencias()).isNull();
        var analysis = IndividualFixtures.analysis();
        var withProducts = new AnaliseReuniaoDto(analysis.scoreChurn(), analysis.nivelRisco(), analysis.justificativaRisco(),
                analysis.sentimentoGeral(), analysis.resumo(), analysis.topicosPrincipais(), analysis.objecoes(),
                analysis.acoesRecomendadas(), a.withProducts(products));
        assertThat(IndividualAnalysisSanitizer.sanitize(withProducts, IndividualFixtures.TRANSCRIPT)
                .analiseIndividual().produtosExperiencias()).isEqualTo(products);
    }

    @Test void aspectsCannotBorrowEvidenceFromAnotherProductObservation() {
        var o = new Observacao("Ana", Relacao.AVALIANDO, Percepcao.POSITIVA, Experiencia.NAO_RELATADA,
                claim("Expectativa", "P1"), List.of(claim("Suporte ruim", "P2")), List.of("P1"));
        String transcript = QUOTE + " Ana: O suporte da SAP atrasou.";
        var output = sanitize(input(List.of(product("totvs-protheus", "Protheus", o)),
                List.of(new Evidencia("P1", "ATUAL", QUOTE, "Ana", null),
                        new Evidencia("P2", "ATUAL", "Ana: O suporte da SAP atrasou.", "Ana", null)), List.of()), transcript);
        assertThat(output.produtos().getFirst().observacoes().getFirst().aspectos()).isEmpty();
    }
}
