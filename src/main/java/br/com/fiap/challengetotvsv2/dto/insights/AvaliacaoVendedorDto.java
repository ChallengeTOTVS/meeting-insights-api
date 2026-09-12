package br.com.fiap.challengetotvsv2.dto.insights;

import java.util.List;

public record AvaliacaoVendedorDto(
        Double qualidadeAtual,
        Double qualidadeAnterior,
        Double variacao,
        String justificativa,
        List<String> melhorias,
        List<String> pontosAtencao,
        List<String> referencias
) {
}
