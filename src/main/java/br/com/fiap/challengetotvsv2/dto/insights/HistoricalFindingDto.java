package br.com.fiap.challengetotvsv2.dto.insights;

import java.util.List;

public record HistoricalFindingDto(
        String titulo,
        String descricao,
        List<String> referencias,
        String responsavel,
        String prazo,
        String situacao
) {
}
