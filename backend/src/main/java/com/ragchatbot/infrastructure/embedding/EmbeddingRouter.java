package com.ragchatbot.infrastructure.embedding;

import com.ragchatbot.config.EmbeddingProperties;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.port.EmbeddingService;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingRouter {

    private final Map<EmbeddingModel, EmbeddingService> providers;
    private final EmbeddingProperties properties;

    public EmbeddingRouter(List<EmbeddingService> providers, EmbeddingProperties properties) {
        this.properties = Objects.requireNonNull(properties);
        this.providers = new EnumMap<>(EmbeddingModel.class);

        for (EmbeddingService provider : providers) {
            EmbeddingService existing = this.providers.putIfAbsent(provider.supportedModel(), provider);
            if (existing != null) {
                throw new IllegalStateException("Duplicate embedding provider for model " + provider.supportedModel());
            }
        }
    }

    public EmbeddingModel defaultModel() {
        return properties.getDefaultModel();
    }

    public EmbeddingService providerFor(EmbeddingModel model) {
        EmbeddingService provider = providers.get(model);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported embedding model: " + model);
        }
        return provider;
    }

    public List<Float> embed(EmbeddingModel model, String text) {
        return providerFor(model).embed(text);
    }

    public List<List<Float>> embedAll(EmbeddingModel model, List<String> texts) {
        return providerFor(model).embedAll(texts);
    }
}
