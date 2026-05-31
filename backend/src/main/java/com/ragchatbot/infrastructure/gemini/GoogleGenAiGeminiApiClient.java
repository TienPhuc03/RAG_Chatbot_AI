package com.ragchatbot.infrastructure.gemini;

import com.google.genai.Client;
import com.google.genai.types.EmbedContentResponse;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.ragchatbot.config.GeminiProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GoogleGenAiGeminiApiClient implements GeminiApiClient {

    private final GeminiProperties geminiProperties;

    public GoogleGenAiGeminiApiClient(GeminiProperties geminiProperties) {
        this.geminiProperties = Objects.requireNonNull(geminiProperties);
    }

    @Override
    public String generateContent(String modelName, String prompt) {
        GenerateContentResponse response = createClient().models.generateContent(modelName, prompt, null);
        return response.text();
    }

    @Override
    public List<Float> embedContent(String modelName, String text) {
        List<List<Float>> embeddings = embedContents(modelName, List.of(text));
        if (embeddings.isEmpty()) {
            return List.of();
        }
        return embeddings.getFirst();
    }

    @Override
    public List<List<Float>> embedContents(String modelName, List<String> texts) {
        List<List<Float>> embeddings = new ArrayList<>();
        Client client = createClient();
        for (String text : texts) {
            EmbedContentResponse response = client.models.embedContent(modelName, text, null);
            embeddings.add(extractEmbedding(response));
        }
        return embeddings;
    }

    private Client createClient() {
        String apiKey = geminiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is required to call Gemini API.");
        }

        HttpOptions httpOptions = HttpOptions.builder()
                .apiVersion("v1beta")
                .timeout(Math.toIntExact(geminiProperties.getTimeout().toMillis()))
                .retryOptions(
                        HttpRetryOptions.builder()
                                .attempts(geminiProperties.getMaxRetries())
                                .httpStatusCodes(408, 429, 500, 502, 503, 504)
                                .build()
                )
                .build();

        return Client.builder()
                .apiKey(apiKey)
                .httpOptions(httpOptions)
                .build();
    }

    private List<Float> extractEmbedding(EmbedContentResponse response) {
        List<com.google.genai.types.ContentEmbedding> embeddings = response.embeddings().orElse(List.of());
        if (embeddings.isEmpty()) {
            return List.of();
        }

        return embeddings.getFirst().values()
                .map(List::copyOf)
                .orElseGet(List::of);
    }
}
