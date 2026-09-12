package br.com.fiap.challengetotvsv2.service;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class IndividualAnalysisValidatorTest {
    @Test void firstMeetingHasCompletePartialAndUnansweredQuestionsWithoutHistory() {
        var a = IndividualFixtures.analysis();
        assertThatCode(() -> IndividualAnalysisValidator.validate(a, IndividualFixtures.TRANSCRIPT)).doesNotThrowAnyException();
        assertThat(a.analiseIndividual().interacoes()).extracting(i -> i.resposta().name())
                .containsExactly("COMPLETA", "PARCIAL", "NAO_RESPONDIDA");
        assertThat(a.analiseIndividual().interacoes().get(2).situacao().name()).isEqualTo("ACOMPANHAMENTO");
    }
    @Test void fabricatedQuotesAreRejectedWithoutRepairCall() {
        assertThatThrownBy(() -> IndividualAnalysisValidator.validate(IndividualFixtures.withEvidence("Cliente: Estou satisfeito."), IndividualFixtures.TRANSCRIPT))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Trecho não encontrado");
    }
    @Test void unresolvedPromiseCannotBecomeResolved() {
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var json = mapper.writeValueAsString(IndividualFixtures.analysis())
                .replace("\"situacao\":\"ACOMPANHAMENTO\"", "\"situacao\":\"RESOLVIDO\"");
        var response = mapper.readValue(json, br.com.fiap.challengetotvsv2.dto.insights.AnaliseReuniaoDto.class);
        assertThatThrownBy(() -> IndividualAnalysisValidator.validate(response, IndividualFixtures.TRANSCRIPT))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Resposta incompleta");
    }
    @Test void unknownEvidenceReferenceIsRejected() {
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var json = mapper.writeValueAsString(IndividualFixtures.analysis())
                .replace("\"referencias\":[\"E1\"]", "\"referencias\":[\"E404\"]");
        var response = mapper.readValue(json, br.com.fiap.challengetotvsv2.dto.insights.AnaliseReuniaoDto.class);
        assertThatThrownBy(() -> IndividualAnalysisValidator.validate(response, IndividualFixtures.TRANSCRIPT))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Referência inexistente");
    }
    @Test void emptyModelResponseIsFailureNotNoFindings() {
        assertThatThrownBy(() -> IndividualAnalysisValidator.validate(null, IndividualFixtures.TRANSCRIPT)).isInstanceOf(IllegalStateException.class);
    }
}
