package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.insights.*;
import br.com.fiap.challengetotvsv2.enums.StatusReuniao;
import br.com.fiap.challengetotvsv2.exception.*;
import br.com.fiap.challengetotvsv2.model.*;
import br.com.fiap.challengetotvsv2.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InsightsService {
    private final IInsightRepository insightRepository;
    private final IReuniaoRepository reuniaoRepository;
    private final IUsuarioRepository usuarioRepository;
    private final AnaliseIaService analiseIaService;
    private final IndividualAnalysisJobs jobs;
    private final ObjectMapper mapper;
    private final RagService ragService;
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(InsightsService.class);
    public record State(String status, InsightsResponseDto resultado, String mensagem, String retryToken) {}

    private ReuniaoEntity authorized(UUID id) {
        var user = usuarioRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(UsuarioAutenticadoNotFoundException::new);
        return reuniaoRepository.findByIdAndUsuarioId(id, user.getId())
                .orElseThrow(() -> new ReuniaoNotFoundException(id));
    }
    /** Leitura: o dono da reunião ou um administrador (somente leitura). Gerar continua só com o dono. */
    private ReuniaoEntity readable(UUID id) {
        if (br.com.fiap.challengetotvsv2.security.AuthUtils.isAdmin()) {
            return reuniaoRepository.findComRelacoesById(id).orElseThrow(() -> new ReuniaoNotFoundException(id));
        }
        return authorized(id);
    }
    public InsightsResponseDto buscarInsightsPorReuniao(UUID id) {
        readable(id);
        return converterParaResponse(insightRepository.findByReuniaoId(id)
                .orElseThrow(() -> new InsightsNotFoundException(id)));
    }
    public State consultar(UUID id) {
        var meeting = readable(id);
        var saved = insightRepository.findByReuniaoId(id).orElse(null);
        String status = jobs.status(id);
        if (saved != null && saved.getAnaliseIndividualJson() != null && !"PROCESSANDO".equals(status)) status = "CONCLUIDO";
        if (status == null) status = saved != null ? "VERSAO_ANTIGA" : switch (meeting.getStatus()) {
            case PROCESSANDO -> "INTERROMPIDO";
            case ERRO -> "ERRO";
            case CONCLUIDA -> "INTERROMPIDO";
            default -> "NAO_GERADO";
        };
        return new State(status, saved == null ? null : converterParaResponse(saved), switch (status) {
            case "VERSAO_ANTIGA" -> "Avaliação indisponível nesta versão. Atualizar por IA é opcional e pode consumir tokens.";
            case "PROCESSANDO" -> "Análise em processamento. Consultar o andamento não chama IA.";
            case "ERRO" -> "A análise falhou. Você pode solicitar uma nova tentativa, que pode consumir tokens.";
            case "INTERROMPIDO" -> "Conclusão não confirmada. Consulte o andamento antes de gerar novamente.";
            case "NAO_GERADO" -> "Análise ainda não gerada.";
            default -> "Análise salva. Esta avaliação usa somente a reunião selecionada.";
        }, "ERRO".equals(status) ? jobs.retryToken(id) : null);
    }
    public InsightsResponseDto gerarInsights(UUID id) { return generate(id, false); }
    public InsightsResponseDto atualizarAvaliacao(UUID id) { return generate(id, true); }

    public InsightsResponseDto tentarNovamente(UUID id, String token) {
        if (token == null || token.isBlank()) throw conflict();
        return generate(id, true, token);
    }

    private InsightsResponseDto generate(UUID id, boolean upgrade) {
        return generate(id, upgrade, null);
    }
    private InsightsResponseDto generate(UUID id, boolean upgrade, String retryToken) {
        var meeting = authorized(id);
        var existing = insightRepository.findByReuniaoId(id).orElse(null);
        if (existing != null && (!upgrade || existing.getAnaliseIndividualJson() != null))
            return converterParaResponse(existing);
        if (upgrade && existing == null && retryToken == null) throw new InsightsNotFoundException(id);
        if (meeting.getTranscricao() == null || meeting.getTranscricao().isBlank()) throw new TranscricaoVaziaException();
        // Legacy interrupted jobs must not be silently resubmitted to a potentially charged provider.
        if (retryToken == null && existing == null && meeting.getStatus() != StatusReuniao.PENDENTE)
            throw conflict();
        // Persistent unique claim survives restarts and protects simultaneous requests across instances.
        if (!(retryToken == null ? jobs.claim(id) : jobs.retry(id, retryToken))) throw conflict();
        try {
            if (existing == null) {
                meeting.setStatus(StatusReuniao.PROCESSANDO);
                reuniaoRepository.save(meeting);
            }
            var analysis = IndividualAnalysisSanitizer.sanitize(analiseIaService.analisar(meeting.getTranscricao()), meeting.getTranscricao());
            var entity = existing == null ? InsightEntity.builder().reuniao(meeting)
                    .scoreChurn(analysis.scoreChurn()).nivelRisco(analysis.nivelRisco())
                    .justificativaRisco(analysis.justificativaRisco()).sentimentoGeral(analysis.sentimentoGeral())
                    .resumo(analysis.resumo()).topicosPrincipais(analysis.topicosPrincipais())
                    .objecoes(analysis.objecoes()).acoesRecomendadas(analysis.acoesRecomendadas())
                    .dataCriacao(LocalDateTime.now()).build() : existing;
            // Explicit legacy upgrade adds only the new evaluation. All original values are retained.
            entity.setAnaliseIndividualJson(mapper.writeValueAsString(analysis.analiseIndividual()));
            var saved = insightRepository.save(entity);
            meeting.setStatus(StatusReuniao.CONCLUIDA);
            reuniaoRepository.save(meeting);
            // The paid analysis is already saved. Vector indexing has its own failure boundary.
            disponibilizarNoHistorico(meeting);
            jobs.finish(id, "CONCLUIDO");
            return converterParaResponse(saved);
        } catch (Exception error) {
            jobs.finish(id, "ERRO");
            if (existing == null) {
                meeting.setStatus(StatusReuniao.ERRO);
                reuniaoRepository.save(meeting);
            }
            throw error;
        }
    }
    private void disponibilizarNoHistorico(ReuniaoEntity meeting) {
        try {
            // RagService checks the transcript hash before generating any embeddings.
            ragService.indexarReuniao(meeting);
        } catch (RuntimeException error) {
            LOGGER.warn("Insight salvo; indexação histórica pendente para reunião {}", meeting.getId());
        }
    }
    private Boolean disponibilidadeHistorico(ReuniaoEntity meeting) {
        try {
            return ragService.estaIndexada(meeting);
        } catch (RuntimeException error) {
            LOGGER.warn("Não foi possível consultar indexação histórica da reunião {}", meeting.getId());
            return null;
        }
    }
    private ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Já existe uma tentativa de análise. Consulte o andamento; uma nova geração está bloqueada.");
    }
    private InsightsResponseDto converterParaResponse(InsightEntity i) {
        var individual = i.getAnaliseIndividualJson() == null ? null
                : mapper.readValue(i.getAnaliseIndividualJson(), IndividualAnalysisDto.class);
        return InsightsResponseDto.builder().id(i.getId()).reuniaoId(i.getReuniao().getId())
                .scoreChurn(i.getScoreChurn()).nivelRisco(i.getNivelRisco()).justificativaRisco(i.getJustificativaRisco())
                .sentimentoGeral(i.getSentimentoGeral()).resumo(i.getResumo()).topicosPrincipais(i.getTopicosPrincipais())
                .objecoes(i.getObjecoes()).acoesRecomendadas(i.getAcoesRecomendadas()).dataCriacao(i.getDataCriacao())
                .historicoDisponivel(disponibilidadeHistorico(i.getReuniao()))
                .analiseIndividual(individual).estadoAvaliacao(individual == null ? "VERSAO_ANTIGA" : individual.estado().name()).build();
    }
}
