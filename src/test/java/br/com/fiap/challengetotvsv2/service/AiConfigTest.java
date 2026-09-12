package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.config.AiConfig;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.mockito.ArgumentCaptor;
import java.time.Duration;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class AiConfigTest {
    @Test
    void explicitRequestTimeoutPreservesModelAndDisablesRetries() {
        var model = mock(OpenAiChatModel.class);
        when(model.getDefaultOptions()).thenReturn(OpenAiChatOptions.builder()
                .model("configured-model").temperature(0.3).timeout(Duration.ofSeconds(60)).build());
        var builder = mock(ChatClient.Builder.class, RETURNS_SELF);
        new AiConfig().chatClient(builder, model, Duration.ofSeconds(180));
        var captor = ArgumentCaptor.forClass(ChatOptions.Builder.class);
        verify(builder).defaultOptions(captor.capture());
        var options = (OpenAiChatOptions) captor.getValue().build();
        assertThat(options.getTimeout()).isEqualTo(Duration.ofSeconds(180));
        assertThat(options.getMaxRetries()).isZero();
        assertThat(options.getModel()).isEqualTo("configured-model");
        assertThat(options.getTemperature()).isEqualTo(0.3);
    }
}