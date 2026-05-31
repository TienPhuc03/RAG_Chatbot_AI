package com.ragchatbot.infrastructure.embedding;

import com.ragchatbot.config.GeminiProperties;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.port.EmbeddingService;
import com.ragchatbot.infrastructure.gemini.GeminiApiClient;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class GeminiEmbeddingService implements EmbeddingService {

    private final GeminiProperties geminiProperties;
    private final GeminiApiClient geminiApiClient;

    public GeminiEmbeddingService(GeminiProperties geminiProperties, GeminiApiClient geminiApiClient) {
        this.geminiProperties = Objects.requireNonNull(geminiProperties);
        this.geminiApiClient = Objects.requireNonNull(geminiApiClient);
    }

    @Override
    public EmbeddingModel supportedModel() {
        return EmbeddingModel.GEMINI_EMBEDDING_001;
    }

    @Override
    public List<Float> embed(String text) {
        return geminiApiClient.embedContent(geminiProperties.getEmbeddingModel(), text);
    }

    @Override
    public List<List<Float>> embedAll(List<String> texts) {
        return geminiApiClient.embedContents(geminiProperties.getEmbeddingModel(), texts);
    }
}