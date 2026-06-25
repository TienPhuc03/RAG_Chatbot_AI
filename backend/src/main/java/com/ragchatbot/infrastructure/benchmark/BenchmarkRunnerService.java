package com.ragchatbot.infrastructure.benchmark;

import com.ragchatbot.domain.model.TestCase;
import com.ragchatbot.domain.model.EvaluationResult;
import com.ragchatbot.domain.model.BenchmarkResult;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.enums.ExperimentType;
import com.ragchatbot.domain.port.EvaluationService;
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.domain.port.EmbeddingService;
import com.ragchatbot.domain.port.LlmInferenceService;
import com.ragchatbot.domain.port.RetrievedContext;
import com.ragchatbot.domain.port.LlmAnswer;
import com.ragchatbot.infrastructure.persistence.BenchmarkResultRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class BenchmarkRunnerService {

    private final TestSetLoader testSetLoader;
    private final EvaluationService evaluationService;
    private final VectorStoreService vectorStoreService;
    private final LlmInferenceService llmInferenceService;
    private final EmbeddingService embeddingService;
    private final BenchmarkResultRepository benchmarkResultRepository;
    private final BenchmarkJobRegistry jobRegistry;

    public BenchmarkRunnerService(TestSetLoader testSetLoader,
                                  EvaluationService evaluationService,
                                  VectorStoreService vectorStoreService,
                                  LlmInferenceService llmInferenceService,
                                  EmbeddingService embeddingService,
                                  BenchmarkResultRepository benchmarkResultRepository,
                                  BenchmarkJobRegistry jobRegistry) {
        this.testSetLoader = testSetLoader;
        this.evaluationService = evaluationService;
        this.vectorStoreService = vectorStoreService;
        this.llmInferenceService = llmInferenceService;
        this.embeddingService = embeddingService;
        this.benchmarkResultRepository = benchmarkResultRepository;
        this.jobRegistry = jobRegistry;
    }

    /**
     * Chạy benchmark bất đồng bộ.
     *
     * Flow:
     *  1. Đánh dấu job RUNNING
     *  2. Load test cases, lặp qua từng câu hỏi
     *  3. Embed → Search → Generate → Evaluate → Save DB
     *  4. Cập nhật tiến trình (doneCases) sau mỗi test case
     *  5. Đánh dấu COMPLETED hoặc FAILED khi xong
     *
     * @param jobId  UUID được tạo và đăng ký bởi controller trước khi gọi method này
     * @param config Tham số cấu hình của lần chạy
     */
    @Async
    public void runBenchmark(String jobId, BenchmarkConfig config) {
        try {
            jobRegistry.markRunning(jobId);

            List<TestCase> testCases = testSetLoader.loadTestCases();
            int done = 0;

            for (TestCase testCase : testCases) {

                // 1. Nhúng câu hỏi thành vector
                List<Float> questionEmbedding = embeddingService.embed(testCase.question());

                // 2. Tìm kiếm ngữ cảnh liên quan
                List<RetrievedContext> retrievedContexts =
                        vectorStoreService.search(questionEmbedding, 5, null, null);

                // 3. Sinh câu trả lời từ LLM
                LlmAnswer llmAnswer = llmInferenceService.generateAnswer(
                        testCase.question(), new ArrayList<>(), retrievedContexts);

                // 4. Trích xuất chuỗi ngữ cảnh để chấm điểm
                List<String> contextsForEvaluation = retrievedContexts.stream()
                        .map(RetrievedContext::content)
                        .toList();

                // 5. Chấm điểm
                EvaluationResult evalResult = evaluationService.evaluate(
                        testCase.question(),
                        testCase.groundTruth(),
                        llmAnswer.answer(),
                        contextsForEvaluation
                );

                // 6. Tạo entity và lưu DB
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

                result.setLatencyMs(0L);
                result.setCostUsd(BigDecimal.ZERO);

                benchmarkResultRepository.save(result);

                // 7. Cập nhật tiến trình
                done++;
                jobRegistry.updateProgress(jobId, done);
                System.out.println("[Benchmark job=" + jobId + "] Done " + done + "/" + testCases.size()
                        + " | testCase=" + testCase.id() + " | F1=" + evalResult.f1());
            }

            jobRegistry.markCompleted(jobId);

        } catch (Exception ex) {
            jobRegistry.markFailed(jobId, ex.getMessage());
            System.err.println("[Benchmark job=" + jobId + "] FAILED: " + ex.getMessage());
        }
    }

    /**
     * Cấu hình cho một lần chạy benchmark.
     *
     * @param strategy      Tên ChunkingStrategy enum (ví dụ: "FIXED_SIZE")
     * @param embeddingModel Tên EmbeddingModel enum (ví dụ: "MULTILINGUAL_E5_BASE")
     * @param experimentType Tên ExperimentType enum (ví dụ: "RAG")
     */
    public record BenchmarkConfig(
            String strategy,
            String embeddingModel,
            String experimentType
    ) {}
}