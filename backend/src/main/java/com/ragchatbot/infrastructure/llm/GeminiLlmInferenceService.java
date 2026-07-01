package com.ragchatbot.infrastructure.llm;

import com.ragchatbot.config.GeminiProperties;
import com.ragchatbot.domain.port.ConversationTurn;
import com.ragchatbot.domain.port.LlmAnswer;
import com.ragchatbot.domain.port.LlmInferenceService;
import com.ragchatbot.domain.port.RetrievedContext;
import com.ragchatbot.infrastructure.gemini.GeminiApiClient;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "rag.llm", name = "provider", havingValue = "GEMINI", matchIfMissing = true)
public class GeminiLlmInferenceService implements LlmInferenceService {

    private final GeminiProperties geminiProperties;
    private final GeminiApiClient geminiApiClient;

    public GeminiLlmInferenceService(GeminiProperties geminiProperties, GeminiApiClient geminiApiClient) {
        this.geminiProperties = Objects.requireNonNull(geminiProperties);
        this.geminiApiClient = Objects.requireNonNull(geminiApiClient);
    }

    @Override
    public LlmAnswer generateAnswer(
            String question,
            List<ConversationTurn> conversationHistory,
            List<RetrievedContext> retrievedContexts
    ) {
        String prompt = buildPrompt(question, conversationHistory, retrievedContexts);
        String answer = geminiApiClient.generateContent(geminiProperties.getChatModel(), prompt);
        List<String> citations = retrievedContexts.stream()
                .map(context -> context.documentId() + ":" + context.chunkId())
                .toList();

        return new LlmAnswer(answer, citations, !retrievedContexts.isEmpty());
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
