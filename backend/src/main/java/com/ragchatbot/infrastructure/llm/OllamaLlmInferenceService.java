package com.ragchatbot.infrastructure.llm;

import com.ragchatbot.domain.port.ConversationTurn;
import com.ragchatbot.domain.port.LlmAnswer;
import com.ragchatbot.domain.port.LlmInferenceService;
import com.ragchatbot.domain.port.RetrievedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * LLM inference thông qua Ollama REST API.
 *
 * Dùng cho luồng FINE_TUNED_MODEL trong benchmark:
 * gửi câu hỏi thẳng đến model fine-tuned trên Ollama (của Tiến Phúc),
 * không qua retrieval pipeline.
 */
public class OllamaLlmInferenceService implements LlmInferenceService {

    private static final Logger log = LoggerFactory.getLogger(OllamaLlmInferenceService.class);

    private final RestClient restClient;
    private final String baseUrl;
    private final String model;

    public OllamaLlmInferenceService(RestClient restClient, String baseUrl, String model) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        this.model = Objects.requireNonNull(model, "model must not be null");
    }

    @Override
    public LlmAnswer generateAnswer(
            String question,
            List<ConversationTurn> conversationHistory,
            List<RetrievedContext> retrievedContexts
    ) {
        log.info("Gọi Ollama [{}] model={} | question length={}", baseUrl, model, question.length());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(baseUrl + "/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", model,
                            "prompt", question,
                            "stream", false
                    ))
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("response")) {
                throw new IllegalStateException("Ollama trả về response rỗng hoặc thiếu field 'response'");
            }

            String answer = response.get("response").toString();
            log.info("Ollama phản hồi thành công | answer length={}", answer.length());

            // Fine-tuned model không có retrieval → citations rỗng, groundedInDocuments = false
            return new LlmAnswer(answer, List.of(), false);

        } catch (Exception ex) {
            log.error("Lỗi khi gọi Ollama: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Không thể gọi Ollama tại " + baseUrl + ": " + ex.getMessage(), ex);
        }
    }
}
