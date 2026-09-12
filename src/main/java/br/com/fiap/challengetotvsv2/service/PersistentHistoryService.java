package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.insights.*;
import br.com.fiap.challengetotvsv2.exception.ReuniaoNotFoundException;
import br.com.fiap.challengetotvsv2.exception.UsuarioAutenticadoNotFoundException;
import br.com.fiap.challengetotvsv2.model.ReuniaoEntity;
import br.com.fiap.challengetotvsv2.repository.*;
import jakarta.annotation.PreDestroy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.*;

@Service
public class PersistentHistoryService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final HistoricalInsightsService generator;
    private final IUsuarioRepository users;
    private final IReuniaoRepository meetings;
    private final IInsightRepository insights;
    private final ExecutorService executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(20), new ThreadPoolExecutor.AbortPolicy());

    public record State(String status, HistoricalInsightsResponseDto resultado, String mensagem) {}
    private record Job(String status, String resultado) {}

    public PersistentHistoryService(JdbcTemplate jdbc, ObjectMapper mapper, HistoricalInsightsService generator,
            IUsuarioRepository users, IReuniaoRepository meetings, IInsightRepository insights) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.generator = generator;
        this.users = users;
        this.meetings = meetings;
        this.insights = insights;
    }

    @jakarta.annotation.PostConstruct
    void initializeStorage() {
        // Existing installations may not enable Flyway auto-configuration.
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ClassPathResource("db/migration/V2__historical_analysis_jobs.sql"))
                .execute(java.util.Objects.requireNonNull(jdbc.getDataSource()));
    }

    private ReuniaoEntity authorized(UUID id) {
        var user = users.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(UsuarioAutenticadoNotFoundException::new);
        return meetings.findByIdAndUsuarioId(id, user.getId())
                .orElseThrow(() -> new ReuniaoNotFoundException(id));
    }

    public State consultar(UUID id) {
        var meeting = authorized(id);
        var jobs = jdbc.query("SELECT CASE WHEN status='PROCESSANDO' AND criado_em < CURRENT_TIMESTAMP - INTERVAL '15 minutes' THEN 'INTERROMPIDO' ELSE status END AS status, resultado FROM historical_analysis_job WHERE reuniao_id = ?",
                (rs, n) -> new Job(rs.getString(1), rs.getString(2)), id);
        if (jobs.isEmpty()) return new State("NAO_GERADO", base(meeting), "A evolução disponível já pode ser consultada. A análise por IA é opcional.");
        var job = jobs.getFirst();
        var result = job.resultado() == null ? base(meeting)
                : mapper.readValue(job.resultado(), HistoricalInsightsResponseDto.class);
        return new State(job.status(), result, switch (job.status()) {
            case "PROCESSANDO" -> "Análise em andamento. Você pode sair desta página; o resultado será salvo.";
            case "ERRO", "INTERROMPIDO" -> "A análise não foi concluída. Novas gerações estão bloqueadas para evitar cobranças repetidas. Consulte os logs do servidor.";
            default -> "Resultado salvo. Consultar novamente não gera outra análise.";
        });
    }

    public State iniciar(UUID id) {
        var meeting = authorized(id);
        // A database primary key makes repeated clicks, tabs and HTTP retries idempotent.
        int claimed = jdbc.update("INSERT INTO historical_analysis_job (reuniao_id,status) VALUES (?, 'PROCESSANDO') ON CONFLICT (reuniao_id) DO NOTHING", id);
        if (claimed == 1) {
            try {
                Integer previous = jdbc.queryForObject("""
                    SELECT count(*) FROM reuniao r JOIN rag_index i ON i.reuniao_id=r.id
                    WHERE r.cliente_id=? AND r.usuario_id=? AND r.data_reuniao<?
                    """, Integer.class, meeting.getCliente().getId(), meeting.getUsuario().getId(), meeting.getDataReuniao());
                if (previous == null || previous == 0) {
                    save(id, base(meeting)); // No embeddings or LLM call when no indexed history exists.
                } else {
                    var auth = SecurityContextHolder.getContext().getAuthentication();
                    executor.execute(() -> {
                        var context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(auth);
                        SecurityContextHolder.setContext(context);
                        try {
                            save(id, generator.gerarHistorico(id));
                        } catch (Exception error) {
                            failed(id, error);
                        } finally {
                            SecurityContextHolder.clearContext();
                        }
                    });
                }
            } catch (Exception error) {
                failed(id, error);
            }
        }
        return consultar(id);
    }

    private HistoricalInsightsResponseDto base(ReuniaoEntity meeting) {
        var scores = insights.findHistoricoComReuniaoPorClienteEUsuario(
                meeting.getCliente().getId(), meeting.getUsuario().getId()).stream()
                .filter(i -> i.getReuniao().getDataReuniao().isBefore(meeting.getDataReuniao()))
                .map(i -> new HistoricoReuniaoDto(i.getReuniao().getId(), i.getReuniao().getDataReuniao(),
                        i.getScoreChurn(), i.getSentimentoGeral())).toList();
        return new HistoricalInsightsResponseDto(meeting.getId(), meeting.getCliente().getId(), false,
                "Evolução calculada a partir dos insights salvos. A análise semântica requer reuniões anteriores indexadas.",
                scores, null, List.of());
    }

    private void save(UUID id, HistoricalInsightsResponseDto result) {
        jdbc.update("UPDATE historical_analysis_job SET status='CONCLUIDO', resultado=?, atualizado_em=CURRENT_TIMESTAMP WHERE reuniao_id=?",
                mapper.writeValueAsString(result), id);
    }

    private void failed(UUID id, Exception error) {
        org.slf4j.LoggerFactory.getLogger(getClass()).error("Falha na análise histórica da reunião {}", id, error);
        jdbc.update("UPDATE historical_analysis_job SET status='ERRO', atualizado_em=CURRENT_TIMESTAMP WHERE reuniao_id=?", id);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
        // Never resubmit unfinished jobs automatically: the provider may already have charged.
    }
}
