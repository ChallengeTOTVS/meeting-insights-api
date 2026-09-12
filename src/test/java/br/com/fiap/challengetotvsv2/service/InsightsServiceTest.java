package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.model.*;
import br.com.fiap.challengetotvsv2.repository.*;
import br.com.fiap.challengetotvsv2.enums.StatusReuniao;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class InsightsServiceTest {
    final IInsightRepository insights = mock(IInsightRepository.class);
    final IReuniaoRepository meetings = mock(IReuniaoRepository.class);
    final IUsuarioRepository users = mock(IUsuarioRepository.class);
    final AnaliseIaService ai = mock(AnaliseIaService.class);
    final IndividualAnalysisJobs jobs = mock(IndividualAnalysisJobs.class);
    final UUID id = UUID.randomUUID();
    final ReuniaoEntity meeting = new ReuniaoEntity();
    InsightsService service;
    @BeforeEach void setup() {
        var user = UsuarioEntity.builder().id(UUID.randomUUID()).email("test@example.test").nome("Cadastrante ausente").build();
        meeting.setId(id); meeting.setUsuario(user); meeting.setStatus(StatusReuniao.PENDENTE);
        meeting.setTranscricao(IndividualFixtures.TRANSCRIPT);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of()));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(meetings.findByIdAndUsuarioId(id, user.getId())).thenReturn(Optional.of(meeting));
        service = new InsightsService(insights, meetings, users, ai, jobs, JsonMapper.builder().build());
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void legacyReadAndPollingNeverCallAiOrWrite() {
        var old = InsightEntity.builder().reuniao(meeting).resumo("Original").build();
        when(insights.findByReuniaoId(id)).thenReturn(Optional.of(old));
        assertThat(service.buscarInsightsPorReuniao(id).estadoAvaliacao()).isEqualTo("VERSAO_ANTIGA");
        assertThat(service.consultar(id).status()).isEqualTo("VERSAO_ANTIGA");
        verifyNoInteractions(ai);
        verify(insights, never()).save(any()); verify(jobs, never()).claim(any());
    }
    @Test void firstMeetingIsPersistedWithSellerFromTranscript() {
        when(jobs.claim(id)).thenReturn(true);
        when(ai.analisar(meeting.getTranscricao())).thenReturn(IndividualFixtures.analysis());
        when(insights.save(any())).thenAnswer(c -> c.getArgument(0));
        var result = service.gerarInsights(id);
        assertThat(result.analiseIndividual().vendedor()).isEqualTo("Bruno");
        verify(ai, times(1)).analisar(IndividualFixtures.TRANSCRIPT);
        verify(insights).save(argThat(i -> i.getAnaliseIndividualJson().contains("I3")));
        verify(jobs).finish(id, "CONCLUIDO");
    }
    @Test void explicitUpgradeKeepsEveryOriginalFieldAndCreationDate() {
        var old = InsightEntity.builder().reuniao(meeting).scoreChurn(0.9).resumo("Original").objecoes("Original objection").dataCriacao(java.time.LocalDateTime.of(2020,1,1,1,0)).build();
        when(insights.findByReuniaoId(id)).thenReturn(Optional.of(old));
        when(jobs.claim(id)).thenReturn(true);
        when(ai.analisar(anyString())).thenReturn(IndividualFixtures.analysis());
        when(insights.save(any())).thenAnswer(c -> c.getArgument(0));
        var result = service.atualizarAvaliacao(id);
        assertThat(result.resumo()).isEqualTo("Original"); assertThat(result.scoreChurn()).isEqualTo(0.9);
        assertThat(result.objecoes()).isEqualTo("Original objection"); assertThat(result.dataCriacao().getYear()).isEqualTo(2020);
        assertThat(result.analiseIndividual()).isNotNull();
    }
    @Test void losingConcurrentClaimNeverCallsAi() {
        when(jobs.claim(id)).thenReturn(false);
        assertThatThrownBy(() -> service.gerarInsights(id)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(ai);
    }
    @Test void failedGenerationCannotBeRetriedByAnotherClick() {
        when(jobs.claim(id)).thenReturn(true);
        when(ai.analisar(anyString())).thenThrow(new IllegalStateException("simulated timeout"));
        assertThatThrownBy(() -> service.gerarInsights(id)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.gerarInsights(id)).isInstanceOf(ResponseStatusException.class);
        service.consultar(id);
        verify(ai, times(1)).analisar(anyString()); verify(jobs).finish(id, "ERRO");
    }
    @Test void savedResultIsIdempotentOnPost() {
        when(insights.findByReuniaoId(id)).thenReturn(Optional.of(InsightEntity.builder().reuniao(meeting).resumo("Saved").build()));
        assertThat(service.gerarInsights(id).resumo()).isEqualTo("Saved");
        verifyNoInteractions(ai, jobs);
    }
    @Test void forbiddenMeetingDoesNotReadSavedAnalysisOrClaim() {
        when(meetings.findByIdAndUsuarioId(eq(id), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.gerarInsights(id)).isInstanceOf(br.com.fiap.challengetotvsv2.exception.ReuniaoNotFoundException.class);
        verifyNoInteractions(ai, insights, jobs);
    }
}
