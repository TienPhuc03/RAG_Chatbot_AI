package com.ragchatbot.infrastructure.benchmark;

import com.ragchatbot.application.usecase.document.DocumentMaintenanceService;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.enums.ExperimentType;
import com.ragchatbot.domain.model.BenchmarkResult;
import com.ragchatbot.domain.model.EvaluationResult;
import com.ragchatbot.domain.model.TestCase;
import com.ragchatbot.domain.port.EmbeddingService;
import com.ragchatbot.domain.port.EvaluationService;
import com.ragchatbot.domain.port.LlmAnswer;
import com.ragchatbot.domain.port.LlmInferenceService;
import com.ragchatbot.domain.port.RetrievedContext;
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.persistence.BenchmarkResultRepository;
import java.util.Collections;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkRunnerService {

    private final TestSetLoader testSetLoader;
    private final EvaluationService evaluationService;
    private final VectorStoreService vectorStoreService;
    private final LlmInferenceService llmInferenceService;
    private final EmbeddingService embeddingService;
    private final BenchmarkResultRepository benchmarkResultRepository;
    private final BenchmarkJobRegistry benchmarkJobRegistry;
    private final DocumentMaintenanceService documentMaintenanceService;

    public BenchmarkRunnerService(
            TestSetLoader testSetLoader,
            EvaluationService evaluationService,
            VectorStoreService vectorStoreService,
            LlmInferenceService llmInferenceService,
            EmbeddingService embeddingService,
            BenchmarkResultRepository benchmarkResultRepository,
            BenchmarkJobRegistry benchmarkJobRegistry,
            DocumentMaintenanceService documentMaintenanceService
    ) {
        this.testSetLoader = testSetLoader;
        this.evaluationService = evaluationService;
        this.vectorStoreService = vectorStoreService;
        this.llmInferenceService = llmInferenceService;
        this.embeddingService = embeddingService;
        this.benchmarkResultRepository = benchmarkResultRepository;
        this.benchmarkJobRegistry = benchmarkJobRegistry;
        this.documentMaintenanceService = documentMaintenanceService;
    }

    @Async
    public void runBenchmark(String jobId, BenchmarkConfig config) {
        List<TestCase> testCases = testSetLoader.loadTestCases();
        benchmarkJobRegistry.markRunning(jobId);

        try {
            documentMaintenanceService.reconcileStaleProcessingDocuments();
            ExperimentType experimentType = ExperimentType.valueOf(config.experimentType());
            if (experimentType == ExperimentType.RAG && !documentMaintenanceService.hasIndexedDocuments(null, null)) {
                throw new IllegalStateException(
                        "Khong co tai lieu INDEXED de chay benchmark RAG. Hay upload DOCX hoac PDF co text truoc."
                );
            }

            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                List<RetrievedContext> retrievedContexts = Collections.emptyList();
                boolean retrievalHit = false;

                if (experimentType == ExperimentType.RAG) {
                    List<Float> questionEmbedding = embeddingService.embed(testCase.question());
                    retrievedContexts = vectorStoreService.search(questionEmbedding, 5, null, null, null);
                    retrievalHit = computeRetrievalHit(testCase.groundTruth(), retrievedContexts);
                }

                LlmAnswer llmAnswer = llmInferenceService.generateAnswer(
                        testCase.question(),
                        new ArrayList<>(),
                        retrievedContexts
                );

                List<String> contextsForEvaluation = retrievedContexts.stream()
                        .map(RetrievedContext::content)
                        .toList();

                EvaluationResult evaluationResult = evaluationService.evaluate(
                        testCase.question(),
                        testCase.groundTruth(),
                        llmAnswer.answer(),
                        contextsForEvaluation
                );

                BenchmarkResult benchmarkResult = new BenchmarkResult();
                benchmarkResult.setExperimentType(experimentType);
                benchmarkResult.setChunkingStrategy(ChunkingStrategy.valueOf(config.strategy()));
                benchmarkResult.setEmbeddingModel(EmbeddingModel.valueOf(config.embeddingModel()));
                benchmarkResult.setQuestion(testCase.question());
                benchmarkResult.setGroundTruth(testCase.groundTruth());
                benchmarkResult.setGeneratedAnswer(llmAnswer.answer());
                benchmarkResult.setExactMatch(evaluationResult.exactMatch());
                benchmarkResult.setF1Score(evaluationResult.f1());
                benchmarkResult.setFaithfulness(evaluationResult.faithfulness());
                benchmarkResult.setAnswerRelevancy(evaluationResult.answerRelevancy());
                benchmarkResult.setContextPrecision(evaluationResult.contextPrecision());
                benchmarkResult.setContextRecall(evaluationResult.contextRecall());
                benchmarkResult.setRetrievalHit(retrievalHit);
                benchmarkResult.setLatencyMs(0L);
                benchmarkResult.setCostUsd(BigDecimal.ZERO);

                benchmarkResultRepository.save(benchmarkResult);
                benchmarkJobRegistry.updateProgress(jobId, i + 1);
            }

            benchmarkJobRegistry.markCompleted(jobId);
        } catch (Exception ex) {
            benchmarkJobRegistry.markFailed(jobId, simplifyErrorMessage(ex));
        }
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

    private boolean computeRetrievalHit(String groundTruth, List<RetrievedContext> retrievedContexts) {
        if (groundTruth == null || groundTruth.isBlank()) {
            return false;
        }
        if (retrievedContexts == null || retrievedContexts.isEmpty()) {
            return false;
        }

        String normalizedGroundTruth = groundTruth.toLowerCase().trim();
        return retrievedContexts.stream()
                .map(RetrievedContext::content)
                .filter(content -> content != null && !content.isBlank())
                .map(content -> content.toLowerCase().trim())
                .anyMatch(content ->
                        content.contains(normalizedGroundTruth) || normalizedGroundTruth.contains(content)
                );
    }

    public record BenchmarkConfig(
            String strategy,
            String embeddingModel,
            String experimentType
    ) {
    }
}
