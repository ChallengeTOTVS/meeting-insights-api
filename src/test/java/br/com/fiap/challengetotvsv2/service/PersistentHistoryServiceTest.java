package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.model.*;
import br.com.fiap.challengetotvsv2.repository.*;
import br.com.fiap.challengetotvsv2.exception.ReuniaoNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PersistentHistoryServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final HistoricalInsightsService generator = mock(HistoricalInsightsService.class);
    private final IUsuarioRepository users = mock(IUsuarioRepository.class);
    private final IReuniaoRepository meetings = mock(IReuniaoRepository.class);
    private final IInsightRepository insights = mock(IInsightRepository.class);
    private final UUID id = UUID.randomUUID();
    private PersistentHistoryService service;

    @BeforeEach
    void setup() {
        var user = new UsuarioEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.test");
        var client = new ClienteEntity();
        client.setId(UUID.randomUUID());
        var meeting = new ReuniaoEntity();
        meeting.setId(id);
        meeting.setUsuario(user);
        meeting.setCliente(client);
        meeting.setDataReuniao(LocalDateTime.now());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of()));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(meetings.findByIdAndUsuarioId(id, user.getId())).thenReturn(Optional.of(meeting));
        service = new PersistentHistoryService(jdbc, JsonMapper.builder().build(), generator, users, meetings, insights);
    }

    @AfterEach
    void cleanup() { service.shutdown(); SecurityContextHolder.clearContext(); }

    @Test
    void readingHistoryNeverCallsAiOrWrites() {
        var result = service.consultar(id);
        assertThat(result.status()).isEqualTo("NAO_GERADO");
        assertThat(result.resultado()).isNotNull();
        verifyNoInteractions(generator);
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void repeatedPostNeverStartsAnotherGeneration() {
        // The INSERT ... ON CONFLICT claim returns zero for existing jobs, including failed jobs.
        when(jdbc.update(anyString(), eq(id))).thenReturn(0);
        service.iniciar(id);
        service.iniciar(id);
        verifyNoInteractions(generator);
        verify(jdbc, never()).queryForObject(anyString(), eq(Integer.class), any(Object[].class));
    }

    @Test
    void noIndexedHistoryPersistsBaseWithoutCallingAi() {
        when(jdbc.update(startsWith("INSERT"), eq(id))).thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any())).thenReturn(0);
        service.iniciar(id);
        verifyNoInteractions(generator);
        verify(jdbc).update(startsWith("UPDATE historical_analysis_job SET status='CONCLUIDO'"), anyString(), eq(id));
    }

    @Test
    void forbiddenMeetingCannotCreateJob() {
        when(meetings.findByIdAndUsuarioId(eq(id), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.iniciar(id)).isInstanceOf(ReuniaoNotFoundException.class);
        verifyNoInteractions(generator, jdbc);
    }
    @Test
    void asyncResultIsSavedAndRepeatedClickDoesNotCallAiAgain() {
        when(jdbc.update(startsWith("INSERT"), eq(id))).thenReturn(1, 0);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any())).thenReturn(2);
        var result = new br.com.fiap.challengetotvsv2.dto.insights.HistoricalInsightsResponseDto(
                id, UUID.randomUUID(), true, "saved", List.of(), null, List.of());
        when(generator.gerarHistorico(id)).thenReturn(result);
        service.iniciar(id);
        service.iniciar(id);
        verify(jdbc, timeout(3000)).update(startsWith("UPDATE historical_analysis_job SET status='CONCLUIDO'"),
                contains("saved"), eq(id));
        verify(generator, times(1)).gerarHistorico(id);
    }

    @Test
    void asyncFailureIsRecordedAndNeverRetriedOnPost() {
        when(jdbc.update(startsWith("INSERT"), eq(id))).thenReturn(1, 0);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any())).thenReturn(2);
        when(generator.gerarHistorico(id)).thenThrow(new IllegalStateException("simulated timeout"));
        service.iniciar(id);
        verify(jdbc, timeout(3000)).update(startsWith("UPDATE historical_analysis_job SET status='ERRO'"), eq(id));
        service.iniciar(id);
        verify(generator, times(1)).gerarHistorico(id);
    }}

