package com.ragchatbot.infrastructure.benchmark;

import com.ragchatbot.application.usecase.document.DocumentMaintenanceService;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.enums.ExperimentType;
import com.ragchatbot.domain.model.BenchmarkResult;
import com.ragchatbot.domain.model.TestCase;
import com.ragchatbot.domain.port.FineTunedInferenceService;
import com.ragchatbot.domain.port.LlmAnswer;
import com.ragchatbot.domain.port.LlmInferenceService;
import com.ragchatbot.domain.port.RetrievedContext;
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.embedding.EmbeddingRouter;
import com.ragchatbot.infrastructure.persistence.BenchmarkResultRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BenchmarkRunnerService {

    private final TestSetLoader testSetLoader;
    private final RagasEvaluationService evaluationService;
    private final VectorStoreService vectorStoreService;
    private final LlmInferenceService llmInferenceService;
    private final FineTunedInferenceService fineTunedInferenceService;
    private final EmbeddingRouter embeddingRouter;
    private final BenchmarkResultRepository benchmarkResultRepository;
    private final BenchmarkJobRegistry benchmarkJobRegistry;
    private final DocumentMaintenanceService documentMaintenanceService;
    private final BenchmarkCostEstimator benchmarkCostEstimator;

    public BenchmarkRunnerService(
            TestSetLoader testSetLoader,
            RagasEvaluationService evaluationService,
            VectorStoreService vectorStoreService,
            LlmInferenceService llmInferenceService,
            FineTunedInferenceService fineTunedInferenceService,
            EmbeddingRouter embeddingRouter,
            BenchmarkResultRepository benchmarkResultRepository,
            BenchmarkJobRegistry benchmarkJobRegistry,
            DocumentMaintenanceService documentMaintenanceService,
            BenchmarkCostEstimator benchmarkCostEstimator
    ) {
        this.testSetLoader = testSetLoader;
        this.evaluationService = evaluationService;
        this.vectorStoreService = vectorStoreService;
        this.llmInferenceService = llmInferenceService;
        this.fineTunedInferenceService = fineTunedInferenceService;
        this.embeddingRouter = embeddingRouter;
        this.benchmarkResultRepository = benchmarkResultRepository;
        this.benchmarkJobRegistry = benchmarkJobRegistry;
        this.documentMaintenanceService = documentMaintenanceService;
        this.benchmarkCostEstimator = benchmarkCostEstimator;
    }

    @Async
    public void runBenchmark(String jobId, BenchmarkConfig config) {
        List<TestCase> testCases = testSetLoader.loadTestCases();
        benchmarkJobRegistry.markRunning(jobId);

        try {
            documentMaintenanceService.reconcileStaleProcessingDocuments();
            ExperimentType experimentType = ExperimentType.valueOf(config.experimentType());
            ChunkingStrategy chunkingStrategy = parseChunkingStrategy(config.strategy());
            EmbeddingModel embeddingModel = parseEmbeddingModel(config.embeddingModel());
            if (experimentType == ExperimentType.RAG
                    && !documentMaintenanceService.hasIndexedDocumentsForBenchmark(chunkingStrategy, embeddingModel)) {
                throw new IllegalStateException(
                        "Khong co tai lieu INDEXED cho benchmark config "
                                + chunkingStrategy
                                + " + "
                                + embeddingModel
                                + ". Hay upload va index cung mot corpus voi dung strategy va embedding model nay truoc."
                );
            }

            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                List<RetrievedContext> retrievedContexts = Collections.emptyList();
                boolean retrievalHit = false;
                String generatedAnswer;
                long latencyMs;
                BigDecimal costUsd;

                if (experimentType == ExperimentType.RAG) {
                    long startedAt = System.nanoTime();
                    List<Float> questionEmbedding = embeddingRouter.embed(
                            embeddingModel,
                            testCase.question()
                    );
                    retrievedContexts = vectorStoreService.search(
                            embeddingModel,
                            questionEmbedding,
                            5,
                            chunkingStrategy,
                            null,
                            null,
                            null
                    );
                    retrievalHit = computeRetrievalHit(testCase, retrievedContexts);
                    LlmAnswer llmAnswer = llmInferenceService.generateAnswer(
                            testCase.question(),
                            new ArrayList<>(),
                            retrievedContexts
                    );
                    generatedAnswer = llmAnswer.answer();
                    latencyMs = elapsedMillis(startedAt);
                    costUsd = benchmarkCostEstimator.estimateRagCost(
                            embeddingModel,
                            testCase.question(),
                            retrievedContexts,
                            generatedAnswer
                    );
                } else {
                    long startedAt = System.nanoTime();
                    generatedAnswer = fineTunedInferenceService.generateAnswer(testCase.question());
                    latencyMs = elapsedMillis(startedAt);
                    costUsd = benchmarkCostEstimator.estimateFineTuneCost(
                            testCase.question(),
                            generatedAnswer
                    );
                }

                List<String> contextsForEvaluation = retrievedContexts.stream()
                        .map(RetrievedContext::content)
                        .toList();

                RagasEvaluationService.EvaluationDetails evaluationDetails = evaluationService.evaluateDetailed(
                        testCase.question(),
                        testCase.groundTruth(),
                        generatedAnswer,
                        contextsForEvaluation
                );

                BenchmarkResult benchmarkResult = new BenchmarkResult();
                benchmarkResult.setRunId(config.runId());
                benchmarkResult.setQuestionId(testCase.id());
                benchmarkResult.setExperimentType(experimentType);
                benchmarkResult.setChunkingStrategy(experimentType == ExperimentType.RAG ? chunkingStrategy : null);
                benchmarkResult.setEmbeddingModel(experimentType == ExperimentType.RAG ? embeddingModel : null);
                benchmarkResult.setQuestion(testCase.question());
                benchmarkResult.setGroundTruth(testCase.groundTruth());
                benchmarkResult.setGeneratedAnswer(generatedAnswer);
                benchmarkResult.setExactMatch(evaluationDetails.result().exactMatch());
                benchmarkResult.setF1Score(evaluationDetails.result().f1());
                benchmarkResult.setFaithfulness(evaluationDetails.result().faithfulness());
                benchmarkResult.setAnswerRelevancy(evaluationDetails.result().answerRelevancy());
                benchmarkResult.setContextPrecision(evaluationDetails.result().contextPrecision());
                benchmarkResult.setContextRecall(evaluationDetails.result().contextRecall());
                benchmarkResult.setRetrievalHit(retrievalHit);
                benchmarkResult.setEvaluationSource(evaluationDetails.source());
                benchmarkResult.setEvaluationFallbackUsed(evaluationDetails.fallbackUsed());
                benchmarkResult.setLatencyMs(latencyMs);
                benchmarkResult.setCostUsd(costUsd);

                benchmarkResultRepository.save(benchmarkResult);
                benchmarkJobRegistry.updateProgress(jobId, i + 1);
            }

            benchmarkJobRegistry.markCompleted(jobId);
        } catch (Exception ex) {
            benchmarkJobRegistry.markFailed(jobId, simplifyErrorMessage(ex));
        }
    }

    private EmbeddingModel parseEmbeddingModel(String value) {
        if (!StringUtils.hasText(value)) {
            return EmbeddingModel.GEMINI_EMBEDDING_001;
        }
        EmbeddingModel embeddingModel = EmbeddingModel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        if (!embeddingModel.isAllowedForNewRequests()) {
            throw new IllegalArgumentException(
                    "Embedding model "
                            + embeddingModel
                            + " da ngung ho tro cho benchmark moi."
            );
        }
        return embeddingModel;
    }

    private ChunkingStrategy parseChunkingStrategy(String value) {
        if (!StringUtils.hasText(value)) {
            return ChunkingStrategy.SEMANTIC;
        }
        return ChunkingStrategy.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private String simplifyErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Benchmark that bai do loi khong xac dinh.";
        }
        if (message.contains("429") || message.toLowerCase().contains("quota exceeded")) {
            return "Gemini dang het quota hoac bi gioi han toc do. Hay doi them, doi API key, hoac chuyen rag.llm.provider sang OLLAMA.";
        }
        return message;
    }

    private boolean computeRetrievalHit(TestCase testCase, List<RetrievedContext> retrievedContexts) {
        if (Boolean.TRUE.equals(testCase.outOfScope())) {
            return retrievedContexts == null || retrievedContexts.isEmpty();
        }

        if (retrievedContexts == null || retrievedContexts.isEmpty()) {
            return false;
        }

        String expectedSource = normalize(testCase.expectedSource());

        boolean sourceHit = expectedSource == null || retrievedContexts.stream()
                .anyMatch(context -> {
                    String sourceFileName = normalize(context.sourceFileName());
                    return sourceFileName != null && sourceFileName.contains(expectedSource);
                });

        List<String> keywords = testCase.expectedKeywords() == null
                ? List.of()
                : testCase.expectedKeywords();

        long keywordHits = keywords.stream()
                .filter(keyword -> {
                    String normalizedKeyword = normalize(keyword);
                    return normalizedKeyword != null && retrievedContexts.stream()
                            .anyMatch(context -> {
                                String content = normalize(context.content());
                                return content != null && content.contains(normalizedKeyword);
                            });
                })
                .count();

        int requiredHits = Math.max(1, (int) Math.ceil(keywords.size() * 0.5));

        return sourceHit && keywordHits >= requiredHits;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT).trim();
    }

    private long elapsedMillis(long startedAtNanos) {
        return Math.max(0L, (System.nanoTime() - startedAtNanos) / 1_000_000L);
    }

    public record BenchmarkConfig(
            String strategy,
            String embeddingModel,
            String experimentType,
            String runId
    ) {
    }
}
