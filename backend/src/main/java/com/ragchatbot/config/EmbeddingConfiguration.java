package com.ragchatbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.port.EmbeddingService;
import com.ragchatbot.infrastructure.embedding.LocalHttpEmbeddingService;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EmbeddingProperties.class)
public class EmbeddingConfiguration {

    @Bean
    EmbeddingService multilingualE5BaseEmbeddingService(
            EmbeddingProperties properties,
            ObjectMapper objectMapper
    ) {
        return new LocalHttpEmbeddingService(
                EmbeddingModel.MULTILINGUAL_E5_BASE,
                "multilingual-e5-base",
                properties,
                objectMapper
        );
    }

    @Bean
    EmbeddingService phoBertBaseEmbeddingService(
            EmbeddingProperties properties,
            ObjectMapper objectMapper
    ) {
        return new LocalHttpEmbeddingService(
                EmbeddingModel.PHOBERT_BASE,
                "phobert-base",
                properties,
                objectMapper
        );
    }

    @Bean
    EmbeddingService bgeM3EmbeddingService(
            EmbeddingProperties properties,
            ObjectMapper objectMapper
    ) {
        return new LocalHttpEmbeddingService(
                EmbeddingModel.BGE_M3,
                "bge-m3",
                properties,
                objectMapper
        );
    }

    @Bean
    SmartInitializingSingleton embeddingModelStartupValidator(EmbeddingProperties properties) {
        return () -> {
            if (properties.getDefaultModel() == EmbeddingModel.TEXT_EMBEDDING_3_SMALL) {
                throw new IllegalStateException(
                        "RAG_EMBEDDING_DEFAULT_MODEL=TEXT_EMBEDDING_3_SMALL da ngung ho tro. "
                                + "Hay doi sang GEMINI_EMBEDDING_001, MULTILINGUAL_E5_BASE, PHOBERT_BASE hoac BGE_M3."
                );
            }
        };
    }
}