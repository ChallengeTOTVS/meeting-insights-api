package br.com.fiap.challengetotvsv2.dto.chat;

import java.util.List;

/**
 * Formato que a IA devolve (saída estruturada).
 * reunioesCitadas: códigos (R1, R2...) das reuniões usadas na resposta, para o front mostrar os links.
 */
public record AssistantChatModelResponse(boolean dentroDoEscopo, String resposta, List<String> reunioesCitadas) {
    public AssistantChatModelResponse(boolean dentroDoEscopo, String resposta) {
        this(dentroDoEscopo, resposta, List.of());
    }
}
