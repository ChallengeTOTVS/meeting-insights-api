package br.com.fiap.challengetotvsv2.dto.cliente;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ClientContextResponseDto(UUID clienteId, long reunioesConcluidas, String estado,
        String mensagem, boolean divergenciasEncontradas, List<Fato> fatos) {
    public record Fato(String tipo, String assunto, String descricao, String situacao,
                       List<LocalDateTime> datasReuniao, boolean divergente) {}
}
