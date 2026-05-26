package com.ragchatbot.config;

import com.ragchatbot.infrastructure.gemini.GeminiApiClient;
import com.ragchatbot.infrastructure.gemini.GoogleGenAiGeminiApiClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfiguration {

    @Bean
    GeminiApiClient geminiApiClient(GeminiProperties geminiProperties) {
        return new GoogleGenAiGeminiApiClient(geminiProperties);
    }
}