package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.insights.AnaliseReuniaoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnaliseIaService {

    private final ChatClient chatClient;

    public AnaliseReuniaoDto analisar(String transcricao){

        return chatClient.prompt().system("""
                 Você é um analista especializado em Customer Success,
                                        vendas B2B e análise de reuniões comerciais.
                
                                        Analise a transcrição da reunião e produza:
                
                                        - scoreChurn: número entre 0.0 e 1.0
                                        - nivelRisco: BAIXO, MEDIO ou ALTO
                                        - justificativaRisco
                                        - sentimentoGeral: POSITIVO, NEUTRO ou NEGATIVO
                                        - resumo
                                        - topicosPrincipais
                                        - objecoes
                                        - acoesRecomendadas
                
                                        Não invente informações que não estejam presentes
                                        na transcrição.
                """)
                .user("""
                        Analise a seguinte transcrição:
                        
                        %s
                        """.formatted(transcricao))
                .call()
                .entity(
                        AnaliseReuniaoDto.class,
                        spec ->spec
                                .useProviderStructuredOutput()
                                .validateSchema()
                );
    }
}
