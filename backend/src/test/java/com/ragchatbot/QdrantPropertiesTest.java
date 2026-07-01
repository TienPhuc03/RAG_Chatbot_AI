package com.ragchatbot;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragchatbot.config.QdrantConfiguration;
import com.ragchatbot.config.QdrantProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class QdrantPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(QdrantConfiguration.class)
            .withPropertyValues(
                    "rag.vector-store.qdrant-host=qdrant.local",
                    "rag.vector-store.qdrant-port=6334",
                    "rag.vector-store.collection-name=chunks",
                    "rag.vector-store.vector-size=1024",
                    "rag.vector-store.use-tls=true",
                    "rag.vector-store.request-timeout=12s"
            );

    @Test
    void bindsQdrantPropertiesFromConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(QdrantProperties.class);

            QdrantProperties properties = context.getBean(QdrantProperties.class);
            assertThat(properties.getQdrantHost()).isEqualTo("qdrant.local");
            assertThat(properties.getQdrantPort()).isEqualTo(6334);
            assertThat(properties.getCollectionName()).isEqualTo("chunks");
            assertThat(properties.getVectorSize()).isEqualTo(1024);
            assertThat(properties.isUseTls()).isTrue();
            assertThat(properties.getRequestTimeout()).hasSeconds(12);
        });
    }

    @Test
    void usesExpectedDefaultsWhenValuesAreMissing() {
        new ApplicationContextRunner()
                .withUserConfiguration(QdrantConfiguration.class)
                .run(context -> {
                    QdrantProperties properties = context.getBean(QdrantProperties.class);
                    assertThat(properties.getQdrantHost()).isEqualTo("localhost");
                    assertThat(properties.getQdrantPort()).isEqualTo(6334);
                    assertThat(properties.getCollectionName()).isEqualTo("rag_chunks");
                    assertThat(properties.getVectorSize()).isEqualTo(3072);
                    assertThat(properties.isUseTls()).isFalse();
                    assertThat(properties.getRequestTimeout()).hasSeconds(30);
                });
    }
}
