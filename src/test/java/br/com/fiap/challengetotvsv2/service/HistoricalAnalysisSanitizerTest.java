package br.com.fiap.challengetotvsv2.service;
import br.com.fiap.challengetotvsv2.dto.insights.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class HistoricalAnalysisSanitizerTest {
    private final List<HistoricalEvidenceDto> evidence = List.of(
            new HistoricalEvidenceDto("ATUAL", UUID.randomUUID(), null, "Atual", "Texto"),
            new HistoricalEvidenceDto("E1", UUID.randomUUID(), null, "Anterior", "Texto"));
    private HistoricalFindingDto finding(String text, String... refs) {
        return new HistoricalFindingDto("Título", text, List.of(refs), null, null, null);
    }
    private HistoricalAnalysisDto analysis(List<HistoricalFindingDto> findings, HistoricalFindingDto comparison) {
        return new HistoricalAnalysisDto(findings, comparison, comparison, List.of(), null, null,
                List.of(), List.of(), List.of(), List.of(), null, 2, "Resumo", List.of(), List.of());
    }
    @Test void keepsValidFindingWhenAnotherReferenceIsMissing() {
        var valid = finding("Objeção válida", "E1");
        var result = HistoricalAnalysisSanitizer.sanitize(analysis(List.of(valid, finding("Inválida", "E404")), null), evidence);
        assertThat(result.objecoesRecorrentes()).containsExactly(valid);
        assertThat(result.categoriasSemEvidencia()).contains("objecoesRecorrentes");
        assertThat(result.observacoes()).contains("parcialmente");
    }
    @Test void comparisonsRequireBothPeriods() {
        var result = HistoricalAnalysisSanitizer.sanitize(analysis(List.of(), finding("Mudança", "E1")), evidence);
        assertThat(result.evolucaoRiscoChurn()).isNull();
        var valid = finding("Mudança", "E1", "ATUAL");
        assertThat(HistoricalAnalysisSanitizer.sanitize(analysis(List.of(), valid), evidence).evolucaoRiscoChurn()).isEqualTo(valid);
    }
    @Test void duplicatesAndNullGroupsDoNotFailWholeResult() {
        var f = finding("Duplicado", "E1");
        assertThat(HistoricalAnalysisSanitizer.sanitize(analysis(List.of(f, f), null), evidence).objecoesRecorrentes()).hasSize(1);
        assertThat(HistoricalAnalysisSanitizer.sanitize(analysis(null, null), evidence).categoriasSemEvidencia()).contains("objecoesRecorrentes");
    }
    @Test void emptyResponseIsNotReportedAsSuccess() {
        assertThatThrownBy(() -> HistoricalAnalysisSanitizer.sanitize(null, evidence)).isInstanceOf(IllegalStateException.class);
    }
}
