package br.com.fiap.challengetotvsv2.service;
import br.com.fiap.challengetotvsv2.dto.insights.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
class LegacyAnalysisCompatibilityTest {
    @Test void oldHistoricalJsonStillLoadsWithoutNewFields() {
        var a = JsonMapper.builder().build().readValue("""
          {"objecoesRecorrentes":[{"titulo":"Preço","descricao":"Pedido de desconto","referencias":["R1"]}],"observacoes":"Original"}
          """, HistoricalAnalysisDto.class);
        assertThat(a.versao()).isNull(); assertThat(a.satisfacaoCliente()).isNull();
        assertThat(a.objecoesRecorrentes().getFirst().descricao()).isEqualTo("Pedido de desconto");
        assertThat(a.objecoesRecorrentes().getFirst().responsavel()).isNull();
    }
    @Test void structuredIndividualRoundTripPreservesAnswersAndEvidence() {
        var mapper = JsonMapper.builder().build(); var a = IndividualFixtures.analysis().analiseIndividual();
        var read = mapper.readValue(mapper.writeValueAsString(a), IndividualAnalysisDto.class);
        assertThat(read).isEqualTo(a);
    }
}
