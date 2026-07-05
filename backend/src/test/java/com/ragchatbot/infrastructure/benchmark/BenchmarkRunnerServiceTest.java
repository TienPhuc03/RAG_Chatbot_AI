package com.ragchatbot.infrastructure.benchmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ragchatbot.application.usecase.document.DocumentMaintenanceService;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.model.BenchmarkResult;
import com.ragchatbot.domain.model.EvaluationResult;
import com.ragchatbot.domain.model.TestCase;
import com.ragchatbot.domain.port.FineTunedInferenceService;
import com.ragchatbot.domain.port.LlmAnswer;
import com.ragchatbot.domain.port.LlmInferenceService;
import com.ragchatbot.domain.port.RetrievedContext;
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.embedding.EmbeddingRouter;
import com.ragchatbot.infrastructure.persistence.BenchmarkResultRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BenchmarkRunnerServiceTest {

    @Test
    void updatesJobStatusUntilCompleted() {
        TestSetLoader testSetLoader = mock(TestSetLoader.class);
        RagasEvaluationService evaluationService = mock(RagasEvaluationService.class);
        VectorStoreService vectorStoreService = mock(VectorStoreService.class);
        LlmInferenceService llmInferenceService = mock(LlmInferenceService.class);
        FineTunedInferenceService fineTunedInferenceService = mock(FineTunedInferenceService.class);
        EmbeddingRouter embeddingRouter = mock(EmbeddingRouter.class);
        BenchmarkResultRepository benchmarkResultRepository = mock(BenchmarkResultRepository.class);
        BenchmarkJobRegistry benchmarkJobRegistry = new BenchmarkJobRegistry();
        DocumentMaintenanceService documentMaintenanceService = mock(DocumentMaintenanceService.class);
        BenchmarkCostEstimator benchmarkCostEstimator = mock(BenchmarkCostEstimator.class);

        when(testSetLoader.loadTestCases()).thenReturn(List.of(
                new TestCase("Q001", "Cau hoi", "Dap an", "DEFINITION")
        ));
        when(embeddingRouter.embed(EmbeddingModel.BGE_M3, "Cau hoi"))
                .thenReturn(List.of(0.1f, 0.2f));
        when(vectorStoreService.search(any(), any(), anyInt(), any(), any(), any(), any())).thenReturn(List.of(
                new RetrievedContext(UUID.randomUUID(), UUID.randomUUID(), "Dap an", 0.9, "DB101", "CH1", "db101.pdf", 2)
        ));
        when(llmInferenceService.generateAnswer(any(), any(), any())).thenReturn(
                new LlmAnswer("Dap an", List.of(), true)
        );
        when(documentMaintenanceService.hasIndexedDocumentsForBenchmark(ChunkingStrategy.SEMANTIC, EmbeddingModel.BGE_M3))
                .thenReturn(true);
        when(evaluationService.evaluateDetailed(any(), any(), any(), any())).thenReturn(
                new RagasEvaluationService.EvaluationDetails(
                        new EvaluationResult(1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
                        false,
                        "ragas-service:gemini",
                        "GEMINI",
                        false,
                        123L
                )
        );
        when(benchmarkCostEstimator.estimateRagCost(any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);

        BenchmarkRunnerService service = new BenchmarkRunnerService(
                testSetLoader,
                evaluationService,
                vectorStoreService,
                llmInferenceService,
                fineTunedInferenceService,
                embeddingRouter,
                benchmarkResultRepository,
                benchmarkJobRegistry,
                documentMaintenanceService,
                benchmarkCostEstimator
        );

        benchmarkJobRegistry.register("job-1", "test", 1);
        service.runBenchmark("job-1", new BenchmarkRunnerService.BenchmarkConfig(
                "SEMANTIC",
                "BGE_M3",
                "RAG"
        ));

        BenchmarkJobRegistry.JobSnapshot snapshot = benchmarkJobRegistry.get("job-1");
        assertThat(snapshot.status()).isEqualTo(BenchmarkJobStatus.COMPLETED);
        assertThat(snapshot.doneCases()).isEqualTo(1);
        verify(embeddingRouter).embed(EmbeddingModel.BGE_M3, "Cau hoi");
        verify(vectorStoreService).search(
                EmbeddingModel.BGE_M3,
                List.of(0.1f, 0.2f),
                5,
                ChunkingStrategy.SEMANTIC,
                null,
                null,
                null
        );

        ArgumentCaptor<BenchmarkResult> resultCaptor = ArgumentCaptor.forClass(BenchmarkResult.class);
        verify(benchmarkResultRepository).save(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getEmbeddingModel()).isEqualTo(EmbeddingModel.BGE_M3);
        assertThat(resultCaptor.getValue().getEvaluationFallbackUsed()).isFalse();
        assertThat(resultCaptor.getValue().getEvaluationSource()).isEqualTo("ragas-service:gemini");
    }

    @Test
    void finetuneBenchmarkSkipsVectorSearchAndCallsFineTunedService() {
        TestSetLoader testSetLoader = mock(TestSetLoader.class);
        RagasEvaluationService evaluationService = mock(RagasEvaluationService.class);
        VectorStoreService vectorStoreService = mock(VectorStoreService.class);
        LlmInferenceService llmInferenceService = mock(LlmInferenceService.class);
        FineTunedInferenceService fineTunedInferenceService = mock(FineTunedInferenceService.class);
        EmbeddingRouter embeddingRouter = mock(EmbeddingRouter.class);
        BenchmarkResultRepository benchmarkResultRepository = mock(BenchmarkResultRepository.class);
        BenchmarkJobRegistry benchmarkJobRegistry = new BenchmarkJobRegistry();
        DocumentMaintenanceService documentMaintenanceService = mock(DocumentMaintenanceService.class);
        BenchmarkCostEstimator benchmarkCostEstimator = mock(BenchmarkCostEstimator.class);

        when(testSetLoader.loadTestCases()).thenReturn(List.of(
                new TestCase("Q001", "Cau hoi fine-tune", "Dap an fine-tune", "DEFINITION")
        ));
        when(fineTunedInferenceService.generateAnswer("Cau hoi fine-tune"))
                .thenReturn("Tra loi tu model fine-tuned");
        when(evaluationService.evaluateDetailed(any(), any(), any(), any())).thenReturn(
                new RagasEvaluationService.EvaluationDetails(
                        new EvaluationResult(0.8, 0.8, 0.8, 0.8, 0.0, 0.0),
                        true,
                        "local-fallback",
                        "LOCAL",
                        false,
                        55L
                )
        );
        when(benchmarkCostEstimator.estimateFineTuneCost(any(), any())).thenReturn(BigDecimal.ZERO);

        BenchmarkRunnerService service = new BenchmarkRunnerService(
                testSetLoader,
                evaluationService,
                vectorStoreService,
                llmInferenceService,
                fineTunedInferenceService,
                embeddingRouter,
                benchmarkResultRepository,
                benchmarkJobRegistry,
                documentMaintenanceService,
                benchmarkCostEstimator
        );

        benchmarkJobRegistry.register("job-ft", "test", 1);
        service.runBenchmark("job-ft", new BenchmarkRunnerService.BenchmarkConfig(
                "SEMANTIC",
                "GEMINI_EMBEDDING_001",
                "FINETUNE"
        ));

        verify(fineTunedInferenceService).generateAnswer("Cau hoi fine-tune");
        verify(vectorStoreService, never()).search(any(), any(), anyInt(), any(), any(), any(), any());
        verify(embeddingRouter, never()).embed(any(), any());
        verify(llmInferenceService, never()).generateAnswer(any(), any(), any());
        verify(documentMaintenanceService, never()).hasIndexedDocumentsForBenchmark(any(), any());

        ArgumentCaptor<BenchmarkResult> resultCaptor = ArgumentCaptor.forClass(BenchmarkResult.class);
        verify(benchmarkResultRepository).save(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getExperimentType().name()).isEqualTo("FINETUNE");
        assertThat(resultCaptor.getValue().getRetrievalHit()).isFalse();
        assertThat(resultCaptor.getValue().getChunkingStrategy()).isNull();
        assertThat(resultCaptor.getValue().getEmbeddingModel()).isNull();
        assertThat(resultCaptor.getValue().getEvaluationFallbackUsed()).isTrue();
    }
}
