package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.insights.*;
import java.util.List;
import static br.com.fiap.challengetotvsv2.dto.insights.IndividualAnalysisDto.*;

final class IndividualFixtures {
    static final String TRANSCRIPT = "Ana (cliente): Qual o prazo? Bruno (vendedor): Entrega em 10 dias. Ana: Aprovado. Ana: Inclui migração? Bruno: Inclui arquivos; confirmarei o restante. Ana: Qual o preço? Bruno: Vou enviar amanhã.";
    static AnaliseReuniaoDto analysis() {
        var refs = List.of("E1");
        var conclusion = new Conclusao("A negociação tem respostas parciais e acompanhamento definido.", refs);
        var individual = new IndividualAnalysisDto(2, Estado.CONCLUIDO, "Bruno", refs,
                conclusion, conclusion,
                List.of(new Interacao("I1", "Qual o prazo?", "Entrega em 10 dias.", Resposta.COMPLETA, Situacao.RESOLVIDO, null, refs),
                        new Interacao("I2", "Inclui migração?", "Inclui arquivos; confirmarei o restante.", Resposta.PARCIAL, Situacao.ACOMPANHAMENTO, null, refs),
                        new Interacao("I3", "Qual o preço?", "Vou enviar amanhã.", Resposta.NAO_RESPONDIDA, Situacao.ACOMPANHAMENTO, null, refs)),
                List.of(new Compromisso("Enviar preço", "Bruno", "amanhã", Situacao.ACOMPANHAMENTO, "I3", refs)),
                new Categoria(Estado.SEM_ACHADOS, List.of()),
                new Desempenho(Estado.CONCLUIDO, List.of(conclusion), List.of(conclusion), List.of(conclusion), List.of(conclusion), List.of("I3", "I2")),
                List.of(new Evidencia("E1", TRANSCRIPT)));
        return new AnaliseReuniaoDto(0.3, "BAIXO", "Há acompanhamento", "NEUTRO", "Resumo", "Prazos", "", "Enviar preço", individual);
    }
    static AnaliseReuniaoDto withEvidence(String quote) {
        var base = analysis(); var a = base.analiseIndividual();
        return new AnaliseReuniaoDto(base.scoreChurn(), base.nivelRisco(), base.justificativaRisco(), base.sentimentoGeral(), base.resumo(), base.topicosPrincipais(), base.objecoes(), base.acoesRecomendadas(),
                new IndividualAnalysisDto(a.versao(), a.estado(), a.vendedor(), a.evidenciasVendedor(), a.resultadoReuniao(), a.situacaoCliente(), a.interacoes(), a.compromissos(), a.relacionamento(), a.desempenho(), List.of(new Evidencia("E1", quote))));
    }
}
