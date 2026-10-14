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
    final RagService rag = mock(RagService.class);
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
        service = new InsightsService(insights, meetings, users, ai, jobs, JsonMapper.builder().build(), rag);
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void legacyReadAndPollingNeverCallAiOrWrite() {
        var old = InsightEntity.builder().reuniao(meeting).resumo("Original").build();
        when(insights.findByReuniaoId(id)).thenReturn(Optional.of(old));
        assertThat(service.buscarInsightsPorReuniao(id).estadoAvaliacao()).isEqualTo("VERSAO_ANTIGA");
        assertThat(service.consultar(id).status()).isEqualTo("VERSAO_ANTIGA");
        verifyNoInteractions(ai);
        verify(rag, never()).indexarReuniao(any());
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
        verify(rag).indexarReuniao(meeting);
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
    @Test void explicitRetryAfterFailureSavesResultOnce() {
        meeting.setStatus(StatusReuniao.ERRO);
        when(jobs.retry(id, "failure-1")).thenReturn(true).thenReturn(false);
        when(ai.analisar(anyString())).thenReturn(IndividualFixtures.analysis());
        when(insights.save(any())).thenAnswer(c -> c.getArgument(0));
        assertThat(service.tentarNovamente(id, "failure-1").analiseIndividual()).isNotNull();
        assertThatThrownBy(() -> service.tentarNovamente(id, "failure-1")).isInstanceOf(ResponseStatusException.class);
        verify(ai, times(1)).analisar(anyString());
        verify(jobs, never()).claim(any());
    }
    @Test void staleOrRunningAttemptCannotBeRetried() {
        when(jobs.retry(id, "stale")).thenReturn(false);
        assertThatThrownBy(() -> service.tentarNovamente(id, "stale")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.tentarNovamente(id, "")).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(ai);
    }
    @Test void retryOfLegacyUpgradePreservesOriginalInsight() {
        var old = InsightEntity.builder().reuniao(meeting).resumo("Original").scoreChurn(0.7).build();
        when(insights.findByReuniaoId(id)).thenReturn(Optional.of(old));
        when(jobs.retry(id, "failure-1")).thenReturn(true);
        when(ai.analisar(anyString())).thenReturn(IndividualFixtures.analysis());
        when(insights.save(any())).thenAnswer(c -> c.getArgument(0));
        var result = service.tentarNovamente(id, "failure-1");
        assertThat(result.resumo()).isEqualTo("Original");
        assertThat(result.scoreChurn()).isEqualTo(0.7);
        assertThat(result.analiseIndividual()).isNotNull();
    }
    @Test void forbiddenMeetingDoesNotReadSavedAnalysisOrClaim() {
        when(meetings.findByIdAndUsuarioId(eq(id), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.gerarInsights(id)).isInstanceOf(br.com.fiap.challengetotvsv2.exception.ReuniaoNotFoundException.class);
        verifyNoInteractions(ai, insights, jobs);
    }

    @Test void productCatalogIsSavedWithIndividualAndReadsNeverCallAiAgain() {
        var catalog = new CommercialCatalogService(JsonMapper.builder().build()).relevant("Protheus");
        var products = new br.com.fiap.challengetotvsv2.dto.insights.ProductsAnalysisDto(
                br.com.fiap.challengetotvsv2.dto.insights.ProductsAnalysisDto.Estado.SEM_MENCOES,
                catalog, List.of(), List.of(), List.of());
        var base = IndividualFixtures.analysis();
        var result = new br.com.fiap.challengetotvsv2.dto.insights.AnaliseReuniaoDto(
                base.scoreChurn(), base.nivelRisco(), base.justificativaRisco(), base.sentimentoGeral(),
                base.resumo(), base.topicosPrincipais(), base.objecoes(), base.acoesRecomendadas(),
                base.analiseIndividual().withProducts(products));
        var saved = new java.util.concurrent.atomic.AtomicReference<InsightEntity>();
        when(jobs.claim(id)).thenReturn(true);
        when(ai.analisar(anyString())).thenReturn(result);
        when(insights.save(any())).thenAnswer(c -> { saved.set(c.getArgument(0)); return saved.get(); });
        assertThat(service.gerarInsights(id).analiseIndividual().produtosExperiencias()).isEqualTo(products);
        verify(ai, times(1)).analisar(anyString());
        assertThat(saved.get().getAnaliseIndividualJson()).contains(catalog.versao());
        when(insights.findByReuniaoId(id)).thenReturn(Optional.of(saved.get()));
        clearInvocations(ai);
        assertThat(service.buscarInsightsPorReuniao(id).analiseIndividual().produtosExperiencias()).isEqualTo(products);
        service.consultar(id);
        service.gerarInsights(id);
        verifyNoInteractions(ai);
    }

    @Test void vectorFailureDoesNotTurnSavedInsightIntoFailedAnalysisOrCallAiAgain() {
        when(jobs.claim(id)).thenReturn(true);
        when(ai.analisar(anyString())).thenReturn(IndividualFixtures.analysis());
        when(insights.save(any())).thenAnswer(c -> c.getArgument(0));
        doThrow(new IllegalStateException("Vector store unavailable")).when(rag).indexarReuniao(meeting);
        var result = service.gerarInsights(id);
        assertThat(result.analiseIndividual()).isNotNull();
        assertThat(result.historicoDisponivel()).isFalse();
        assertThat(meeting.getStatus()).isEqualTo(StatusReuniao.CONCLUIDA);
        verify(ai, times(1)).analisar(anyString());
        verify(jobs).finish(id, "CONCLUIDO");
        verify(jobs, never()).finish(id, "ERRO");
    }
    @Test void indexingRunsAfterInsightPersistenceAndReturnsAvailability() {
        when(jobs.claim(id)).thenReturn(true);
        when(ai.analisar(anyString())).thenReturn(IndividualFixtures.analysis());
        when(insights.save(any())).thenAnswer(c -> c.getArgument(0));
        when(rag.estaIndexada(meeting)).thenReturn(true);
        assertThat(service.gerarInsights(id).historicoDisponivel()).isTrue();
        var order = inOrder(insights, rag, jobs);
        order.verify(insights).save(any());
        order.verify(rag).indexarReuniao(meeting);
        order.verify(jobs).finish(id, "CONCLUIDO");
        verify(ai, times(1)).analisar(anyString());
    }

    @Test void savedInsightDoesNotHidePreparationStillInProgress() {
        var base = IndividualFixtures.analysis().analiseIndividual();
        var saved = InsightEntity.builder().reuniao(meeting).analiseIndividualJson(JsonMapper.builder().build().writeValueAsString(base)).build();
        when(insights.findByReuniaoId(id)).thenReturn(Optional.of(saved));
        when(jobs.status(id)).thenReturn("PROCESSANDO");
        assertThat(service.consultar(id).status()).isEqualTo("PROCESSANDO");
        verifyNoInteractions(ai);
        verify(rag, never()).indexarReuniao(any());
    }
}
