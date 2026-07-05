package com.ragchatbot.infrastructure.benchmark;

import com.ragchatbot.config.RagasEvaluationProperties;
import com.ragchatbot.domain.model.EvaluationResult;
import com.ragchatbot.domain.port.EvaluationService;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Primary
public class RagasEvaluationService implements EvaluationService {

    private final RestClient restClient;
    private final RagasEvaluationProperties properties;
    private final LocalEvaluationService localEvaluationService;

    public RagasEvaluationService(
            RestClient restClient,
            RagasEvaluationProperties properties,
            LocalEvaluationService localEvaluationService
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.properties = Objects.requireNonNull(properties);
        this.localEvaluationService = Objects.requireNonNull(localEvaluationService);
    }

    @Override
    public EvaluationResult evaluate(String question, String groundTruth, String answer, List<String> contexts) {
        return evaluateDetailed(question, groundTruth, answer, contexts).result();
    }

    public EvaluationDetails evaluateDetailed(String question, String groundTruth, String answer, List<String> contexts) {
        List<String> safeContexts = contexts == null ? List.of() : List.copyOf(contexts);
        EvaluationResult localResult = localEvaluationService.evaluate(question, groundTruth, answer, safeContexts);

        long startedAt = System.nanoTime();
        try {
            RemoteEvaluationResponse response = restClient.post()
                    .uri(buildEvaluateUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RemoteEvaluationRequest(question, groundTruth, answer, safeContexts))
                    .retrieve()
                    .body(RemoteEvaluationResponse.class);

            if (response == null) {
                throw new IllegalStateException("RAGAS service returned an empty response");
            }

            return new EvaluationDetails(
                    new EvaluationResult(
                            localResult.exactMatch(),
                            localResult.f1(),
                            response.faithfulness(),
                            response.answerRelevancy(),
                            response.contextPrecision(),
                            response.contextRecall()
                    ),
                    false,
                    "ragas-service:" + normalizeJudgeProviderForSource(response.judgeProvider()),
                    normalizeJudgeProviderForResponse(response.judgeProvider()),
                    response.judgeFallbackUsed(),
                    elapsedMillis(startedAt)
            );
        } catch (Exception ex) {
            return new EvaluationDetails(
                    localResult,
                    true,
                    "local-fallback",
                    "LOCAL",
                    false,
                    elapsedMillis(startedAt)
            );
        }
    }

    private URI buildEvaluateUri() {
        String baseUrl = stripTrailingSlash(properties.getBaseUrl());
        String path = properties.getEvaluatePath();
        if (path == null || path.isBlank()) {
            path = "/evaluate";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return URI.create(baseUrl + path);
    }

    private long elapsedMillis(long startedAtNanos) {
        return Math.max(0L, (System.nanoTime() - startedAtNanos) / 1_000_000L);
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8002";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String normalizeJudgeProviderForSource(String judgeProvider) {
        if (judgeProvider == null || judgeProvider.isBlank()) {
            return "unknown";
        }
        return judgeProvider.trim().toLowerCase();
    }

    private String normalizeJudgeProviderForResponse(String judgeProvider) {
        if (judgeProvider == null || judgeProvider.isBlank()) {
            return "UNKNOWN";
        }
        return judgeProvider.trim().toUpperCase();
    }

    public record EvaluationDetails(
            EvaluationResult result,
            boolean fallbackUsed,
            String source,
            String judgeProvider,
            boolean judgeFallbackUsed,
            long latencyMs
    ) {
    }

    private record RemoteEvaluationRequest(
            String question,
            String groundTruth,
            String answer,
            List<String> contexts
    ) {
    }

    private record RemoteEvaluationResponse(
            double faithfulness,
            double answerRelevancy,
            double contextPrecision,
            double contextRecall,
            String judgeProvider,
            boolean judgeFallbackUsed
    ) {
    }
}
