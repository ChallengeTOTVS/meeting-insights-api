package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.insights.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Casos da Rota Sul: o histórico saía vazio porque o filtro descartava conclusões inteiras
 * por uma referência inválida e não reconhecia quem falou cada trecho.
 */
class HistoricoEvidenciasTest {

    private static final String TRANSCRICAO = """
            Empresa cliente: Rota Sul Distribuidora Ltda.
            Data: 25/08/2026
            Camila Rocha: Boa tarde, Eduardo. Aqui é a Camila Rocha, executiva de contas da TOTVS.
            Eduardo Nunes: Hoje usamos o ERP Omie, faz três anos. Para financeiro e nota fiscal o Omie funciona bem.
            Camila Rocha (TOTVS): Proposta até 10/09 e a resposta sobre a convivência com o Omie junto com ela.
            """;

    @Test
    void identificaQuemFalouPeloRotuloDaLinha() {
        assertThat(TranscriptSpeakers.speakerOf(TRANSCRICAO, "Hoje usamos o ERP Omie, faz três anos."))
                .isEqualTo("Eduardo Nunes");
        assertThat(TranscriptSpeakers.speakerOf(TRANSCRICAO, "Proposta até 10/09"))
                .isEqualTo("Camila Rocha (TOTVS)");
        // Cabeçalho não é fala; trecho inexistente não tem dono.
        assertThat(TranscriptSpeakers.speakerOf(TRANSCRICAO, "Rota Sul Distribuidora Ltda.")).isNull();
        assertThat(TranscriptSpeakers.speakerOf(TRANSCRICAO, "frase que não existe")).isNull();
        assertThat(TranscriptSpeakers.same("Eduardo", "Eduardo Nunes")).isTrue();
        assertThat(TranscriptSpeakers.same("Camila Rocha", "Eduardo Nunes")).isFalse();
    }

    @Test
    void referenciasDoHistoricoViramCodigosCurtos() {
        var docs = List.of(
                new Document("a", "trecho 1", Map.of("referencia", "R-" + UUID.randomUUID() + "-C-0")),
                new Document("b", "trecho 2", Map.of("referencia", "R-" + UUID.randomUUID() + "-C-1")));
        var curtos = HistoricalInsightsService.comReferenciasCurtas(docs);
        assertThat(curtos).extracting(d -> d.getMetadata().get("referencia")).containsExactly("H1", "H2");
        assertThat(curtos).extracting(Document::getText).containsExactly("trecho 1", "trecho 2");
    }

    @Test
    void referenciaErradaNaoApagaAConclusaoInteira() {
        var evidencias = List.of(
                new HistoricalEvidenceDto("ATUAL", UUID.randomUUID(), null, "Atual", "x"),
                new HistoricalEvidenceDto("H1", UUID.randomUUID(), null, "Anterior", "y"));
        var compromisso = new HistoricalFindingDto("Proposta", "Enviar proposta", List.of("H1", "H99"), "Camila", "10/09", null);
        var vendedor = new AvaliacaoVendedorDto(null, null, null, "Melhorou", List.of(), List.of(), List.of("ATUAL", "H1", "Hx"));
        var evolucao = new HistoricalFindingDto("Risco", "Risco caiu", List.of("H1", "ATUAL"), null, null, null);
        var produtividade = new HistoricalFindingDto("Avanço", "Pendências resolvidas", List.of("ATUAL"), null, null, null);
        var analise = new HistoricalAnalysisDto(List.of(), evolucao, evolucao, List.of(), produtividade, vendedor,
                List.of(compromisso), List.of(), List.of(), List.of(), null, 2, "Resumo", List.of(), List.of());

        var r = HistoricalAnalysisSanitizer.sanitize(analise, evidencias);

        assertThat(r.compromissos()).hasSize(1);
        assertThat(r.compromissos().get(0).referencias()).containsExactly("H1");
        assertThat(r.qualidadeVendedor()).isNotNull();
        assertThat(r.qualidadeVendedor().referencias()).containsExactly("ATUAL", "H1");
        // Só tirar referência inválida não esconde o resumo.
        assertThat(r.resumoEvolucao()).isEqualTo("Resumo");
    }

    @Test
    void desempenhoComUmaReferenciaRealFicaEOInventadoSai() {
        var evidencias = List.of(
                new HistoricalEvidenceDto("ATUAL", UUID.randomUUID(), null, "Atual", "x"),
                new HistoricalEvidenceDto("H1", UUID.randomUUID(), null, "Anterior", "y"));
        var soAnterior = new AvaliacaoVendedorDto(null, null, null, "x", List.of(), List.of(), List.of("H1", "H404"));
        var analise = new HistoricalAnalysisDto(List.of(), null, null, List.of(), null, soAnterior,
                List.of(), List.of(), List.of(), List.of(), null, 2, "Resumo", List.of(), List.of());
        var seller = HistoricalAnalysisSanitizer.sanitize(analise, evidencias).qualidadeVendedor();
        assertThat(seller).isNotNull();
        assertThat(seller.referencias()).containsExactly("H1");
        var soInventadas = new AvaliacaoVendedorDto(null, null, null, "x", List.of(), List.of(), List.of("H404"));
        var analise2 = new HistoricalAnalysisDto(List.of(), null, null, List.of(), null, soInventadas,
                List.of(), List.of(), List.of(), List.of(), null, 2, "Resumo", List.of(), List.of());
        assertThat(HistoricalAnalysisSanitizer.sanitize(analise2, evidencias).qualidadeVendedor()).isNull();
    }

    @Test
    void fornecedorSozinhoContaQuandoOProdutoFoiNomeadoNaConversa() {
        var sankhya = new ProductsAnalysisDto.Entrada("sankhya-erp", "ERP Sankhya", "Sankhya", ProductsAnalysisDto.Tipo.CONCORRENTE,
                List.of("ERP Sankhya", "Sankhya ERP"), "ERP", List.of());
        var winthor = new ProductsAnalysisDto.Entrada("totvs-winthor", "Winthor", "TOTVS", ProductsAnalysisDto.Tipo.TOTVS,
                List.of("Winthor"), "ERP", List.of());
        var fluig = new ProductsAnalysisDto.Entrada("totvs-fluig", "TOTVS Fluig", "TOTVS", ProductsAnalysisDto.Tipo.TOTVS,
                List.of("Fluig"), "BPM", List.of());
        var catalogo = new ProductsAnalysisDto.Catalogo("v", List.of(sankhya, winthor, fluig), List.of());
        String conversa = "Eduardo: lá usávamos o ERP Sankhya. Camila: o Winthor e o Fluig.";

        // "o Sankhya" vale: o ERP Sankhya foi nomeado na conversa.
        assertThat(ProductsAnalysisSanitizer.refersTo(sankhya, "o Sankhya mandou proposta", conversa, catalogo)).isTrue();
        // Sem o produto nomeado em lugar nenhum, o fornecedor sozinho não basta.
        assertThat(ProductsAnalysisSanitizer.refersTo(sankhya, "o Sankhya mandou proposta", "o Sankhya ligou", catalogo)).isFalse();
        // "a TOTVS" é ambíguo com Winthor e Fluig nomeados.
        assertThat(ProductsAnalysisSanitizer.refersTo(winthor, "a TOTVS atrasou", conversa, catalogo)).isFalse();
    }
}
