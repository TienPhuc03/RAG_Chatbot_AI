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
import java.nio.charset.StandardCharsets;
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
                        .version(HttpClient.Version.HTTP_1_1)
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
        this.supportedModel = Objects.requireNonNull(supportedModel, "supportedModel must not be null");
        this.upstreamModelName = Objects.requireNonNull(upstreamModelName, "upstreamModelName must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    @Override
    public EmbeddingModel supportedModel() {
        return supportedModel;
    }

    @Override
    public List<Float> embed(String text) {
        List<List<Float>> embeddings = embedAll(List.of(text));

        if (embeddings.isEmpty()) {
            throw new IllegalStateException(
                    "Embedding service returned no embeddings for model " + supportedModel
            );
        }

        return embeddings.getFirst();
    }

    @Override
    public List<List<Float>> embedAll(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        try {
            String requestBody = objectMapper.writeValueAsString(
                    new LocalEmbeddingRequest(texts, upstreamModelName)
            );

            String embedUrl = normalizeBaseUrl(properties.getLocalService().getBaseUrl()) + "/embed";

            // Debug tạm thời để xác nhận backend thật sự gửi đúng body sang FastAPI.
            // Sau khi index chạy ổn, bạn có thể xóa 2 dòng println này.
            System.out.println("LOCAL EMBEDDING URL = " + embedUrl);
            System.out.println("LOCAL EMBEDDING REQUEST BODY = " + requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .uri(URI.create(embedUrl))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .timeout(properties.getLocalService().getTimeout())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Local embedding service error: " + response.body()
                );
            }

            return objectMapper.readValue(response.body(), EMBEDDINGS_RESPONSE);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to call local embedding service", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling local embedding service", ex);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("Local embedding service baseUrl must not be blank");
        }

        return baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
    }

    private record LocalEmbeddingRequest(
            List<String> texts,
            String model
    ) {
    }
}