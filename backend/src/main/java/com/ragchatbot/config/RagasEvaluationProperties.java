package com.ragchatbot.config;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "rag.evaluation")
public class RagasEvaluationProperties {

    @NotBlank
    private String baseUrl = "http://localhost:8002";

    @NotBlank
    private String evaluatePath = "/evaluate";

    private Duration requestTimeout = Duration.ofSeconds(30);

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getEvaluatePath() {
        return evaluatePath;
    }

    public void setEvaluatePath(String evaluatePath) {
        this.evaluatePath = evaluatePath;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }
}
