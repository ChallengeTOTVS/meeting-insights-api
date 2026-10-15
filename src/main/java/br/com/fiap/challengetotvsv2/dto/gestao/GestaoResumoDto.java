package br.com.fiap.challengetotvsv2.dto.gestao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tudo que a "Visão da equipe" do administrador precisa, já calculado no backend.
 * Risco sempre em 0–100 (percentual), ou null quando não avaliado.
 */
public record GestaoResumoDto(
        Totais totais,
        List<Conta> contas,
        List<Vendedor> vendedores,
        List<Sinal> concorrentes,
        List<Sinal> produtos,
        List<OportunidadeItem> oportunidades,
        List<Sinal> objecoes,
        LocalDateTime geradoEm) {

    public record Totais(int reunioes, int analisadas, int aguardandoAnalise, int comFalha, int clientes,
                         int contasAcompanhadas, int contasRiscoAlto, Integer riscoMedio,
                         int pendenciasAbertas, int oportunidades) {}

    /** Situação de cada cliente segundo a última reunião analisada. */
    public record Conta(UUID clienteId, String cliente, String empresa, String vendedor, UUID vendedorId,
                        UUID ultimaReuniaoId, String ultimaReuniao, LocalDateTime dataUltimaReuniao,
                        Integer risco, String nivelRisco, String sentimento, int pendenciasAbertas,
                        int oportunidades) {}

    public record Vendedor(UUID id, String nome, String email, int reunioes, int analisadas, int contas,
                           int contasRiscoAlto, Integer riscoMedio, int pendenciasAbertas, int oportunidades) {}

    /** Algo citado em várias reuniões (concorrente, produto TOTVS, objeção). */
    public record Sinal(String nome, int mencoes, List<String> clientes, Map<String, Integer> contextos) {}

    public record OportunidadeItem(UUID reuniaoId, String reuniao, String cliente, String vendedor,
                                   LocalDateTime data, String descricao, String tipo, String produto) {}
}
