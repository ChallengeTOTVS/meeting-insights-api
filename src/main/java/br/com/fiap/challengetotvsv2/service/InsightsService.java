package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.insights.AnaliseReuniaoDto;
import br.com.fiap.challengetotvsv2.dto.insights.InsightsResponseDto;
import br.com.fiap.challengetotvsv2.enums.StatusReuniao;
import br.com.fiap.challengetotvsv2.exception.*;
import br.com.fiap.challengetotvsv2.model.InsightEntity;
import br.com.fiap.challengetotvsv2.model.ReuniaoEntity;
import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import br.com.fiap.challengetotvsv2.repository.IInsightRepository;
import br.com.fiap.challengetotvsv2.repository.IReuniaoRepository;
import br.com.fiap.challengetotvsv2.repository.IUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InsightsService {

    private final IInsightRepository insightRepository;
    private final IReuniaoRepository reuniaoRepository;
    private final IUsuarioRepository usuarioRepository;
    private final AnaliseIaService analiseIaService;

    public InsightsResponseDto buscarInsightsPorReuniao(UUID reuniaoId) {

        String emailUsuario = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        UsuarioEntity usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() ->
                        new InsightsAlreadyExistException(reuniaoId));

        ReuniaoEntity reuniao = reuniaoRepository
                .findByIdAndUsuarioId(reuniaoId, usuario.getId())
                .orElseThrow(() ->
                        new ReuniaoNotFoundException(reuniaoId));

        InsightEntity insights = insightRepository
                .findByReuniaoId(reuniao.getId())
                .orElseThrow(() ->
                        new InsightsNotFoundException(reuniaoId));

        return converterParaResponse(insights);
    }

    public InsightsResponseDto gerarInsights(UUID reuniaoId) {

        String emailUsuario = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        UsuarioEntity usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(UsuarioAutenticadoNotFoundException::new);

        ReuniaoEntity reuniao = reuniaoRepository
                .findByIdAndUsuarioId(reuniaoId, usuario.getId())
                .orElseThrow(() ->
                        new ReuniaoNotFoundException(reuniaoId));

        if (insightRepository.findByReuniaoId(reuniaoId).isPresent()) {
            throw new RuntimeException(
                    "Insights já foram gerados para esta reunião"
            );
        }
        if (reuniao.getTranscricao() == null ||
                reuniao.getTranscricao().isBlank()) {

            throw new TranscricaoVaziaException();
        }

        reuniao.setStatus(StatusReuniao.PROCESSANDO);
        reuniaoRepository.save(reuniao);

        try {
            AnaliseReuniaoDto analise = analiseIaService.analisar(reuniao.getTranscricao());

            InsightEntity insights = InsightEntity.builder()
                    .reuniao(reuniao)
                    .scoreChurn(analise.scoreChurn())
                    .nivelRisco(analise.nivelRisco())
                    .justificativaRisco(analise.justificativaRisco())
                    .sentimentoGeral(analise.sentimentoGeral())
                    .resumo(analise.resumo())
                    .topicosPrincipais(analise.topicosPrincipais())
                    .objecoes(analise.objecoes())
                    .acoesRecomendadas(analise.acoesRecomendadas())
                    .dataCriacao(LocalDateTime.now())
                    .build();

            InsightEntity insightsSalvos =
                    insightRepository.save(insights);

            reuniao.setStatus(StatusReuniao.CONCLUIDA);
            reuniaoRepository.save(reuniao);

            return converterParaResponse(insightsSalvos);
        }catch(Exception e) {
            reuniao.setStatus(StatusReuniao.ERRO);
            reuniaoRepository.save(reuniao);

            throw e;
        }
    }

    private InsightsResponseDto converterParaResponse(
            InsightEntity insights
    ) {
        return InsightsResponseDto.builder()
                .id(insights.getId())
                .reuniaoId(insights.getReuniao().getId())
                .scoreChurn(insights.getScoreChurn())
                .nivelRisco(insights.getNivelRisco())
                .justificativaRisco(insights.getJustificativaRisco())
                .sentimentoGeral(insights.getSentimentoGeral())
                .resumo(insights.getResumo())
                .topicosPrincipais(insights.getTopicosPrincipais())
                .objecoes(insights.getObjecoes())
                .acoesRecomendadas(insights.getAcoesRecomendadas())
                .dataCriacao(insights.getDataCriacao())
                .build();
    }
}