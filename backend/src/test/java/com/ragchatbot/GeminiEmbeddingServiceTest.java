package com.ragchatbot;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragchatbot.config.GeminiProperties;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.infrastructure.embedding.GeminiEmbeddingService;
import com.ragchatbot.infrastructure.gemini.GeminiApiClient;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeminiEmbeddingServiceTest {

    @Test
    void delegatesEmbeddingCallsToGeminiClient() {
        GeminiProperties properties = new GeminiProperties();
        properties.setEmbeddingModel("gemini-embedding-001");

        GeminiApiClient fakeClient = new GeminiApiClient() {
            @Override
            public String generateContent(String modelName, String prompt) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<Float> embedContent(String modelName, String text) {
                assertThat(modelName).isEqualTo("gemini-embedding-001");
                assertThat(text).isEqualTo("Java la gi?");
                return List.of(0.1f, 0.2f, 0.3f);
            }

            @Override
            public List<List<Float>> embedContents(String modelName, List<String> texts) {
                assertThat(modelName).isEqualTo("gemini-embedding-001");
                assertThat(texts).containsExactly("A", "B");
                return List.of(List.of(1.0f), List.of(2.0f));
            }
        };

        GeminiEmbeddingService service = new GeminiEmbeddingService(properties, fakeClient);

        assertThat(service.supportedModel()).isEqualTo(EmbeddingModel.GEMINI_EMBEDDING_001);
        assertThat(service.embed("Java la gi?")).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(service.embedAll(List.of("A", "B"))).containsExactly(List.of(1.0f), List.of(2.0f));
    }
}
