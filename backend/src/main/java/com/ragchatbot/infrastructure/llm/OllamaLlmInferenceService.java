package com.ragchatbot.infrastructure.llm;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class OllamaLlmInferenceService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:hoc-phan-chatbot}")
    private String modelName;

    public OllamaLlmInferenceService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String generate(String question) {
        try {
            String prompt = buildPrompt(question);

            OllamaGenerateRequest requestBody = new OllamaGenerateRequest(
                    modelName,
                    prompt,
                    false
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaBaseUrl + "/api/generate"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Ollama API error: " + response.body());
            }

            OllamaGenerateResponse result = objectMapper.readValue(
                    response.body(),
                    OllamaGenerateResponse.class
            );

            return result.response();

        } catch (Exception e) {
            throw new RuntimeException("Không gọi được Ollama local: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String question) {
        return """
                Bạn là chatbot hỗ trợ sinh viên hỏi đáp dựa trên tài liệu môn học.
                Hãy trả lời bằng tiếng Việt, ngắn gọn, đúng trọng tâm.
                Nếu không chắc chắn, hãy nói rằng bạn không có đủ thông tin.

                Câu hỏi:
                %s
                """.formatted(question);
    }

    public record OllamaGenerateRequest(
            String model,
            String prompt,
            boolean stream
    ) {
    }

    public record OllamaGenerateResponse(
            String model,
            String response,
            boolean done
    ) {
    }
}
