package com.ragchatbot.infrastructure.benchmark;

import com.ragchatbot.application.usecase.document.DocumentMaintenanceService;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.enums.ExperimentType;
import com.ragchatbot.domain.model.BenchmarkResult;
import com.ragchatbot.domain.model.RetrievalMetrics;
import com.ragchatbot.domain.model.TestCase;
import com.ragchatbot.domain.port.FineTunedInferenceService;
import com.ragchatbot.domain.port.LlmAnswer;
import com.ragchatbot.domain.port.LlmInferenceService;
import com.ragchatbot.domain.port.RetrievedContext;
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.embedding.EmbeddingRouter;
import com.ragchatbot.infrastructure.persistence.BenchmarkResultRepository;
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

            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                
                String configKey = config.strategy() + ":" + config.embeddingModel() + ":" + config.topK();

                // Kiểm tra xem câu hỏi này đã được chấm xong ở lần chạy trước chưa
                boolean exists = benchmarkResultRepository.existsByRunIdAndQuestionIdAndConfigKeyAndItemStatus(
                        config.runId(), testCase.id(), configKey, "COMPLETED");
                
                if (exists) {
                    continue; 
                }

                List<RetrievedContext> retrievedContexts = Collections.emptyList();
                boolean retrievalHit = false;
                String generatedAnswer = null;
                long embeddingMs = 0;
                long searchMs = 0;
                long totalLatencyMs = 0;

                // ==========================================
                // TẦNG 1: RETRIEVAL
                // ==========================================
                long startTotal = System.nanoTime();
                if (experimentType == ExperimentType.RAG) {
                    long e0 = System.nanoTime();
                    List<Float> questionEmbedding = embeddingRouter.embed(embeddingModel, testCase.question());
                    embeddingMs = elapsedMillis(e0);

                    long s0 = System.nanoTime();
                    retrievedContexts = vectorStoreService.search(
                            embeddingModel, questionEmbedding, config.topK(), chunkingStrategy, null, null, null
                    );
                    searchMs = elapsedMillis(s0);
                    
                    retrievalHit = computeRetrievalHit(testCase, retrievedContexts);
                }

                BenchmarkResult benchmarkResult = new BenchmarkResult();
                benchmarkResult.setRunId(config.runId());
                benchmarkResult.setQuestionId(testCase.id());
                benchmarkResult.setBenchmarkMode(config.benchmarkMode().name());
                benchmarkResult.setExperimentType(experimentType);
                benchmarkResult.setChunkingStrategy(experimentType == ExperimentType.RAG ? chunkingStrategy : null);
                benchmarkResult.setEmbeddingModel(experimentType == ExperimentType.RAG ? embeddingModel : null);
                benchmarkResult.setQuestion(testCase.question());
                benchmarkResult.setGroundTruth(testCase.groundTruth());
                benchmarkResult.setRetrievalHit(retrievalHit);
                benchmarkResult.setEmbeddingLatencyMs(embeddingMs);
                benchmarkResult.setSearchLatencyMs(searchMs);

                // NGẮT LUỒNG NẾU LÀ RETRIEVAL_ONLY
                if (config.benchmarkMode() != com.ragchatbot.domain.enums.BenchmarkMode.RETRIEVAL_ONLY) {
                    
                    // ==========================================
                    // TẦNG 2 & 3: GENERATION VÀ EVALUATION
                    // ==========================================
                    if (experimentType == ExperimentType.RAG) {
                        LlmAnswer llmAnswer = llmInferenceService.generateAnswer(testCase.question(), new ArrayList<>(), retrievedContexts);
                        generatedAnswer = llmAnswer.answer();
                    } else {
                        generatedAnswer = fineTunedInferenceService.generateAnswer(testCase.question());
                    }
                    totalLatencyMs = elapsedMillis(startTotal);

                    List<String> contextsForEvaluation = retrievedContexts.stream().map(RetrievedContext::content).toList();

                    RagasEvaluationService.EvaluationDetails evaluationDetails = evaluationService.evaluateDetailed(
                            testCase.question(), testCase.groundTruth(), generatedAnswer, contextsForEvaluation
                    );

                    benchmarkResult.setGeneratedAnswer(generatedAnswer);
                    benchmarkResult.setF1Score(evaluationDetails.result().f1());
                    benchmarkResult.setFaithfulness(evaluationDetails.result().faithfulness());
                    benchmarkResult.setAnswerRelevancy(evaluationDetails.result().answerRelevancy());
                    benchmarkResult.setContextPrecision(evaluationDetails.result().contextPrecision());
                    benchmarkResult.setContextRecall(evaluationDetails.result().contextRecall());
                    benchmarkResult.setEvaluationSource(evaluationDetails.source());
                    benchmarkResult.setEvaluationFallbackUsed(evaluationDetails.fallbackUsed());
                    benchmarkResult.setLatencyMs(totalLatencyMs);
                }

                benchmarkResult.setItemStatus("COMPLETED");

                // --- PHẦN BẮT LỖI MỚI ---
                try {
                    System.out.println("DEBUG: Đang chuẩn bị lưu kết quả cho ID: " + testCase.id());
                    benchmarkResultRepository.save(benchmarkResult);
                    System.out.println("DEBUG: LƯU THÀNH CÔNG ID: " + testCase.id());
                    benchmarkJobRegistry.updateProgress(jobId, i + 1);
                } catch (Exception e) {
                    System.err.println("!!! LỖI NGHIÊM TRỌNG KHI LƯU ID " + testCase.id() + ": " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException("Lưu dữ liệu thất bại tại ID: " + testCase.id(), e);
                }
            }

            benchmarkJobRegistry.markCompleted(jobId);
        } catch (Exception ex) {
            System.err.println("!!! LỖI TỔNG THỂ KHI CHẠY JOB: " + ex.getMessage());
            ex.printStackTrace();
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
        if (Boolean.TRUE.equals(testCase.outOfScope()) || retrievedContexts == null || retrievedContexts.isEmpty()) {
            return false;
        }

        return testCase.relevantSources().stream().anyMatch(src -> 
            retrievedContexts.stream().anyMatch(ctx -> {
                if (!src.documentId().equals(ctx.documentId().toString())) return false;
                
                if (ctx.pageStart() != null && ctx.pageEnd() != null && src.pageStart() != null && src.pageEnd() != null) {
                    return ctx.pageStart() <= src.pageEnd() && ctx.pageEnd() >= src.pageStart();
                }
                
                if (ctx.section() != null && src.section() != null) {
                    String ctxSec = ctx.section().toLowerCase(Locale.ROOT).trim();
                    String srcSec = src.section().toLowerCase(Locale.ROOT).trim();
                    return ctxSec.contains(srcSec) || srcSec.contains(ctxSec);
                }
                return true; 
            })
        );
    }
    
    private long elapsedMillis(long startedAtNanos) {
        return Math.max(0L, (System.nanoTime() - startedAtNanos) / 1_000_000L);
    }
    
    public RetrievalMetrics evaluate(TestCase tc, List<RetrievedContext> contexts, int topK) {
        List<Integer> relevantRanks = new ArrayList<>();
        for (int i = 0; i < contexts.size(); i++) {
            if (isRelevant(tc, contexts.get(i))) {
                relevantRanks.add(i + 1);
            }
        }

        double hitAtK = relevantRanks.isEmpty() ? 0.0 : 1.0;
        double recallAtK = (double) relevantRanks.size() / tc.relevantSources().size();
        double mrr = relevantRanks.isEmpty() ? 0.0 : 1.0 / relevantRanks.get(0);
        
        return new RetrievalMetrics(hitAtK, recallAtK, 0.0, mrr, 0.0, relevantRanks.size(), tc.relevantSources().size(), 
                                    relevantRanks.isEmpty() ? null : relevantRanks.get(0));
    }
    
    private boolean isRelevant(TestCase tc, RetrievedContext ctx) {
        if (tc.relevantSources() == null || tc.relevantSources().isEmpty()) return false;

        return tc.relevantSources().stream().anyMatch(src -> {
            boolean isSameDoc = src.documentId().equals(ctx.documentId().toString());
            if (!isSameDoc) return false;

            boolean pageOverlap = false;
            if (ctx.pageStart() != null && ctx.pageEnd() != null && src.pageStart() != null && src.pageEnd() != null) {
                pageOverlap = (ctx.pageStart() <= src.pageEnd()) && (ctx.pageEnd() >= src.pageStart());
            }

            boolean sectionMatch = false;
            if (ctx.section() != null && src.section() != null) {
                String ctxSec = ctx.section().toLowerCase(Locale.ROOT).trim();
                String srcSec = src.section().toLowerCase(Locale.ROOT).trim();
                sectionMatch = ctxSec.contains(srcSec) || srcSec.contains(srcSec);
            }

            return pageOverlap || sectionMatch;
        });
    }

    public record BenchmarkConfig(
            String strategy,
            String embeddingModel,
            String experimentType,
            com.ragchatbot.domain.enums.BenchmarkMode benchmarkMode,
            String runId,
            Integer topK
    ) {
    }
    
}