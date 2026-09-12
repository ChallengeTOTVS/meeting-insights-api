package br.com.fiap.challengetotvsv2.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class AiConfig {
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, OpenAiChatModel model,
            @Value("${spring.ai.openai.chat.timeout:180s}") Duration timeout) {
        // RequestOptions overrides the HTTP client's timeout in Spring AI 2.0.1.
        // Preserve the configured model and provider settings when setting the request timeout.
        var options = ((OpenAiChatOptions) model.getDefaultOptions()).mutate()
                .timeout(timeout)
                .maxRetries(0);
        return builder.defaultOptions(options).build();
    }
}