package com.ragchatbot.infrastructure.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchatbot.config.FineTunedBenchmarkProperties;
import com.ragchatbot.domain.port.FineTunedInferenceService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FineTunedOllamaInferenceService implements FineTunedInferenceService {

    private final ObjectMapper objectMapper;
    private final FineTunedBenchmarkProperties properties;
    private final HttpClient httpClient;

    @Autowired
    public FineTunedOllamaInferenceService(
            ObjectMapper objectMapper,
            FineTunedBenchmarkProperties properties
    ) {
        this(
                objectMapper,
                properties,
                HttpClient.newBuilder()
                        .connectTimeout(properties.getTimeout())
                        .build()
        );
    }

    FineTunedOllamaInferenceService(
            ObjectMapper objectMapper,
            FineTunedBenchmarkProperties properties,
            HttpClient httpClient
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.properties = Objects.requireNonNull(properties);
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    @Override
    public String generateAnswer(String question) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.getOllamaBaseUrl()) + "/api/generate"))
                    .timeout(properties.getTimeout())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(
                            new OllamaGenerateRequest(properties.getOllamaModel(), question, false)
                    )))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Ollama API error: " + response.body());
            }

            OllamaGenerateResponse result = objectMapper.readValue(response.body(), OllamaGenerateResponse.class);
            return result.response();
        } catch (Exception ex) {
            throw new IllegalStateException("Khong goi duoc Ollama fine-tuned local: " + ex.getMessage(), ex);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private record OllamaGenerateRequest(
            String model,
            String prompt,
            boolean stream
    ) {
    }

    private record OllamaGenerateResponse(
            String model,
            String response,
            boolean done
    ) {
    }
}
