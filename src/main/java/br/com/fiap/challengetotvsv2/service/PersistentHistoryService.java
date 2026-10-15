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
    private final RagService rag;
    private final IndividualAnalysisJobs individualJobs;
    private final ExecutorService executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(20), new ThreadPoolExecutor.AbortPolicy());

    public record State(String status, HistoricalInsightsResponseDto resultado, String mensagem, String etapa) {}
    private record Job(String status, String resultado, String etapa) {}

    public PersistentHistoryService(JdbcTemplate jdbc, ObjectMapper mapper, HistoricalInsightsService generator,
            IUsuarioRepository users, IReuniaoRepository meetings, IInsightRepository insights,
            RagService rag, IndividualAnalysisJobs individualJobs) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.generator = generator;
        this.users = users;
        this.meetings = meetings;
        this.insights = insights;
        this.rag = rag;
        this.individualJobs = individualJobs;
    }

    @jakarta.annotation.PostConstruct
    void initializeStorage() {
        // Existing installations may not enable Flyway auto-configuration.
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ClassPathResource("db/migration/V2__historical_analysis_jobs.sql"),
                new org.springframework.core.io.ClassPathResource("db/migration/V4__historical_progress.sql"))
                .execute(java.util.Objects.requireNonNull(jdbc.getDataSource()));
    }

    private ReuniaoEntity authorized(UUID id) {
        var user = users.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(UsuarioAutenticadoNotFoundException::new);
        return meetings.findByIdAndUsuarioId(id, user.getId())
                .orElseThrow(() -> new ReuniaoNotFoundException(id));
    }

    /** Leitura: o dono da reunião ou um administrador. Gerar continua restrito ao dono. */
    private ReuniaoEntity readable(UUID id) {
        if (br.com.fiap.challengetotvsv2.security.AuthUtils.isAdmin()) {
            return meetings.findComRelacoesById(id).orElseThrow(() -> new ReuniaoNotFoundException(id));
        }
        return authorized(id);
    }

    public State consultar(UUID id) {
        var meeting = readable(id);
        var jobs = jdbc.query("SELECT CASE WHEN status='PROCESSANDO' AND criado_em < CURRENT_TIMESTAMP - INTERVAL '15 minutes' THEN 'INTERROMPIDO' ELSE status END AS status, resultado, etapa FROM historical_analysis_job WHERE reuniao_id = ?",
                (rs, n) -> new Job(rs.getString(1), rs.getString(2), rs.getString(3)), id);
        if (jobs.isEmpty()) return new State("NAO_GERADO", base(meeting), "A evolução disponível já pode ser consultada. A análise por IA é opcional.", "AGUARDANDO");
        var job = jobs.getFirst();
        var result = job.resultado() == null ? base(meeting)
                : mapper.readValue(job.resultado(), HistoricalInsightsResponseDto.class);
        return new State(job.status(), result, switch (job.status()) {
            case "PROCESSANDO" -> "Análise em andamento. Você pode sair desta página; o resultado será salvo.";
            case "ERRO", "INTERROMPIDO" -> "A análise não foi concluída. Novas gerações estão bloqueadas para evitar cobranças repetidas. Consulte os logs do servidor.";
            default -> "Resultado salvo. Consultar novamente não gera outra análise.";
        }, job.etapa());
    }

    public State iniciar(UUID id) {
        var meeting = authorized(id);
        // Historical generation must follow the first saved individual insight and its indexing attempt.
        if (insights.findByReuniaoId(id).isEmpty() || "PROCESSANDO".equals(individualJobs.status(id))) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                    "Aguarde a conclusão dos insights individuais e da preparação do histórico.");
        }
        // Repeated clicks, tabs and HTTP retries cannot start another paid generation.
        int claimed = jdbc.update("INSERT INTO historical_analysis_job (reuniao_id,status,etapa) VALUES (?, 'PROCESSANDO', 'INDEXANDO_RAG') ON CONFLICT (reuniao_id) DO NOTHING", id);
        if (claimed == 1) {
            try {
                // Initialize metadata while the request's persistence context is still open.
                meeting.getCliente().getNome();
                var auth = SecurityContextHolder.getContext().getAuthentication();
                executor.execute(() -> {
                    var context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(auth);
                    SecurityContextHolder.setContext(context);
                    try {
                        progress(id, "INDEXANDO_RAG");
                        // Also repairs missing indexing for legacy insights, without regenerating the insight.
                        // The transcript hash makes an already indexed meeting a no-op.
                        rag.indexarReuniao(meeting);
                        progress(id, "ANALISANDO_HISTORICO");
                        Integer previous = jdbc.queryForObject("""
                            SELECT count(*) FROM reuniao r JOIN rag_index i ON i.reuniao_id=r.id
                            WHERE r.cliente_id=? AND r.usuario_id=? AND r.data_reuniao<?
                            """, Integer.class, meeting.getCliente().getId(), meeting.getUsuario().getId(), meeting.getDataReuniao());
                        var result = previous == null || previous == 0 ? base(meeting) : generator.gerarHistorico(id);
                        progress(id, "SALVANDO_RESULTADO");
                        save(id, result);
                    } catch (Exception error) {
                        failed(id, error);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });
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

    private void progress(UUID id, String stage) {
        jdbc.update("UPDATE historical_analysis_job SET etapa=?, atualizado_em=CURRENT_TIMESTAMP WHERE reuniao_id=? AND status='PROCESSANDO'", stage, id);
    }

    private void save(UUID id, HistoricalInsightsResponseDto result) {
        jdbc.update("UPDATE historical_analysis_job SET status='CONCLUIDO', etapa='CONCLUIDO', resultado=?, atualizado_em=CURRENT_TIMESTAMP WHERE reuniao_id=?",
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
