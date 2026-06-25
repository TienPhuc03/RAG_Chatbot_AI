package com.ragchatbot.infrastructure.benchmark;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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

@Service
public class BenchmarkRunnerService {

    private final TestSetLoader testSetLoader;
    private final EvaluationService evaluationService;
    private final VectorStoreService vectorStoreService;
    private final LlmInferenceService llmInferenceService;
    private final EmbeddingService embeddingService;
    private final BenchmarkResultRepository benchmarkResultRepository;

    public BenchmarkRunnerService(TestSetLoader testSetLoader,
                                  EvaluationService evaluationService,
                                  VectorStoreService vectorStoreService,
                                  LlmInferenceService llmInferenceService,
                                  EmbeddingService embeddingService,
                                  BenchmarkResultRepository benchmarkResultRepository) {
        this.testSetLoader = testSetLoader;
        this.evaluationService = evaluationService;
        this.vectorStoreService = vectorStoreService;
        this.llmInferenceService = llmInferenceService;
        this.embeddingService = embeddingService;
        this.benchmarkResultRepository = benchmarkResultRepository;
    }

    @Async
    public void runBenchmark(BenchmarkConfig config) {
        List<TestCase> testCases = testSetLoader.loadTestCases();

        for (TestCase testCase : testCases) {

            // 1. Nhúng câu hỏi thành vector bằng hàm embed() chuẩn của interface
            List<Float> questionEmbedding = embeddingService.embed(testCase.question());

            // 2. Tìm kiếm ngữ cảnh
            List<RetrievedContext> retrievedContexts = vectorStoreService.search(questionEmbedding, 5, null, null);
            boolean retrievalHit = computeRetrievalHit(testCase.groundTruth(), retrievedContexts);

            // 3. Gọi LLM sinh câu trả lời
            LlmAnswer llmAnswer = llmInferenceService.generateAnswer(testCase.question(), new ArrayList<>(), retrievedContexts);

            // 4. Trích xuất danh sách chuỗi ngữ cảnh để chấm điểm
            List<String> contextsForEvaluation = retrievedContexts.stream()
                    .map(RetrievedContext::content)
                    .toList();

            // 5. Chấm điểm sử dụng llmAnswer.answer()
            EvaluationResult evalResult = evaluationService.evaluate(
                    testCase.question(),
                    testCase.groundTruth(),
                    llmAnswer.answer(),
                    contextsForEvaluation
            );

            // Khởi tạo Entity và lưu Database
            BenchmarkResult result = new BenchmarkResult();
            result.setExperimentType(ExperimentType.valueOf(config.experimentType()));
            result.setChunkingStrategy(ChunkingStrategy.valueOf(config.strategy()));
            result.setEmbeddingModel(EmbeddingModel.valueOf(config.embeddingModel()));

            result.setQuestion(testCase.question());
            result.setGroundTruth(testCase.groundTruth());
            result.setGeneratedAnswer(llmAnswer.answer());

            result.setExactMatch(evalResult.exactMatch());
            result.setF1Score(evalResult.f1());
            result.setFaithfulness(evalResult.faithfulness());
            result.setAnswerRelevancy(evalResult.answerRelevancy());
            result.setContextPrecision(evalResult.contextPrecision());
            result.setContextRecall(evalResult.contextRecall());
            result.setRetrievalHit(retrievalHit);

            result.setLatencyMs(0L);
            result.setCostUsd(BigDecimal.ZERO);

            benchmarkResultRepository.save(result);
            System.out.println("Đã chạy xong và lưu test case: " + testCase.id() + " | F1: " + evalResult.f1());

        }
    }

    private boolean computeRetrievalHit(String groundTruth, List<RetrievedContext> retrievedContexts) {
    if (groundTruth == null || groundTruth.isBlank() || retrievedContexts == null || retrievedContexts.isEmpty()) {
        return false;
    }
    String normalizedGroundTruth = groundTruth.toLowerCase().trim();
    return retrievedContexts.stream()
            .map(RetrievedContext::content)
            .filter(content -> content != null && !content.isBlank())
            .anyMatch(content -> {
                String normalizedContent = content.toLowerCase().trim();
                return normalizedContent.contains(normalizedGroundTruth)
                        || normalizedGroundTruth.contains(normalizedContent);
            });
    }

    public record BenchmarkConfig(
            String strategy,
            String embeddingModel,
            String experimentType
    ) {}
    
}