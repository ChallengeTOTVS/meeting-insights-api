package br.com.fiap.challengetotvsv2.dto.insights;

import java.util.List;

/** Optional on legacy JSON. Catalog metadata is attached by the backend, never by the model. */
public record ProductsAnalysisDto(
        Estado estado, Catalogo catalogo, List<Produto> produtos,
        List<Evidencia> evidencias, List<Achado> mudancasPercepcao) {
    public enum Estado { CONCLUIDO, SEM_MENCOES, EVIDENCIAS_INSUFICIENTES }
    public enum Tipo { TOTVS, CONCORRENTE, OUTRO_FORNECEDOR }
    public enum Relacao { UTILIZA_ATUALMENTE, UTILIZOU_ANTERIORMENTE, AVALIANDO, RECEBEU_PROPOSTA, APENAS_MENCIONOU, NAO_IDENTIFICADA }
    public enum Percepcao { POSITIVA, NEGATIVA, MISTA, NEUTRA, NAO_IDENTIFICADA }
    public enum Experiencia { BOA, RUIM, MISTA, NAO_RELATADA }
    public record Fonte(String url, String revisadoEm) {}
    public record Entrada(String id, String nome, String fornecedor, Tipo tipo,
                          List<String> apelidos, String segmento, List<Fonte> fontes) {}
    public record Concorrencia(String produtoId, String concorrenteId, String criterio,
                              String natureza, List<Fonte> fontes) {}
    public record Catalogo(String versao, List<Entrada> entradas, List<Concorrencia> concorrencias) {}
    public record Evidencia(String id, String referencia, String trecho, String participante, String dataReuniao) {}
    public record Achado(String texto, List<String> referencias) {}
    public record Observacao(String participante, Relacao relacao, Percepcao percepcao,
                             Experiencia experiencia, Achado contexto, List<Achado> aspectos,
                             List<String> referencias) {}
    public record Produto(String nome, String fornecedor, String catalogoId, Tipo tipo,
                          List<Observacao> observacoes, List<Achado> motivosConsideracao,
                          List<Achado> vantagens, List<Achado> dificuldades,
                          List<Achado> comparacoesCliente, List<Achado> obstaculosOportunidades,
                          List<Achado> recomendacoesVendedor) {}
}
