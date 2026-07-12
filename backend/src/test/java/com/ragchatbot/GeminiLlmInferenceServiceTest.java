package com.ragchatbot;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragchatbot.config.GeminiProperties;
import com.ragchatbot.domain.enums.MessageRole;
import com.ragchatbot.domain.port.ConversationTurn;
import com.ragchatbot.domain.port.LlmAnswer;
import com.ragchatbot.domain.port.RetrievedContext;
import com.ragchatbot.infrastructure.gemini.GeminiApiClient;
import com.ragchatbot.infrastructure.llm.GeminiLlmInferenceService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GeminiLlmInferenceServiceTest {

    @Test
    void buildsGroundedAnswerUsingRetrievedContexts() {
        GeminiProperties properties = new GeminiProperties();
        properties.setChatModel("gemini-2.5-flash");

        GeminiApiClient fakeClient = new GeminiApiClient() {
            @Override
            public String generateContent(String modelName, String prompt) {
                assertThat(modelName).isEqualTo("gemini-2.5-flash");
                assertThat(prompt).contains("Java la gi?", "Retrieved contexts:", "huong doi tuong");
                return "Java la ngon ngu lap trinh huong doi tuong.";
            }

            @Override
            public List<Float> embedContent(String modelName, String text) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<List<Float>> embedContents(String modelName, List<String> texts) {
                throw new UnsupportedOperationException();
            }
        };

        GeminiLlmInferenceService service = new GeminiLlmInferenceService(properties, fakeClient);
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();

        LlmAnswer answer = service.generateAnswer(
                "Java la gi?",
                List.of(new ConversationTurn(MessageRole.USER, "Mon nay hoc ve Java co ban.")),
                List.of(new RetrievedContext(
                        chunkId,
                        documentId,
                        "Java la ngon ngu lap trinh huong doi tuong.",
                        0.92,
                        "JAVA101",
                        "CH1",
                        "java-basic.pdf",
                        3, null, null, null))
        );

        assertThat(answer.answer()).isEqualTo("Java la ngon ngu lap trinh huong doi tuong.");
        assertThat(answer.groundedInDocuments()).isTrue();
        assertThat(answer.citations()).extracting("documentId", "chunkId", "sourceFileName", "pageNumber")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(documentId, chunkId, "java-basic.pdf", 3));
    }

    @Test
    void fallsBackToOllamaWhenGeminiQuotaIsExceeded() {
        GeminiProperties properties = new GeminiProperties();
        properties.setChatModel("gemini-2.5-flash");

        GeminiApiClient fakeClient = new GeminiApiClient() {
            @Override
            public String generateContent(String modelName, String prompt) {
                throw new RuntimeException("429 quota exceeded for generate content");
            }

            @Override
            public List<Float> embedContent(String modelName, String text) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<List<Float>> embedContents(String modelName, List<String> texts) {
                throw new UnsupportedOperationException();
            }
        };

        GeminiLlmInferenceService service = new GeminiLlmInferenceService(
                properties,
                fakeClient,
                (question, conversationHistory, retrievedContexts) -> new LlmAnswer(
                        "Tra loi tu Ollama fallback.",
                        List.of(retrievedContexts.getFirst().toCitationReference()),
                        true
                )
        );

        LlmAnswer answer = service.generateAnswer(
                "Java la gi?",
                List.of(new ConversationTurn(MessageRole.USER, "Cho minh biet them.")),
                List.of(new RetrievedContext(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Java la ngon ngu lap trinh.",
                        0.9,
                        "JAVA101",
                        "CH1",
                        "fallback.pdf",
                        5, null, null, null))
        );

        assertThat(answer.answer()).isEqualTo("Tra loi tu Ollama fallback.");
        assertThat(answer.citations()).hasSize(1);
        assertThat(answer.groundedInDocuments()).isTrue();
    }
}
