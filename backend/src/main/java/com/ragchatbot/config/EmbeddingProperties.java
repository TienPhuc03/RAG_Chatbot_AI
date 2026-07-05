package com.ragchatbot.config;

import com.ragchatbot.domain.enums.EmbeddingModel;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.embedding")
public class EmbeddingProperties {

    private EmbeddingModel defaultModel = EmbeddingModel.GEMINI_EMBEDDING_001;
    private String providerMode = "ROUTED";
    private LocalService localService = new LocalService();
    private Map<EmbeddingModel, Integer> vectorDimensions = defaultVectorDimensions();

    public EmbeddingModel getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(EmbeddingModel defaultModel) {
        this.defaultModel = defaultModel;
    }

    public String getProviderMode() {
        return providerMode;
    }

    public void setProviderMode(String providerMode) {
        this.providerMode = providerMode;
    }

    public LocalService getLocalService() {
        return localService;
    }

    public void setLocalService(LocalService localService) {
        this.localService = localService;
    }

    public Map<EmbeddingModel, Integer> getVectorDimensions() {
        return vectorDimensions;
    }

    public void setVectorDimensions(Map<EmbeddingModel, Integer> vectorDimensions) {
        this.vectorDimensions = new EnumMap<>(EmbeddingModel.class);
        if (vectorDimensions != null) {
            this.vectorDimensions.putAll(vectorDimensions);
        }
    }

    private Map<EmbeddingModel, Integer> defaultVectorDimensions() {
        Map<EmbeddingModel, Integer> defaults = new EnumMap<>(EmbeddingModel.class);
        defaults.put(EmbeddingModel.GEMINI_EMBEDDING_001, 3072);
        defaults.put(EmbeddingModel.TEXT_EMBEDDING_3_SMALL, 1536);
        defaults.put(EmbeddingModel.MULTILINGUAL_E5_BASE, 768);
        defaults.put(EmbeddingModel.PHOBERT_BASE, 768);
        defaults.put(EmbeddingModel.BGE_M3, 1024);
        return defaults;
    }

    public static class LocalService {

        private String baseUrl = "http://localhost:8000";
        private Duration timeout = Duration.ofSeconds(30);

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }

}
