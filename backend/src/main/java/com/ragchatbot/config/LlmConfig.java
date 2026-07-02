package com.ragchatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.llm")
public record LlmConfig(

        String provider,

        String ollamaBaseUrl,

        String ollamaModel,

        String geminiApiKey,

        String geminiChatModel,

        String geminiEmbeddingModel,

        String geminiTimeout,

        Integer geminiMaxRetries
) {
}
