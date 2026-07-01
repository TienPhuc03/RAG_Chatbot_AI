package com.ragchatbot.infrastructure.benchmark;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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

    private static final Logger log = LoggerFactory.getLogger(BenchmarkRunnerService.class);

    private final TestSetLoader testSetLoader;
    private final EvaluationService evaluationService;
    private final VectorStoreService vectorStoreService;
    private final LlmInferenceService geminiLlmService;
    private final LlmInferenceService ollamaLlmService;
    private final EmbeddingService embeddingService;
    private final BenchmarkResultRepository benchmarkResultRepository;
    private final BenchmarkJobRegistry jobRegistry;

    public BenchmarkRunnerService(TestSetLoader testSetLoader,
                                  EvaluationService evaluationService,
                                  VectorStoreService vectorStoreService,
                                  @Qualifier("geminiLlm") LlmInferenceService geminiLlmService,
                                  @Qualifier("ollamaLlm") LlmInferenceService ollamaLlmService,
                                  EmbeddingService embeddingService,
                                  BenchmarkResultRepository benchmarkResultRepository,
                                  BenchmarkJobRegistry jobRegistry) {
        this.testSetLoader = testSetLoader;
        this.evaluationService = evaluationService;
        this.vectorStoreService = vectorStoreService;
        this.geminiLlmService = geminiLlmService;
        this.ollamaLlmService = ollamaLlmService;
        this.embeddingService = embeddingService;
        this.benchmarkResultRepository = benchmarkResultRepository;
        this.jobRegistry = jobRegistry;
    }

    /**
     * Chạy benchmark bất đồng bộ cho một cấu hình.
     *
     * Luồng xử lý:
     * 1. Đánh dấu job RUNNING
     * 2. Duyệt qua từng test case
     *    - RAG_SYSTEM: embed → vector search (collection động) → Gemini generate
     *    - FINE_TUNED_MODEL: gửi thẳng câu hỏi sang Ollama
     * 3. Đo latency thực tế bằng System.currentTimeMillis()
     * 4. Lưu kết quả + retrieved contexts text vào DB
     * 5. Cập nhật tiến trình cho job registry
     * 6. Đánh dấu COMPLETED hoặc FAILED
     */
    @Async
    public void runBenchmark(String jobId, BenchmarkConfig config) {
        log.info("▶ Bắt đầu benchmark job={} | config={}", jobId, config);

        try {
            jobRegistry.markRunning(jobId);
            List<TestCase> testCases = testSetLoader.loadTestCases();

            ExperimentType expType = ExperimentType.valueOf(config.experimentType());
            int doneCount = 0;

            for (TestCase testCase : testCases) {
                long startMs = System.currentTimeMillis();

                List<RetrievedContext> retrievedContexts;
                LlmAnswer llmAnswer;

                if (expType == ExperimentType.RAG_SYSTEM) {
                    // ── Luồng RAG: embed → vector search → LLM generate (Gemini) ──
                    List<Float> questionEmbedding = embeddingService.embed(testCase.question());
                    retrievedContexts = vectorStoreService.search(
                            questionEmbedding, 5, null, null, config.collectionName());
                    llmAnswer = geminiLlmService.generateAnswer(
                            testCase.question(), new ArrayList<>(), retrievedContexts);
                } else {
                    // ── Luồng FINE_TUNED_MODEL: gửi thẳng sang Ollama, không retrieval ──
                    retrievedContexts = List.of();
                    llmAnswer = ollamaLlmService.generateAnswer(
                            testCase.question(), new ArrayList<>(), List.of());
                }

                long latencyMs = System.currentTimeMillis() - startMs;

                // Trích xuất danh sách chuỗi ngữ cảnh để chấm điểm
                List<String> contextsForEvaluation = retrievedContexts.stream()
                        .map(RetrievedContext::content)
                        .toList();

                // Ghép ngữ cảnh thành text để lưu DB (bàn giao cho ragas-service)
                String contextsText = retrievedContexts.stream()
                        .map(RetrievedContext::content)
                        .collect(Collectors.joining("\n---\n"));

                // Chấm điểm sử dụng llmAnswer.answer()
                EvaluationResult evalResult = evaluationService.evaluate(
                        testCase.question(),
                        testCase.groundTruth(),
                        llmAnswer.answer(),
                        contextsForEvaluation
                );

                // Khởi tạo Entity và lưu Database
                BenchmarkResult result = new BenchmarkResult();
                result.setExperimentType(expType);
                result.setChunkingStrategy(ChunkingStrategy.valueOf(config.strategy()));
                result.setEmbeddingModel(EmbeddingModel.valueOf(config.embeddingModel()));

                result.setQuestion(testCase.question());
                result.setGroundTruth(testCase.groundTruth());
                result.setGeneratedAnswer(llmAnswer.answer());
                result.setRetrievedContextsText(contextsText);

                result.setExactMatch(evalResult.exactMatch());
                result.setF1Score(evalResult.f1());
                result.setFaithfulness(evalResult.faithfulness());
                result.setAnswerRelevancy(evalResult.answerRelevancy());
                result.setContextPrecision(evalResult.contextPrecision());
                result.setContextRecall(evalResult.contextRecall());
                result.setRetrievalHit(computeRetrievalHit(testCase.groundTruth(), retrievedContexts));

                result.setLatencyMs(latencyMs);
                result.setCostUsd(BigDecimal.ZERO);

                benchmarkResultRepository.save(result);

                doneCount++;
                jobRegistry.updateProgress(jobId, doneCount);
                log.info("  ✓ Test case {} ({}/{}) | latency={}ms | F1={}",
                        testCase.id(), doneCount, testCases.size(), latencyMs, evalResult.f1());
            }

            jobRegistry.markCompleted(jobId);
            log.info("✅ Benchmark job={} hoàn thành | total={}", jobId, doneCount);

        } catch (Exception ex) {
            log.error("❌ Benchmark job={} thất bại: {}", jobId, ex.getMessage(), ex);
            jobRegistry.markFailed(jobId, ex.getMessage());
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

    /**
     * Cấu hình cho một lần chạy benchmark.
     *
     * @param strategy       Tên enum ChunkingStrategy (FIXED_SIZE | SEMANTIC | HIERARCHICAL)
     * @param embeddingModel Tên enum EmbeddingModel
     * @param experimentType Tên enum ExperimentType (RAG_SYSTEM | FINE_TUNED_MODEL)
     * @param collectionName Tên Qdrant collection (null = dùng collection mặc định)
     */
    public record BenchmarkConfig(
            String strategy,
            String embeddingModel,
            String experimentType,
            String collectionName
    ) {}

}
