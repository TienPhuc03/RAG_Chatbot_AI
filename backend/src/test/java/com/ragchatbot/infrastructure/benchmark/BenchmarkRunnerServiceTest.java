package com.ragchatbot.infrastructure.benchmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ragchatbot.application.usecase.document.DocumentMaintenanceService;
import com.ragchatbot.domain.model.EvaluationResult;
import com.ragchatbot.domain.model.TestCase;
import com.ragchatbot.domain.port.EmbeddingService;
import com.ragchatbot.domain.port.EvaluationService;
import com.ragchatbot.domain.port.LlmAnswer;
import com.ragchatbot.domain.port.LlmInferenceService;
import com.ragchatbot.domain.port.RetrievedContext;
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.persistence.BenchmarkResultRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BenchmarkRunnerServiceTest {

    @Test
    void updatesJobStatusUntilCompleted() {
        TestSetLoader testSetLoader = mock(TestSetLoader.class);
        EvaluationService evaluationService = mock(EvaluationService.class);
        VectorStoreService vectorStoreService = mock(VectorStoreService.class);
        LlmInferenceService llmInferenceService = mock(LlmInferenceService.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        BenchmarkResultRepository benchmarkResultRepository = mock(BenchmarkResultRepository.class);
        BenchmarkJobRegistry benchmarkJobRegistry = new BenchmarkJobRegistry();
        DocumentMaintenanceService documentMaintenanceService = mock(DocumentMaintenanceService.class);

        when(testSetLoader.loadTestCases()).thenReturn(List.of(
                new TestCase("Q001", "Cau hoi", "Dap an", "DEFINITION")
        ));
        when(embeddingService.embed("Cau hoi")).thenReturn(List.of(0.1f, 0.2f));
        when(vectorStoreService.search(any(), anyInt(), any(), any())).thenReturn(List.of(
                new RetrievedContext(UUID.randomUUID(), UUID.randomUUID(), "Dap an", 0.9, "DB101", "CH1")
        ));
        when(llmInferenceService.generateAnswer(any(), any(), any())).thenReturn(
                new LlmAnswer("Dap an", List.of("c1"), true)
        );
        when(documentMaintenanceService.hasIndexedDocuments(null, null)).thenReturn(true);
        when(evaluationService.evaluate(any(), any(), any(), any())).thenReturn(
                new EvaluationResult(1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        );

        BenchmarkRunnerService service = new BenchmarkRunnerService(
                testSetLoader,
                evaluationService,
                vectorStoreService,
                llmInferenceService,
                embeddingService,
                benchmarkResultRepository,
                benchmarkJobRegistry,
                documentMaintenanceService
        );

        benchmarkJobRegistry.register("job-1", "test", 1);
        service.runBenchmark("job-1", new BenchmarkRunnerService.BenchmarkConfig(
                "SEMANTIC",
                "GEMINI_EMBEDDING_001",
                "RAG"
        ));

        BenchmarkJobRegistry.JobSnapshot snapshot = benchmarkJobRegistry.get("job-1");
        assertThat(snapshot.status()).isEqualTo(BenchmarkJobStatus.COMPLETED);
        assertThat(snapshot.doneCases()).isEqualTo(1);
        verify(benchmarkResultRepository).save(any());
    }
}
