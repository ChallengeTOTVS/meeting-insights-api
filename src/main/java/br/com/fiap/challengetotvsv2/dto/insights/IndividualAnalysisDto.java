package br.com.fiap.challengetotvsv2.dto.insights;

import java.util.List;

/** Transcript-only evaluation. Missing on legacy analyses, never generated during reads. */
public record IndividualAnalysisDto(
        int versao, Estado estado, String vendedor, List<String> evidenciasVendedor,
        Conclusao resultadoReuniao, Conclusao situacaoCliente,
        List<Interacao> interacoes, List<Compromisso> compromissos,
        Categoria relacionamento, Desempenho desempenho, List<Evidencia> evidencias) {
    public enum Estado { CONCLUIDO, SEM_ACHADOS, EVIDENCIAS_INSUFICIENTES }
    public enum Resposta { COMPLETA, PARCIAL, NAO_RESPONDIDA, NAO_SE_APLICA, NAO_FOI_POSSIVEL_AVALIAR }
    public enum Situacao { RESOLVIDO, PENDENTE, ACOMPANHAMENTO, NAO_FOI_POSSIVEL_AVALIAR }
    public record Evidencia(String id, String trecho) {}
    public record Conclusao(String texto, List<String> referencias) {}
    public record Interacao(String id, String assuntoOuPergunta, String respostaVendedor,
                            Resposta resposta, Situacao situacao, String tratamentoObjecao,
                            List<String> referencias) {}
    public record Compromisso(String descricao, String responsavel, String prazo,
                              Situacao situacao, String interacaoId, List<String> referencias) {}
    public record Categoria(Estado estado, List<Conclusao> itens) {}
    public record Desempenho(Estado estado, List<Conclusao> analise,
                             List<Conclusao> pontosFortes, List<Conclusao> melhorias,
                             List<Conclusao> antesDaProximaReuniao, List<String> pendenciasPrioritarias) {}
}
