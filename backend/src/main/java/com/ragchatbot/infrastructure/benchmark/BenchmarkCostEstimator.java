package com.ragchatbot.infrastructure.benchmark;

import com.ragchatbot.config.BenchmarkCostProperties;
import com.ragchatbot.config.LlmConfig;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.port.RetrievedContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkCostEstimator {

    private static final BigDecimal TOKENS_PER_K = new BigDecimal("1000");

    private final BenchmarkCostProperties properties;
    private final LlmConfig llmConfig;

    public BenchmarkCostEstimator(BenchmarkCostProperties properties, LlmConfig llmConfig) {
        this.properties = properties;
        this.llmConfig = llmConfig;
    }

    public BigDecimal estimateRagCost(
            EmbeddingModel embeddingModel,
            String question,
            List<RetrievedContext> retrievedContexts,
            String generatedAnswer
    ) {
        BigDecimal embeddingCost = rateForEmbedding(embeddingModel)
                .multiply(tokenUnits(question));

        String llmProvider = llmProviderKey();
        String contextText = retrievedContexts == null
                ? ""
                : retrievedContexts.stream().map(RetrievedContext::content).reduce("", String::concat);

        BigDecimal llmInputCost = rateForProvider(properties.getLlmInputPer1kTokens(), llmProvider)
                .multiply(tokenUnits(question + contextText));
        BigDecimal llmOutputCost = rateForProvider(properties.getLlmOutputPer1kTokens(), llmProvider)
                .multiply(tokenUnits(generatedAnswer));

        return embeddingCost.add(llmInputCost).add(llmOutputCost).setScale(6, RoundingMode.HALF_UP);
    }

    public BigDecimal estimateFineTuneCost(String question, String generatedAnswer) {
        String providerKey = "FINETUNE_LOCAL";
        BigDecimal inputCost = rateForProvider(properties.getLlmInputPer1kTokens(), providerKey)
                .multiply(tokenUnits(question));
        BigDecimal outputCost = rateForProvider(properties.getLlmOutputPer1kTokens(), providerKey)
                .multiply(tokenUnits(generatedAnswer));
        return inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal tokenUnits(String text) {
        if (text == null || text.isBlank()) {
            return BigDecimal.ZERO;
        }

        int whitespaceTokens = text.trim().split("\\s+").length;
        int characterTokens = (int) Math.ceil(text.length() / 4.0d);
        int estimatedTokens = Math.max(whitespaceTokens, characterTokens);
        return BigDecimal.valueOf(estimatedTokens).divide(TOKENS_PER_K, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal rateForEmbedding(EmbeddingModel embeddingModel) {
        return properties.getEmbeddingInputPer1kTokens().getOrDefault(embeddingModel, BigDecimal.ZERO);
    }

    private BigDecimal rateForProvider(Map<String, BigDecimal> rates, String providerKey) {
        return rates.getOrDefault(providerKey, BigDecimal.ZERO);
    }

    private String llmProviderKey() {
        if (llmConfig.provider() == null || llmConfig.provider().isBlank()) {
            return "GEMINI";
        }
        return llmConfig.provider().trim().toUpperCase(Locale.ROOT);
    }
}
