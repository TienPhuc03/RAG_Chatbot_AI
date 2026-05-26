package com.ragchatbot;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragchatbot.config.GeminiConfiguration;
import com.ragchatbot.config.GeminiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class GeminiPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(GeminiConfiguration.class)
            .withPropertyValues(
                    "gemini.api-key=test-api-key",
                    "gemini.chat-model=gemini-2.5-pro",
                    "gemini.embedding-model=gemini-embedding-001",
                    "gemini.timeout=45s",
                    "gemini.max-retries=5"
            );

    @Test
    void bindsGeminiPropertiesFromConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(GeminiProperties.class);

            GeminiProperties properties = context.getBean(GeminiProperties.class);
            assertThat(properties.getApiKey()).isEqualTo("test-api-key");
            assertThat(properties.getChatModel()).isEqualTo("gemini-2.5-pro");
            assertThat(properties.getEmbeddingModel()).isEqualTo("gemini-embedding-001");
            assertThat(properties.getTimeout()).hasSeconds(45);
            assertThat(properties.getMaxRetries()).isEqualTo(5);
        });
    }
}
