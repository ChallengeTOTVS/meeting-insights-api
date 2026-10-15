package br.com.fiap.challengetotvsv2.dto.chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Resposta do assistente + as reuniões em que ela se baseou (podem estar vazias). */
public record AssistantChatResponseDto(String resposta, List<Fonte> fontes) {
    public AssistantChatResponseDto(String resposta) {
        this(resposta, List.of());
    }

    public record Fonte(UUID reuniaoId, String titulo, String cliente, LocalDateTime data) {}
}
