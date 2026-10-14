package br.com.fiap.challengetotvsv2.service;
import org.junit.jupiter.api.Test;
import br.com.fiap.challengetotvsv2.dto.insights.*;
import static org.assertj.core.api.Assertions.*;

class IndividualAnalysisSanitizerTest {
    @Test void validAnalysisIsPreserved() {
        var original = IndividualFixtures.analysis();
        var expected = original.analiseIndividual().withFatosCompartilhaveis(new IndividualAnalysisDto.FatosCompartilhaveis(
                IndividualAnalysisDto.Estado.EVIDENCIAS_INSUFICIENTES, java.util.List.of()));
        assertThat(IndividualAnalysisSanitizer.sanitize(original, IndividualFixtures.TRANSCRIPT).analiseIndividual()).isEqualTo(expected);
    }
    @Test void invalidEvidenceDoesNotDiscardBasicInsights() {
        var result = IndividualAnalysisSanitizer.sanitize(IndividualFixtures.withEvidence("Fala inventada"), IndividualFixtures.TRANSCRIPT);
        assertThat(result.resumo()).isEqualTo("Resumo");
        assertThat(result.scoreChurn()).isEqualTo(0.3);
        assertThat(result.analiseIndividual().evidencias()).isEmpty();
        assertThat(result.analiseIndividual().vendedor()).isNull();
        assertThat(result.analiseIndividual().interacoes()).isEmpty();
        assertThat(result.analiseIndividual().estado()).isEqualTo(IndividualAnalysisDto.Estado.EVIDENCIAS_INSUFICIENTES);
    }
    @Test void missingSellerReferencesKeepOtherValidSections() {
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var json = mapper.writeValueAsString(IndividualFixtures.analysis()).replace("\"evidenciasVendedor\":[\"E1\"]", "\"evidenciasVendedor\":null");
        var input = mapper.readValue(json, AnaliseReuniaoDto.class);
        var result = IndividualAnalysisSanitizer.sanitize(input, IndividualFixtures.TRANSCRIPT).analiseIndividual();
        assertThat(result.vendedor()).isNull();
        assertThat(result.desempenho().estado()).isEqualTo(IndividualAnalysisDto.Estado.EVIDENCIAS_INSUFICIENTES);
        assertThat(result.interacoes()).hasSize(3);
        assertThat(result.compromissos()).hasSize(1);
    }
    @Test void absentIndividualAnalysisCanStillBeSaved() {
        var input = new AnaliseReuniaoDto(0.2, "BAIXO", "Justificativa", "NEUTRO", "Resumo útil", "Tópico", "", "Ação", null);
        var result = IndividualAnalysisSanitizer.sanitize(input, IndividualFixtures.TRANSCRIPT);
        assertThat(result.resumo()).isEqualTo("Resumo útil");
        assertThat(result.analiseIndividual().estado()).isEqualTo(IndividualAnalysisDto.Estado.EVIDENCIAS_INSUFICIENTES);
    }
    @Test void noResponseIsStillAnError() {
        assertThatThrownBy(() -> IndividualAnalysisSanitizer.sanitize(null, "texto")).isInstanceOf(IllegalStateException.class);
    }
}
