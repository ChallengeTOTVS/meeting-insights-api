package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.insights.InsightsResponseDto;
import br.com.fiap.challengetotvsv2.exception.InsightsNotFoundException;
import br.com.fiap.challengetotvsv2.exception.ReuniaoNotFoundException;
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

    public InsightsResponseDto buscarInsightsPorReuniao(UUID reuniaoId){

        String emailUsuario = SecurityContextHolder.getContext().getAuthentication().getName();

        UsuarioEntity usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(()-> new RuntimeException("Usuario autenticado não encontrado"));

        ReuniaoEntity reuniao = reuniaoRepository.findByIdAndUsuarioId(reuniaoId, usuario.getId())
                .orElseThrow(()-> new ReuniaoNotFoundException(reuniaoId));

        InsightEntity insights = insightRepository.findByReuniaoId(reuniao.getId())
                .orElseThrow(()-> new InsightsNotFoundException(reuniaoId));

        return converterParaResponse(insights);
    }

    private InsightsResponseDto converterParaResponse(InsightEntity insights){
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

    public InsightsResponseDto gerarInsightsFicticios(UUID reuniaoId) {

        String emailUsuario = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        UsuarioEntity usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() ->
                        new RuntimeException("Usuário autenticado não encontrado"));

        ReuniaoEntity reuniao = reuniaoRepository
                .findByIdAndUsuarioId(reuniaoId, usuario.getId())
                .orElseThrow(() ->
                        new ReuniaoNotFoundException(reuniaoId));

        InsightEntity insights = InsightEntity.builder()
                .reuniao(reuniao)
                .scoreChurn(0.75)
                .nivelRisco("ALTO")
                .justificativaRisco(
                        "O cliente demonstrou insatisfação com o serviço e apresentou dúvidas sobre a continuidade do contrato."
                )
                .sentimentoGeral("NEGATIVO")
                .resumo(
                        "O cliente apresentou preocupações relacionadas ao serviço e ao custo da solução."
                )
                .topicosPrincipais(
                        "Preço, suporte, qualidade do serviço e renovação do contrato"
                )
                .objecoes(
                        "Custo elevado e insatisfação com o suporte"
                )
                .acoesRecomendadas(
                        "Agendar reunião com o cliente, apresentar alternativas comerciais e elaborar plano de melhoria do suporte."
                )
                .dataCriacao(LocalDateTime.now())
                .build();

        InsightEntity insightsSalvos = insightRepository.save(insights);

        return converterParaResponse(insightsSalvos);
    }
}
