package com.ragchatbot.infrastructure.benchmark;

import com.ragchatbot.application.usecase.document.DocumentMaintenanceService;
import com.ragchatbot.domain.enums.BenchmarkMode;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.enums.ExperimentType;
import com.ragchatbot.domain.model.BenchmarkResult;
import com.ragchatbot.domain.model.RelevantSource;
import com.ragchatbot.domain.model.RetrievalMetrics;
import com.ragchatbot.domain.model.TestCase;
import com.ragchatbot.domain.port.FineTunedInferenceService;
import com.ragchatbot.domain.port.LlmAnswer;
import com.ragchatbot.domain.port.LlmInferenceService;
import com.ragchatbot.domain.port.RetrievedContext;
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.embedding.EmbeddingRouter;
import com.ragchatbot.infrastructure.persistence.BenchmarkResultRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

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

            ExperimentType experimentType =
                    ExperimentType.valueOf(config.experimentType());

            ChunkingStrategy chunkingStrategy =
                    parseChunkingStrategy(config.strategy());

            EmbeddingModel embeddingModel =
                    parseEmbeddingModel(config.embeddingModel());

            int topK = resolveTopK(config.topK());

            String configKey = buildConfigKey(
                    chunkingStrategy,
                    embeddingModel,
                    topK
            );

            int failedItems = 0;

            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);

                boolean exists =
                        benchmarkResultRepository
                                .existsByRunIdAndQuestionIdAndConfigKeyAndItemStatus(
                                        config.runId(),
                                        testCase.id(),
                                        configKey,
                                        "COMPLETED"
                                );

                if (exists) {
                    benchmarkJobRegistry.updateProgress(jobId, i + 1);
                    continue;
                }

                BenchmarkResult benchmarkResult =
                        createBaseResult(
                                config,
                                configKey,
                                experimentType,
                                chunkingStrategy,
                                embeddingModel,
                                testCase
                        );

                try {
                    processTestCase(
                            config,
                            topK,
                            experimentType,
                            chunkingStrategy,
                            embeddingModel,
                            testCase,
                            benchmarkResult
                    );

                    benchmarkResult.setItemStatus("COMPLETED");
                    benchmarkResult.setErrorMessage(null);
                } catch (Exception itemException) {
                    failedItems++;
                    benchmarkResult.setItemStatus("FAILED");
                    benchmarkResult.setErrorMessage(
                            simplifyErrorMessage(itemException)
                    );

                    System.err.println(
                            "Benchmark item failed | questionId="
                                    + testCase.id()
                                    + " | error="
                                    + itemException.getMessage()
                    );
                    itemException.printStackTrace();
                }

                try {
                    benchmarkResultRepository.save(benchmarkResult);
                    benchmarkJobRegistry.updateProgress(jobId, i + 1);
                } catch (Exception saveException) {
                    throw new IllegalStateException(
                            "Lưu benchmark result thất bại tại questionId="
                                    + testCase.id(),
                            saveException
                    );
                }
            }

            if (failedItems == 0) {
                benchmarkJobRegistry.markCompleted(jobId);
            } else {
                benchmarkJobRegistry.markFailed(
                        jobId,
                        "Benchmark xử lý xong nhưng có "
                                + failedItems
                                + " test case FAILED. "
                                + "Kiểm tra item_status và error_message."
                );
            }
        } catch (Exception exception) {
            System.err.println(
                    "Benchmark job failed | jobId="
                            + jobId
                            + " | error="
                            + exception.getMessage()
            );
            exception.printStackTrace();

            benchmarkJobRegistry.markFailed(
                    jobId,
                    simplifyErrorMessage(exception)
            );
        }
    }

    private BenchmarkResult createBaseResult(
            BenchmarkConfig config,
            String configKey,
            ExperimentType experimentType,
            ChunkingStrategy chunkingStrategy,
            EmbeddingModel embeddingModel,
            TestCase testCase
    ) {
        BenchmarkResult result = new BenchmarkResult();

        result.setRunId(config.runId());
        result.setConfigKey(configKey);
        result.setQuestionId(testCase.id());
        result.setBenchmarkMode(config.benchmarkMode().name());
        result.setExperimentType(experimentType);

        result.setChunkingStrategy(
                experimentType == ExperimentType.RAG
                        ? chunkingStrategy
                        : null
        );

        result.setEmbeddingModel(
                experimentType == ExperimentType.RAG
                        ? embeddingModel
                        : null
        );

        result.setQuestion(testCase.question());
        result.setGroundTruth(testCase.groundTruth());

        return result;
    }

    private void processTestCase(
            BenchmarkConfig config,
            int topK,
            ExperimentType experimentType,
            ChunkingStrategy chunkingStrategy,
            EmbeddingModel embeddingModel,
            TestCase testCase,
            BenchmarkResult benchmarkResult
    ) {
        List<RetrievedContext> retrievedContexts =
                Collections.emptyList();

        RetrievalMetrics retrievalMetrics =
                emptyRetrievalMetrics(testCase);

        String generatedAnswer = null;
        long embeddingMs = 0L;
        long searchMs = 0L;
        long startedAt = System.nanoTime();

        if (experimentType == ExperimentType.RAG) {
            long embeddingStartedAt = System.nanoTime();

            List<Float> questionEmbedding =
                    embeddingRouter.embed(
                            embeddingModel,
                            testCase.question()
                    );

            embeddingMs = elapsedMillis(embeddingStartedAt);

            long searchStartedAt = System.nanoTime();

            retrievedContexts = vectorStoreService.search(
                    embeddingModel,
                    questionEmbedding,
                    topK,
                    chunkingStrategy,
                    null,
                    null,
                    null
            );

            searchMs = elapsedMillis(searchStartedAt);

            retrievalMetrics = evaluate(
                    testCase,
                    retrievedContexts,
                    topK
            );
        }

        benchmarkResult.setRetrievalHit(
                retrievalMetrics.hitAtK() > 0.0
        );
        benchmarkResult.setHitAtK(
                retrievalMetrics.hitAtK()
        );
        benchmarkResult.setRecallAtK(
                retrievalMetrics.recallAtK()
        );
        benchmarkResult.setReciprocalRank(
                retrievalMetrics.reciprocalRank()
        );
        benchmarkResult.setNdcgAtK(
                retrievalMetrics.ndcgAtK()
        );
        benchmarkResult.setEmbeddingLatencyMs(embeddingMs);
        benchmarkResult.setSearchLatencyMs(searchMs);

        if (config.benchmarkMode() != BenchmarkMode.RETRIEVAL_ONLY) {
            if (experimentType == ExperimentType.RAG) {
                LlmAnswer llmAnswer =
                        llmInferenceService.generateAnswer(
                                testCase.question(),
                                new ArrayList<>(),
                                retrievedContexts
                        );

                generatedAnswer = llmAnswer.answer();
            } else {
                generatedAnswer =
                        fineTunedInferenceService.generateAnswer(
                                testCase.question()
                        );
            }

            List<String> contextsForEvaluation =
                    retrievedContexts.stream()
                            .map(RetrievedContext::content)
                            .toList();

            RagasEvaluationService.EvaluationDetails evaluationDetails =
                    evaluationService.evaluateDetailed(
                            testCase.question(),
                            testCase.groundTruth(),
                            generatedAnswer,
                            contextsForEvaluation
                    );

            benchmarkResult.setGeneratedAnswer(generatedAnswer);
            benchmarkResult.setF1Score(
                    evaluationDetails.result().f1()
            );
            benchmarkResult.setFaithfulness(
                    evaluationDetails.result().faithfulness()
            );
            benchmarkResult.setAnswerRelevancy(
                    evaluationDetails.result().answerRelevancy()
            );
            benchmarkResult.setContextPrecision(
                    evaluationDetails.result().contextPrecision()
            );
            benchmarkResult.setContextRecall(
                    evaluationDetails.result().contextRecall()
            );
            benchmarkResult.setEvaluationSource(
                    evaluationDetails.source()
            );
            benchmarkResult.setEvaluationFallbackUsed(
                    evaluationDetails.fallbackUsed()
            );
        }

        benchmarkResult.setLatencyMs(
                elapsedMillis(startedAt)
        );
    }

    public RetrievalMetrics evaluate(
            TestCase testCase,
            List<RetrievedContext> contexts,
            int topK
    ) {
        int totalRelevant =
                testCase.relevantSources() == null
                        ? 0
                        : testCase.relevantSources().size();

        if (Boolean.TRUE.equals(testCase.outOfScope())
                || totalRelevant == 0
                || contexts == null
                || contexts.isEmpty()
                || topK <= 0) {
            return new RetrievalMetrics(
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0,
                    totalRelevant,
                    null
            );
        }

        int limit = Math.min(topK, contexts.size());

        Set<Integer> matchedSourceIndexes =
                new HashSet<>();

        List<Integer> relevantRanks =
                new ArrayList<>();

        for (int i = 0; i < limit; i++) {
            RetrievedContext context = contexts.get(i);

            int matchingSourceIndex =
                    findUnmatchedRelevantSourceIndex(
                            testCase,
                            context,
                            matchedSourceIndexes
                    );

            if (matchingSourceIndex >= 0) {
                matchedSourceIndexes.add(
                        matchingSourceIndex
                );
                relevantRanks.add(i + 1);
            }
        }

        int relevantRetrieved =
                matchedSourceIndexes.size();

        double hitAtK =
                relevantRetrieved > 0 ? 1.0 : 0.0;

        double recallAtK =
                (double) relevantRetrieved
                        / totalRelevant;

        double precisionAtK =
                (double) relevantRetrieved
                        / topK;

        double reciprocalRank =
                relevantRanks.isEmpty()
                        ? 0.0
                        : 1.0 / relevantRanks.get(0);

        double dcg = 0.0;

        for (Integer rank : relevantRanks) {
            dcg += 1.0 / log2(rank + 1.0);
        }

        int idealRelevantCount =
                Math.min(totalRelevant, topK);

        double idcg = 0.0;

        for (int rank = 1;
             rank <= idealRelevantCount;
             rank++) {
            idcg += 1.0 / log2(rank + 1.0);
        }

        double ndcgAtK =
                idcg == 0.0
                        ? 0.0
                        : dcg / idcg;

        Integer firstRelevantRank =
                relevantRanks.isEmpty()
                        ? null
                        : relevantRanks.get(0);

        return new RetrievalMetrics(
                hitAtK,
                recallAtK,
                precisionAtK,
                reciprocalRank,
                ndcgAtK,
                relevantRetrieved,
                totalRelevant,
                firstRelevantRank
        );
    }

    private int findUnmatchedRelevantSourceIndex(
            TestCase testCase,
            RetrievedContext context,
            Set<Integer> matchedSourceIndexes
    ) {
        List<RelevantSource> sources =
                testCase.relevantSources();

        for (int i = 0; i < sources.size(); i++) {
            if (matchedSourceIndexes.contains(i)) {
                continue;
            }

            if (matchesRelevantSource(
                    sources.get(i),
                    context
            )) {
                return i;
            }
        }

        return -1;
    }

    private RetrievalMetrics emptyRetrievalMetrics(
            TestCase testCase
    ) {
        int totalRelevant =
                testCase.relevantSources() == null
                        ? 0
                        : testCase.relevantSources().size();

        return new RetrievalMetrics(
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0,
                totalRelevant,
                null
        );
    }

    private boolean isRelevant(
            TestCase testCase,
            RetrievedContext context
    ) {
        if (testCase.relevantSources() == null
                || testCase.relevantSources().isEmpty()) {
            return false;
        }

        return testCase.relevantSources().stream()
                .anyMatch(
                        source ->
                                matchesRelevantSource(
                                        source,
                                        context
                                )
                );
    }

    private boolean matchesRelevantSource(
            RelevantSource source,
            RetrievedContext context
    ) {
        if (!isSameSource(source, context)) {
            return false;
        }

        boolean hasPageConstraint =
                source.pageStart() != null
                        || source.pageEnd() != null;

        boolean hasSectionConstraint =
                StringUtils.hasText(source.section());

        if (!hasPageConstraint
                && !hasSectionConstraint) {
            return true;
        }

        boolean pageMatches =
                hasPageConstraint
                        && hasPageOverlap(source, context);

        boolean sectionMatches =
                hasSectionConstraint
                        && hasSectionMatch(
                                source.section(),
                                context.section()
                        );

        return pageMatches || sectionMatches;
    }

    private boolean isSameSource(
            RelevantSource source,
            RetrievedContext context
    ) {
        if (StringUtils.hasText(
                source.sourceFileName()
        )) {
            if (!StringUtils.hasText(
                    context.sourceFileName()
            )) {
                return false;
            }

            String expectedFileName =
                    normalizeSourceFileName(
                            source.sourceFileName()
                    );

            String actualFileName =
                    normalizeSourceFileName(
                            context.sourceFileName()
                    );

            return expectedFileName.equals(
                    actualFileName
            );
        }

        if (!isUuid(source.documentId())
                || context.documentId() == null) {
            return false;
        }

        return UUID.fromString(
                        source.documentId().trim()
                )
                .equals(context.documentId());
    }

    private boolean hasPageOverlap(
            RelevantSource source,
            RetrievedContext context
    ) {
        Integer expectedStart =
                source.pageStart() != null
                        ? source.pageStart()
                        : source.pageEnd();

        Integer expectedEnd =
                source.pageEnd() != null
                        ? source.pageEnd()
                        : source.pageStart();

        Integer actualStart =
                context.pageStart() != null
                        ? context.pageStart()
                        : context.pageNumber() != null
                                ? context.pageNumber()
                                : context.pageEnd();

        Integer actualEnd =
                context.pageEnd() != null
                        ? context.pageEnd()
                        : context.pageNumber() != null
                                ? context.pageNumber()
                                : context.pageStart();

        if (expectedStart == null
                || expectedEnd == null
                || actualStart == null
                || actualEnd == null) {
            return false;
        }

        int normalizedExpectedStart =
                Math.min(expectedStart, expectedEnd);

        int normalizedExpectedEnd =
                Math.max(expectedStart, expectedEnd);

        int normalizedActualStart =
                Math.min(actualStart, actualEnd);

        int normalizedActualEnd =
                Math.max(actualStart, actualEnd);

        return normalizedActualStart
                <= normalizedExpectedEnd
                && normalizedActualEnd
                >= normalizedExpectedStart;
    }

    private boolean hasSectionMatch(
            String expectedSection,
            String actualSection
    ) {
        if (!StringUtils.hasText(expectedSection)
                || !StringUtils.hasText(actualSection)) {
            return false;
        }

        String expected =
                normalizeText(expectedSection);

        String actual =
                normalizeText(actualSection);

        return actual.contains(expected)
                || expected.contains(actual);
    }

    private String normalizeSourceFileName(
            String value
    ) {
        String normalized = value
                .trim()
                .replace('\\', '/');

        int lastSlash =
                normalized.lastIndexOf('/');

        if (lastSlash >= 0) {
            normalized =
                    normalized.substring(
                            lastSlash + 1
                    );
        }

        return normalizeText(normalized);
    }

    private String normalizeText(String value) {
        return Normalizer
                .normalize(
                        value.trim(),
                        Normalizer.Form.NFC
                )
                .toLowerCase(Locale.ROOT);
    }

    private boolean isUuid(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }

        try {
            UUID.fromString(value.trim());
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private int resolveTopK(Integer topK) {
        int resolved = topK == null ? 5 : topK;

        if (resolved <= 0) {
            throw new IllegalArgumentException(
                    "topK phải lớn hơn 0."
            );
        }

        return resolved;
    }

    private String buildConfigKey(
            ChunkingStrategy chunkingStrategy,
            EmbeddingModel embeddingModel,
            int topK
    ) {
        return chunkingStrategy.name()
                + ":"
                + embeddingModel.name()
                + ":"
                + topK;
    }

    private double log2(double value) {
        return Math.log(value)
                / Math.log(2.0);
    }

    private long elapsedMillis(
            long startedAtNanos
    ) {
        return Math.max(
                0L,
                (System.nanoTime()
                        - startedAtNanos)
                        / 1_000_000L
        );
    }

    private EmbeddingModel parseEmbeddingModel(
            String value
    ) {
        if (!StringUtils.hasText(value)) {
            return EmbeddingModel.GEMINI_EMBEDDING_001;
        }

        EmbeddingModel embeddingModel =
                EmbeddingModel.valueOf(
                        value.trim()
                                .toUpperCase(Locale.ROOT)
                );

        if (!embeddingModel
                .isAllowedForNewRequests()) {
            throw new IllegalArgumentException(
                    "Embedding model "
                            + embeddingModel
                            + " đã ngừng hỗ trợ "
                            + "cho benchmark mới."
            );
        }

        return embeddingModel;
    }

    private ChunkingStrategy parseChunkingStrategy(
            String value
    ) {
        if (!StringUtils.hasText(value)) {
            return ChunkingStrategy.SEMANTIC;
        }

        return ChunkingStrategy.valueOf(
                value.trim()
                        .toUpperCase(Locale.ROOT)
        );
    }

    private String simplifyErrorMessage(
            Exception exception
    ) {
        String message = exception.getMessage();

        if (!StringUtils.hasText(message)) {
            return "Benchmark thất bại do lỗi không xác định.";
        }

        String normalized =
                message.toLowerCase(Locale.ROOT);

        if (message.contains("429")
                || normalized.contains("quota exceeded")) {
            return "Gemini hết quota hoặc bị giới hạn tốc độ.";
        }

        return message;
    }

    public record BenchmarkConfig(
            String strategy,
            String embeddingModel,
            String experimentType,
            BenchmarkMode benchmarkMode,
            String runId,
            Integer topK
    ) {
    }
}