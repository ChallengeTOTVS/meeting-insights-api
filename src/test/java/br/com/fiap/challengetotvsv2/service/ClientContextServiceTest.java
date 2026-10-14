package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.insights.IndividualAnalysisDto;
import br.com.fiap.challengetotvsv2.dto.cliente.ClientFactSourceDto;
import br.com.fiap.challengetotvsv2.enums.StatusReuniao;
import br.com.fiap.challengetotvsv2.exception.ClienteNotFoundException;
import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import br.com.fiap.challengetotvsv2.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;
import java.time.LocalDateTime;
import java.util.*;
import static br.com.fiap.challengetotvsv2.dto.insights.IndividualAnalysisDto.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientContextServiceTest {
    private final IClienteRepository clients=mock(IClienteRepository.class);
    private final IReuniaoRepository meetings=mock(IReuniaoRepository.class);
    private final IUsuarioRepository users=mock(IUsuarioRepository.class);
    private final tools.jackson.databind.json.JsonMapper mapper = tools.jackson.databind.json.JsonMapper.builder().build();
    private final UUID clientId=UUID.randomUUID();
    private ClientContextService service;
    @BeforeEach void setup() {
        var user=UsuarioEntity.builder().id(UUID.randomUUID()).email("worker@example.test").build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getEmail(),null,List.of()));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(clients.existsById(clientId)).thenReturn(true);
        service=new ClientContextService(clients,meetings,users,mapper);
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    private ClientFactSourceDto source(int day, String price, UUID meeting) {
        var fact=new FatoCompartilhavel(TipoFato.PRECO_CONDICAO_COMERCIAL,"Preço mensal",price,
                SituacaoFato.DEFINIDO,List.of("E1"));
        var facts=new FatosCompartilhaveis(Estado.CONCLUIDO,List.of(fact));
        var individual=new IndividualAnalysisDto(2,Estado.CONCLUIDO,"Seller private",List.of("E1"),
                new Conclusao("private evaluation",List.of("E1")), new Conclusao("private context",List.of("E1")),
                List.of(),List.of(),new Categoria(Estado.SEM_ACHADOS,List.of()),
                new Desempenho(Estado.CONCLUIDO,List.of(),List.of(),List.of(),List.of(),List.of()),
                List.of(new Evidencia("E1","Customer and seller agreed to this price.")),null,facts);
        return new ClientFactSourceDto(meeting,LocalDateTime.of(2026,9,day,10,0),mapper.writeValueAsString(individual));
    }
    @Test void returnsFactsFromOtherEmployeesWithDatesAndFlagsConflictsWithoutExposingPrivateAnalysis() {
        UUID first=UUID.randomUUID(), second=UUID.randomUUID();
        when(meetings.contarPorClienteEStatus(clientId,StatusReuniao.CONCLUIDA)).thenReturn(2L);
        when(meetings.buscarFatosCompartilhaveis(clientId,StatusReuniao.CONCLUIDA)).thenReturn(List.of(source(1,"R$ 100/mês",first),source(5,"R$ 150/mês",second)));
        var result=service.consultar(clientId);
        assertThat(result.reunioesConcluidas()).isEqualTo(2);
        assertThat(result.divergenciasEncontradas()).isTrue();
        assertThat(result.fatos()).hasSize(2).allMatch(IndividualAnalysisDtoFato -> IndividualAnalysisDtoFato.divergente());
        assertThat(result.fatos()).extracting(f -> f.datasReuniao().getFirst().getDayOfMonth()).containsExactly(1,5);
        String json=mapper.writeValueAsString(result);
        assertThat(json).contains("R$ 100/mês","R$ 150/mês","datasReuniao");
        assertThat(json).doesNotContain("Seller private","private evaluation","private context","evidencias","transcricao","scoreChurn",first.toString());
        verify(meetings).buscarFatosCompartilhaveis(clientId,StatusReuniao.CONCLUIDA);
    }
    @Test void repeatedSameFactConsolidatesDatesWithoutFalseConflict() {
        when(meetings.contarPorClienteEStatus(clientId,StatusReuniao.CONCLUIDA)).thenReturn(2L);
        when(meetings.buscarFatosCompartilhaveis(clientId,StatusReuniao.CONCLUIDA)).thenReturn(List.of(
                source(1,"R$ 100/mês",UUID.randomUUID()),source(5,"R$ 100/mês",UUID.randomUUID())));
        var result=service.consultar(clientId);
        assertThat(result.divergenciasEncontradas()).isFalse();
        assertThat(result.fatos()).hasSize(1);
        assertThat(result.fatos().getFirst().datasReuniao()).hasSize(2);
    }
    @Test void oldAnalysisWithoutSharedFactsIsExplicitlyUnavailableAndNeverRegenerated() {
        when(meetings.contarPorClienteEStatus(clientId,StatusReuniao.CONCLUIDA)).thenReturn(1L);
        when(meetings.buscarFatosCompartilhaveis(clientId,StatusReuniao.CONCLUIDA)).thenReturn(List.of(new ClientFactSourceDto(UUID.randomUUID(),LocalDateTime.now(),"{}")));
        var result=service.consultar(clientId);
        assertThat(result.estado()).isEqualTo("SEM_FATOS_DISPONIVEIS");
        assertThat(result.mensagem()).contains("não foram reanalisadas automaticamente");
        assertThat(result.fatos()).isEmpty();
        verify(meetings).contarPorClienteEStatus(clientId,StatusReuniao.CONCLUIDA);
        verify(meetings).buscarFatosCompartilhaveis(clientId,StatusReuniao.CONCLUIDA);
        verifyNoMoreInteractions(meetings);
    }
    @Test void clientNotFoundIsRejectedBeforeReadingMeetingFacts() {
        when(clients.existsById(clientId)).thenReturn(false);
        assertThatThrownBy(()->service.consultar(clientId)).isInstanceOf(ClienteNotFoundException.class);
        verify(meetings,never()).buscarFatosCompartilhaveis(any(),any());
    }
    @Test void zeroCompletedMeetingsHasSeparateEmptyState() {
        when(meetings.contarPorClienteEStatus(clientId,StatusReuniao.CONCLUIDA)).thenReturn(0L);
        when(meetings.buscarFatosCompartilhaveis(clientId,StatusReuniao.CONCLUIDA)).thenReturn(List.of());
        var result=service.consultar(clientId);
        assertThat(result.estado()).isEqualTo("SEM_FATOS_DISPONIVEIS");
        assertThat(result.mensagem()).isEqualTo("Este cliente ainda não tem reuniões concluídas.");
        verify(meetings).contarPorClienteEStatus(clientId,StatusReuniao.CONCLUIDA);
    }
}
