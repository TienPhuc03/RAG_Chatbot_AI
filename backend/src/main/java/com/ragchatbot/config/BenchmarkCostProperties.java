package com.ragchatbot.config;

import com.ragchatbot.domain.enums.EmbeddingModel;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.benchmark.cost")
public class BenchmarkCostProperties {

    private Map<EmbeddingModel, BigDecimal> embeddingInputPer1kTokens = new EnumMap<>(EmbeddingModel.class);
    private Map<String, BigDecimal> llmInputPer1kTokens = new HashMap<>();
    private Map<String, BigDecimal> llmOutputPer1kTokens = new HashMap<>();

    public Map<EmbeddingModel, BigDecimal> getEmbeddingInputPer1kTokens() {
        return embeddingInputPer1kTokens;
    }

    public void setEmbeddingInputPer1kTokens(Map<EmbeddingModel, BigDecimal> embeddingInputPer1kTokens) {
        this.embeddingInputPer1kTokens = new EnumMap<>(EmbeddingModel.class);
        if (embeddingInputPer1kTokens != null) {
            this.embeddingInputPer1kTokens.putAll(embeddingInputPer1kTokens);
        }
    }

    public Map<String, BigDecimal> getLlmInputPer1kTokens() {
        return llmInputPer1kTokens;
    }

    public void setLlmInputPer1kTokens(Map<String, BigDecimal> llmInputPer1kTokens) {
        this.llmInputPer1kTokens = llmInputPer1kTokens == null ? new HashMap<>() : new HashMap<>(llmInputPer1kTokens);
    }

    public Map<String, BigDecimal> getLlmOutputPer1kTokens() {
        return llmOutputPer1kTokens;
    }

    public void setLlmOutputPer1kTokens(Map<String, BigDecimal> llmOutputPer1kTokens) {
        this.llmOutputPer1kTokens = llmOutputPer1kTokens == null ? new HashMap<>() : new HashMap<>(llmOutputPer1kTokens);
    }
}
