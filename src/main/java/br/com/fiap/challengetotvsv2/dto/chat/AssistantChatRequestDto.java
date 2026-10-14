package br.com.fiap.challengetotvsv2.dto.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AssistantChatRequestDto(@NotBlank @Size(max = 1200) String pergunta,
        @Valid @Size(max = 8) List<Mensagem> historico) {
    public record Mensagem(@NotBlank @Size(max = 1200) String papel,
                           @NotBlank @Size(max = 1200) String texto) {}
}
