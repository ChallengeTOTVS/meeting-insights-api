package br.com.fiap.challengetotvsv2.controller;

import br.com.fiap.challengetotvsv2.dto.chat.AssistantChatRequestDto;
import br.com.fiap.challengetotvsv2.dto.chat.AssistantChatResponseDto;
import br.com.fiap.challengetotvsv2.service.AssistantChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/assistente")
public class AssistantChatController {
    private final AssistantChatService assistantChatService;

    @PostMapping("/chat")
    public AssistantChatResponseDto responder(@Valid @RequestBody AssistantChatRequestDto request) {
        return assistantChatService.responder(request);
    }
}
