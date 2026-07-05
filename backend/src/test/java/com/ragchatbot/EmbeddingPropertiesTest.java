package com.ragchatbot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchatbot.config.EmbeddingConfiguration;
import com.ragchatbot.config.EmbeddingProperties;
import com.ragchatbot.domain.enums.EmbeddingModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class EmbeddingPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                    "rag.embedding.default-model=BGE_M3",
                    "rag.embedding.provider-mode=ROUTED",
                    "rag.embedding.local-service.base-url=http://embedding-service:8000",
                    "rag.embedding.local-service.timeout=12s",
                    "rag.embedding.vector-dimensions.GEMINI_EMBEDDING_001=3072",
                    "rag.embedding.vector-dimensions.TEXT_EMBEDDING_3_SMALL=1536",
                    "rag.embedding.vector-dimensions.MULTILINGUAL_E5_BASE=768",
                    "rag.embedding.vector-dimensions.PHOBERT_BASE=768",
                    "rag.embedding.vector-dimensions.BGE_M3=1024"
            );

    @Test
    void bindsEmbeddingPropertiesFromConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(EmbeddingProperties.class);

            EmbeddingProperties properties = context.getBean(EmbeddingProperties.class);
            assertThat(properties.getDefaultModel()).isEqualTo(EmbeddingModel.BGE_M3);
            assertThat(properties.getProviderMode()).isEqualTo("ROUTED");
            assertThat(properties.getLocalService().getBaseUrl()).isEqualTo("http://embedding-service:8000");
            assertThat(properties.getLocalService().getTimeout()).hasSeconds(12);
            assertThat(properties.getVectorDimensions())
                    .containsEntry(EmbeddingModel.GEMINI_EMBEDDING_001, 3072)
                    .containsEntry(EmbeddingModel.TEXT_EMBEDDING_3_SMALL, 1536)
                    .containsEntry(EmbeddingModel.MULTILINGUAL_E5_BASE, 768)
                    .containsEntry(EmbeddingModel.PHOBERT_BASE, 768)
                    .containsEntry(EmbeddingModel.BGE_M3, 1024);
        });
    }

    @Test
    void startupValidationRejectsTextEmbedding3SmallAsDefaultModel() {
        new ApplicationContextRunner()
                .withUserConfiguration(EmbeddingConfiguration.class)
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withPropertyValues(
                        "rag.embedding.default-model=TEXT_EMBEDDING_3_SMALL",
                        "rag.embedding.local-service.base-url=http://embedding-service:8000"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("TEXT_EMBEDDING_3_SMALL da ngung ho tro");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EmbeddingProperties.class)
    static class TestConfig {
    }
}
