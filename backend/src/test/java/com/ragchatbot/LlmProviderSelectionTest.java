package com.ragchatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchatbot.config.GeminiProperties;
import com.ragchatbot.domain.port.LlmInferenceService;
import com.ragchatbot.infrastructure.gemini.GeminiApiClient;
import com.ragchatbot.infrastructure.llm.GeminiLlmInferenceService;
import com.ragchatbot.infrastructure.llm.OllamaLlmInferenceService;
import java.net.http.HttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class LlmProviderSelectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestBeans.class)
            .withPropertyValues(
                    "ollama.base-url=http://localhost:11434",
                    "ollama.model=hoc-phan-chatbot"
            );

    @Test
    void loadsGeminiServiceWhenProviderIsGemini() {
        contextRunner
                .withPropertyValues("rag.llm.provider=GEMINI")
                .run(context -> {
                    assertThat(context).hasSingleBean(LlmInferenceService.class);
                    assertThat(context.getBean(LlmInferenceService.class)).isInstanceOf(GeminiLlmInferenceService.class);
                });
    }

    @Test
    void loadsOllamaServiceWhenProviderIsOllama() {
        contextRunner
                .withPropertyValues("rag.llm.provider=OLLAMA")
                .run(context -> {
                    assertThat(context).hasSingleBean(LlmInferenceService.class);
                    assertThat(context.getBean(LlmInferenceService.class)).isInstanceOf(OllamaLlmInferenceService.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestBeans {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        GeminiProperties geminiProperties() {
            return new GeminiProperties();
        }

        @Bean
        GeminiApiClient geminiApiClient() {
            return mock(GeminiApiClient.class);
        }

        @Bean
        @ConditionalOnProperty(prefix = "rag.llm", name = "provider", havingValue = "GEMINI", matchIfMissing = true)
        LlmInferenceService geminiInferenceService(GeminiProperties geminiProperties, GeminiApiClient geminiApiClient) {
            return new GeminiLlmInferenceService(geminiProperties, geminiApiClient);
        }

        @Bean
        @ConditionalOnProperty(prefix = "rag.llm", name = "provider", havingValue = "OLLAMA")
        LlmInferenceService ollamaInferenceService(
                ObjectMapper objectMapper,
                @Value("${ollama.base-url}") String baseUrl,
                @Value("${ollama.model}") String modelName
        ) {
            return new OllamaLlmInferenceService(objectMapper, baseUrl, modelName, HttpClient.newHttpClient());
        }
    }
}
