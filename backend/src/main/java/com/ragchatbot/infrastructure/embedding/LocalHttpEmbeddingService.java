package com.ragchatbot.infrastructure.embedding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchatbot.config.EmbeddingProperties;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.port.EmbeddingService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class LocalHttpEmbeddingService implements EmbeddingService {

    private static final TypeReference<List<List<Float>>> EMBEDDINGS_RESPONSE =
            new TypeReference<>() {
            };

    private final EmbeddingModel supportedModel;
    private final String upstreamModelName;
    private final EmbeddingProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public LocalHttpEmbeddingService(
            EmbeddingModel supportedModel,
            String upstreamModelName,
            EmbeddingProperties properties,
            ObjectMapper objectMapper
    ) {
        this(
                supportedModel,
                upstreamModelName,
                properties,
                objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(properties.getLocalService().getTimeout())
                        .build()
        );
    }

    LocalHttpEmbeddingService(
            EmbeddingModel supportedModel,
            String upstreamModelName,
            EmbeddingProperties properties,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.supportedModel = Objects.requireNonNull(supportedModel);
        this.upstreamModelName = Objects.requireNonNull(upstreamModelName);
        this.properties = Objects.requireNonNull(properties);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    @Override
    public EmbeddingModel supportedModel() {
        return supportedModel;
    }

    @Override
    public List<Float> embed(String text) {
        List<List<Float>> embeddings = embedAll(List.of(text));
        if (embeddings.isEmpty()) {
            throw new IllegalStateException("Embedding service returned no embeddings for model " + supportedModel);
        }
        return embeddings.getFirst();
    }

    @Override
    public List<List<Float>> embedAll(List<String> texts) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.getLocalService().getBaseUrl()) + "/embed"))
                    .timeout(timeout())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(
                            new LocalEmbeddingRequest(texts, upstreamModelName)
                    )))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Local embedding service error: " + response.body());
            }
            return objectMapper.readValue(response.body(), EMBEDDINGS_RESPONSE);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse local embedding service response", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling local embedding service", ex);
        }
    }

    private Duration timeout() {
        return properties.getLocalService().getTimeout();
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private record LocalEmbeddingRequest(
            List<String> texts,
            String model_name
    ) {
    }
}
