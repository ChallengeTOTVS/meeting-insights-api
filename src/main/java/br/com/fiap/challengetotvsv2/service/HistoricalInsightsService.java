package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.insights.*;
import br.com.fiap.challengetotvsv2.exception.ReuniaoNotFoundException;
import br.com.fiap.challengetotvsv2.exception.UsuarioAutenticadoNotFoundException;
import br.com.fiap.challengetotvsv2.model.InsightEntity;
import br.com.fiap.challengetotvsv2.model.ReuniaoEntity;
import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import br.com.fiap.challengetotvsv2.repository.IInsightRepository;
import br.com.fiap.challengetotvsv2.repository.IReuniaoRepository;
import br.com.fiap.challengetotvsv2.repository.IUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HistoricalInsightsService {

    private final IReuniaoRepository reuniaoRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IInsightRepository insightRepository;
    private final RagService ragService;
    private final HistoricalAnalysisIaService historicalAnalysisIaService;

    public HistoricalInsightsResponseDto gerarHistorico(UUID reuniaoId) {
        UsuarioEntity usuario = usuarioAutenticado();
        ReuniaoEntity reuniao = reuniaoRepository.findByIdAndUsuarioId(reuniaoId, usuario.getId())
                .orElseThrow(() -> new ReuniaoNotFoundException(reuniaoId));

        List<HistoricoReuniaoDto> evolucaoScores = insightRepository
                .findHistoricoComReuniaoPorClienteEUsuario(
                        reuniao.getCliente().getId(), usuario.getId())
                .stream()
                .filter(insight -> insight.getReuniao().getDataReuniao().isBefore(reuniao.getDataReuniao()))
                .map(this::paraHistoricoReuniao)
                .toList();

        List<Document> contexto = ragService.buscarContextoHistorico(reuniao);
        if (contexto.isEmpty()) {
            return new HistoricalInsightsResponseDto(
                    reuniao.getId(),
                    reuniao.getCliente().getId(),
                    false,
                    "Ainda não há contexto histórico indexado e relevante para este cliente.",
                    evolucaoScores,
                    null,
                    List.of()
            );
        }

        HistoricalAnalysisDto analise = historicalAnalysisIaService.analisar(reuniao, contexto);
        var evidencias = new java.util.ArrayList<>(contexto.stream().map(this::paraEvidencia).toList());
        evidencias.add(new HistoricalEvidenceDto("ATUAL", reuniao.getId(), reuniao.getDataReuniao(),
                reuniao.getTitulo(), reuniao.getTranscricao()));
        HistoricalAnalysisValidator.validate(analise, evidencias);
        return new HistoricalInsightsResponseDto(
                reuniao.getId(),
                reuniao.getCliente().getId(),
                true,
                "Análise gerada com contexto histórico semanticamente relevante.",
                evolucaoScores,
                analise,
                evidencias
        );
    }

    private UsuarioEntity usuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(UsuarioAutenticadoNotFoundException::new);
    }

    private HistoricoReuniaoDto paraHistoricoReuniao(InsightEntity insight) {
        return new HistoricoReuniaoDto(
                insight.getReuniao().getId(),
                insight.getReuniao().getDataReuniao(),
                insight.getScoreChurn(),
                insight.getSentimentoGeral()
        );
    }

    private HistoricalEvidenceDto paraEvidencia(Document documento) {
        var metadata = documento.getMetadata();
        return new HistoricalEvidenceDto(
                metadata.get("referencia").toString(),
                UUID.fromString(metadata.get("reuniaoId").toString()),
                LocalDateTime.parse(metadata.get("dataReuniao").toString()),
                metadata.get("titulo").toString(),
                documento.getText()
        );
    }
}
