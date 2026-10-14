package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.chat.AssistantChatRequestDto;
import br.com.fiap.challengetotvsv2.repository.IInsightRepository;
import br.com.fiap.challengetotvsv2.repository.IReuniaoRepository;
import br.com.fiap.challengetotvsv2.repository.IUsuarioRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AssistantChatServiceTest {
    @Test void rejectsOutOfScopeQuestionBeforeReadingMeetingsOrCallingAi() {
        org.springframework.ai.chat.client.ChatClient ai = mock(org.springframework.ai.chat.client.ChatClient.class);
        IUsuarioRepository users = mock(IUsuarioRepository.class);
        IReuniaoRepository meetings = mock(IReuniaoRepository.class);
        var service = new AssistantChatService(ai, users, meetings, mock(IInsightRepository.class), JsonMapper.builder().build());

        var result = service.responder(new AssistantChatRequestDto("Qual é a capital do Japão?", List.of()));

        assertThat(result.resposta()).contains("uso do Meeting Insights");
        verifyNoInteractions(ai, users, meetings);
    }

    @Test void refusesResponseWhenModelClassifiesItOutOfScopeAndRemovesMarkdownFormatting() {
        var denied = AssistantChatService.safeResponse(new br.com.fiap.challengetotvsv2.dto.chat.AssistantChatModelResponse(false, "Resposta indevida"));
        assertThat(denied.resposta()).contains("uso do Meeting Insights").doesNotContain("Resposta indevida");

        var allowed = AssistantChatService.safeResponse(new br.com.fiap.challengetotvsv2.dto.chat.AssistantChatModelResponse(true, "**Resposta** sobre a reunião"));
        assertThat(allowed.resposta()).isEqualTo("Resposta sobre a reunião");
    }

    @Test void sendsMeetingContextOnlyForMeetingSpecificQuestions() {
        assertThat(AssistantChatService.needsMeetingContext("Como cadastrar um cliente?")).isFalse();
        assertThat(AssistantChatService.needsMeetingContext("O que ficou combinado na reunião com Santa Clara?")).isTrue();
        assertThat(AssistantChatService.needsMeetingContext("Qual preço foi combinado?")).isTrue();
    }
}
