package com.ragchatbot.infrastructure.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ragchatbot.config.EmbeddingProperties;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.port.EmbeddingService;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmbeddingRouterTest {

    @Test
    void routesToProviderMatchingRequestedModel() {
        EmbeddingRouter router = new EmbeddingRouter(
                List.of(
                        new StubEmbeddingService(EmbeddingModel.GEMINI_EMBEDDING_001),
                        new StubEmbeddingService(EmbeddingModel.MULTILINGUAL_E5_BASE),
                        new StubEmbeddingService(EmbeddingModel.TEXT_EMBEDDING_3_SMALL)
                ),
                new EmbeddingProperties()
        );

        assertThat(router.providerFor(EmbeddingModel.GEMINI_EMBEDDING_001).supportedModel())
                .isEqualTo(EmbeddingModel.GEMINI_EMBEDDING_001);
        assertThat(router.providerFor(EmbeddingModel.MULTILINGUAL_E5_BASE).supportedModel())
                .isEqualTo(EmbeddingModel.MULTILINGUAL_E5_BASE);
        assertThat(router.providerFor(EmbeddingModel.TEXT_EMBEDDING_3_SMALL).supportedModel())
                .isEqualTo(EmbeddingModel.TEXT_EMBEDDING_3_SMALL);
    }

    @Test
    void failsFastWhenRequestedModelHasNoProvider() {
        EmbeddingRouter router = new EmbeddingRouter(
                List.of(new StubEmbeddingService(EmbeddingModel.GEMINI_EMBEDDING_001)),
                new EmbeddingProperties()
        );

        assertThatThrownBy(() -> router.providerFor(EmbeddingModel.BGE_M3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported embedding model");
    }

    private record StubEmbeddingService(EmbeddingModel supportedModel) implements EmbeddingService {

        @Override
        public List<Float> embed(String text) {
            return List.of(0.1f);
        }

        @Override
        public List<List<Float>> embedAll(List<String> texts) {
            return List.of(List.of(0.1f));
        }
    }
}
