package com.ragchatbot.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchatbot.domain.enums.MessageRole;
import com.ragchatbot.domain.port.ConversationTurn;
import com.ragchatbot.domain.port.LlmAnswer;
import com.ragchatbot.domain.port.RetrievedContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OllamaLlmInferenceServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void parsesOllamaGenerateResponse() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/generate", this::handleGenerate);
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        OllamaLlmInferenceService service = new OllamaLlmInferenceService(
                new ObjectMapper(),
                baseUrl,
                "hoc-phan-chatbot",
                HttpClient.newHttpClient()
        );

        LlmAnswer answer = service.generateAnswer(
                "Chunking la gi?",
                List.of(new ConversationTurn(MessageRole.USER, "Mon nay hoc ve RAG.")),
                List.of(new RetrievedContext(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Chunking la chia tai lieu thanh doan nho.",
                        0.91,
                        "RAG101",
                        "CH1"))
        );

        assertThat(answer.answer()).isEqualTo("Day la cau tra loi tu Ollama.");
        assertThat(answer.groundedInDocuments()).isTrue();
        assertThat(answer.citations()).hasSize(1);
    }

    private void handleGenerate(HttpExchange exchange) throws IOException {
        byte[] response = """
                {"model":"hoc-phan-chatbot","response":"Day la cau tra loi tu Ollama.","done":true}
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }
}
