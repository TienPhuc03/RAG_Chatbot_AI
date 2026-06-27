package com.ragchatbot.application.usecase.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ragchatbot.application.dto.evaluation.EvaluationRequest;
import com.ragchatbot.application.dto.evaluation.EvaluationResponse;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.enums.ExperimentType;
import com.ragchatbot.domain.model.BenchmarkResult;
import com.ragchatbot.domain.model.EvaluationResult;
import com.ragchatbot.infrastructure.benchmark.RagasEvaluationService;
import com.ragchatbot.infrastructure.persistence.BenchmarkResultRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EvaluateUseCaseTest {

    @Test
    void executePersistsBenchmarkResultWithDefaultMetadata() {
        RagasEvaluationService ragasEvaluationService = mock(RagasEvaluationService.class);
        BenchmarkResultRepository repository = mock(BenchmarkResultRepository.class);

        when(ragasEvaluationService.evaluateDetailed(any(), any(), any(), any()))
                .thenReturn(new RagasEvaluationService.EvaluationDetails(
                        new EvaluationResult(1.0, 0.8, 0.6, 0.5, 0.4, 0.3),
                        true,
                        "local-fallback",
                        123L
                ));
        when(repository.save(any())).thenAnswer(invocation -> {
            BenchmarkResult result = invocation.getArgument(0);
            result.setId(UUID.randomUUID());
            return result;
        });

        EvaluateUseCase useCase = new EvaluateUseCase(ragasEvaluationService, repository);
        EvaluationResponse response = useCase.execute(new EvaluationRequest(
                "Question?",
                "Ground truth",
                "Answer",
                List.of("Context"),
                null,
                null,
                null
        ));

        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.source()).isEqualTo("local-fallback");
        assertThat(response.exactMatch()).isEqualTo(1.0);
        assertThat(response.f1Score()).isEqualTo(0.8);
        assertThat(response.faithfulness()).isEqualTo(0.6);
        assertThat(response.contextRecall()).isEqualTo(0.3);

        ArgumentCaptor<BenchmarkResult> captor = ArgumentCaptor.forClass(BenchmarkResult.class);
        verify(repository).save(captor.capture());
        BenchmarkResult saved = captor.getValue();
        assertThat(saved.getExperimentType()).isEqualTo(ExperimentType.RAG);
        assertThat(saved.getChunkingStrategy()).isEqualTo(ChunkingStrategy.SEMANTIC);
        assertThat(saved.getEmbeddingModel()).isEqualTo(EmbeddingModel.GEMINI_EMBEDDING_001);
        assertThat(saved.getCostUsd()).isNotNull();
    }
}
