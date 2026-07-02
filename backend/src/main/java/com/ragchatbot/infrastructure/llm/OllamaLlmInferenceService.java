package com.ragchatbot.infrastructure.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchatbot.domain.port.ConversationTurn;
import com.ragchatbot.domain.port.LlmAnswer;
import com.ragchatbot.domain.port.LlmInferenceService;
import com.ragchatbot.domain.port.RetrievedContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "rag.llm", name = "provider", havingValue = "OLLAMA")
public class OllamaLlmInferenceService implements LlmInferenceService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String ollamaBaseUrl;
    private final String modelName;

    public OllamaLlmInferenceService(
            ObjectMapper objectMapper,
            @Value("${ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${ollama.model:hoc-phan-chatbot}") String modelName
    ) {
        this(
                objectMapper,
                ollamaBaseUrl,
                modelName,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build()
        );
    }

    public OllamaLlmInferenceService(
            ObjectMapper objectMapper,
            String ollamaBaseUrl,
            String modelName,
            HttpClient httpClient
    ) {
        this.objectMapper = objectMapper;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.modelName = modelName;
        this.httpClient = httpClient;
    }

    @Override
    public LlmAnswer generateAnswer(
            String question,
            List<ConversationTurn> conversationHistory,
            List<RetrievedContext> retrievedContexts
    ) {
        try {
            OllamaGenerateRequest requestBody = new OllamaGenerateRequest(
                    modelName,
                    buildPrompt(question, conversationHistory, retrievedContexts),
                    false
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaBaseUrl + "/api/generate"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Ollama API error: " + response.body());
            }

            OllamaGenerateResponse result = objectMapper.readValue(
                    response.body(),
                    OllamaGenerateResponse.class
            );

            List<String> citations = retrievedContexts.stream()
                    .map(context -> context.documentId() + ":" + context.chunkId())
                    .toList();

            return new LlmAnswer(result.response(), citations, !retrievedContexts.isEmpty());
        } catch (Exception ex) {
            throw new IllegalStateException("Khong goi duoc Ollama local: " + ex.getMessage(), ex);
        }
    }

    private String buildPrompt(
            String question,
            List<ConversationTurn> conversationHistory,
            List<RetrievedContext> retrievedContexts
    ) {
        String historySection = conversationHistory.stream()
                .map(turn -> turn.role() + ": " + turn.content())
                .collect(Collectors.joining(System.lineSeparator()));

        String contextSection = retrievedContexts.stream()
                .map(RetrievedContext::content)
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));

        return """
                Ban la chatbot ho tro sinh vien hoi dap theo tai lieu mon hoc.
                Chi tra loi bang tieng Viet, ngan gon, de hieu.
                Neu tai lieu khong du thong tin thi noi ro dieu do.

                Lich su hoi thoai:
                %s

                Ngu canh truy xuat:
                %s

                Cau hoi:
                %s
                """.formatted(
                historySection.isBlank() ? "(khong co)" : historySection,
                contextSection.isBlank() ? "(khong co)" : contextSection,
                question
        );
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
