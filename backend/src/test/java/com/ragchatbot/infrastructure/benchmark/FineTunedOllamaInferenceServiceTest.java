package com.ragchatbot.infrastructure.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchatbot.config.FineTunedBenchmarkProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FineTunedOllamaInferenceServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void parsesFineTunedOllamaGenerateResponse() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/generate", this::handleGenerate);
        server.start();

        FineTunedBenchmarkProperties properties = new FineTunedBenchmarkProperties();
        properties.setOllamaBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.setOllamaModel("hoc-phan-chatbot-ft");

        FineTunedOllamaInferenceService service = new FineTunedOllamaInferenceService(
                new ObjectMapper(),
                properties,
                HttpClient.newHttpClient()
        );

        String answer = service.generateAnswer("Java dung de lam gi?");

        assertThat(answer).isEqualTo("Day la cau tra loi fine-tuned.");
    }

    private void handleGenerate(HttpExchange exchange) throws IOException {
        byte[] response = """
                {"model":"hoc-phan-chatbot-ft","response":"Day la cau tra loi fine-tuned.","done":true}
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }
}
