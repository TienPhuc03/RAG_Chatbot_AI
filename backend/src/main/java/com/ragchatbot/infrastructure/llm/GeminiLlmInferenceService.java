package com.ragchatbot.infrastructure.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchatbot.config.GeminiProperties;
import com.ragchatbot.domain.port.ConversationTurn;
import com.ragchatbot.domain.port.LlmAnswer;
import com.ragchatbot.domain.port.LlmInferenceService;
import com.ragchatbot.domain.port.RetrievedContext;
import com.ragchatbot.infrastructure.gemini.GeminiApiClient;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "rag.llm", name = "provider", havingValue = "GEMINI", matchIfMissing = true)
public class GeminiLlmInferenceService implements LlmInferenceService {

    private final GeminiProperties geminiProperties;
    private final GeminiApiClient geminiApiClient;
    private final LlmInferenceService fallbackInferenceService;

    @Autowired
    public GeminiLlmInferenceService(
            GeminiProperties geminiProperties,
            GeminiApiClient geminiApiClient,
            ObjectMapper objectMapper,
            @Value("${ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${ollama.model:hoc-phan-chatbot}") String ollamaModel
    ) {
        this(
                geminiProperties,
                geminiApiClient,
                new OllamaLlmInferenceService(
                        objectMapper,
                        ollamaBaseUrl,
                        ollamaModel,
                        HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(10))
                                .build()
                )
        );
    }

    public GeminiLlmInferenceService(GeminiProperties geminiProperties, GeminiApiClient geminiApiClient) {
        this(geminiProperties, geminiApiClient, null);
    }

    public GeminiLlmInferenceService(
            GeminiProperties geminiProperties,
            GeminiApiClient geminiApiClient,
            LlmInferenceService fallbackInferenceService
    ) {
        this.geminiProperties = Objects.requireNonNull(geminiProperties);
        this.geminiApiClient = Objects.requireNonNull(geminiApiClient);
        this.fallbackInferenceService = fallbackInferenceService;
    }

    @Override
    public LlmAnswer generateAnswer(
            String question,
            List<ConversationTurn> conversationHistory,
            List<RetrievedContext> retrievedContexts
    ) {
        String prompt = buildPrompt(question, conversationHistory, retrievedContexts);
        try {
            String answer = geminiApiClient.generateContent(geminiProperties.getChatModel(), prompt);
            List<com.ragchatbot.domain.port.CitationReference> citations = retrievedContexts.stream()
                    .map(RetrievedContext::toCitationReference)
                    .toList();

            return new LlmAnswer(answer, citations, !retrievedContexts.isEmpty());
        } catch (RuntimeException ex) {
            if (fallbackInferenceService != null && shouldFallbackToOllama(ex)) {
                return fallbackInferenceService.generateAnswer(question, conversationHistory, retrievedContexts);
            }
            throw ex;
        }
    }

    private boolean shouldFallbackToOllama(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }

        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("quota exceeded")
                || normalized.contains("rate limit")
                || normalized.contains("status code 429")
                || normalized.contains(" 429 ");
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
                .map(context -> """
                        [%s | %s | score=%s]
                        %s
                        """.formatted(
                        context.documentId(),
                        context.chunkId(),
                        context.score(),
                        context.content()))
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));

        return """
                You are a course-material assistant for Vietnamese university students.
                Answer only from the provided retrieved contexts.
                If the answer is not grounded in the contexts, say that the documents do not provide enough information.
                Keep the answer concise and study-oriented.

                Conversation history:
                %s

                Retrieved contexts:
                %s

                User question:
                %s
                """.formatted(
                historySection.isBlank() ? "(no prior conversation)" : historySection,
                contextSection.isBlank() ? "(no retrieved context)" : contextSection,
                question
        );
    }
}
