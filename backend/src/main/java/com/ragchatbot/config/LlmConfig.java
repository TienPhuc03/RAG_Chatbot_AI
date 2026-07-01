package com.ragchatbot.config;

import com.ragchatbot.domain.port.LlmInferenceService;
import com.ragchatbot.infrastructure.llm.OllamaLlmInferenceService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConfigurationProperties(prefix = "rag.llm")
public class LlmConfig {

    private String ollamaBaseUrl;
    private String ollamaModel;

    // ───────────── getters / setters ─────────────

    public String getOllamaBaseUrl() {
        return ollamaBaseUrl;
    }

    public void setOllamaBaseUrl(String ollamaBaseUrl) {
        this.ollamaBaseUrl = ollamaBaseUrl;
    }

    public String getOllamaModel() {
        return ollamaModel;
    }

    public void setOllamaModel(String ollamaModel) {
        this.ollamaModel = ollamaModel;
    }

    // ───────────── Bean definitions ─────────────

    @Bean("ollamaLlm")
    @Qualifier("ollamaLlm")
    public LlmInferenceService ollamaLlmInferenceService(RestClient restClient) {
        String url = ollamaBaseUrl != null ? ollamaBaseUrl : "http://localhost:11434";
        String mdl = ollamaModel != null ? ollamaModel : "llama3";
        return new OllamaLlmInferenceService(restClient, url, mdl);
    }
}