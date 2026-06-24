package com.ragchatbot.application.usecase.evaluation;

import com.ragchatbot.application.dto.evaluation.EvaluationRequest;
import com.ragchatbot.application.dto.evaluation.EvaluationResponse;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.enums.ExperimentType;
import com.ragchatbot.domain.model.BenchmarkResult;
import com.ragchatbot.infrastructure.benchmark.RagasEvaluationService;
import com.ragchatbot.infrastructure.persistence.BenchmarkResultRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EvaluateUseCase {

    private final RagasEvaluationService ragasEvaluationService;
    private final BenchmarkResultRepository benchmarkResultRepository;

    public EvaluateUseCase(
            RagasEvaluationService ragasEvaluationService,
            BenchmarkResultRepository benchmarkResultRepository
    ) {
        this.ragasEvaluationService = ragasEvaluationService;
        this.benchmarkResultRepository = benchmarkResultRepository;
    }

    public EvaluationResponse execute(EvaluationRequest request) {
        List<String> contexts = request.contexts() == null ? List.of() : List.copyOf(request.contexts());
        RagasEvaluationService.EvaluationDetails details = ragasEvaluationService.evaluateDetailed(
                request.question(),
                request.groundTruth(),
                request.answer(),
                contexts
        );

        BenchmarkResult result = new BenchmarkResult();
        result.setExperimentType(parseExperimentType(request.experimentType()));
        result.setChunkingStrategy(parseChunkingStrategy(request.chunkingStrategy()));
        result.setEmbeddingModel(parseEmbeddingModel(request.embeddingModel()));
        result.setQuestion(request.question());
        result.setGroundTruth(request.groundTruth());
        result.setGeneratedAnswer(request.answer());
        result.setExactMatch(details.result().exactMatch());
        result.setF1Score(details.result().f1());
        result.setFaithfulness(details.result().faithfulness());
        result.setAnswerRelevancy(details.result().answerRelevancy());
        result.setContextPrecision(details.result().contextPrecision());
        result.setContextRecall(details.result().contextRecall());
        result.setLatencyMs(details.latencyMs());
        result.setCostUsd(BigDecimal.ZERO);

        BenchmarkResult saved = benchmarkResultRepository.save(result);
        return new EvaluationResponse(
                saved.getId(),
                details.fallbackUsed(),
                details.source(),
                details.latencyMs(),
                details.result().exactMatch(),
                details.result().f1(),
                details.result().faithfulness(),
                details.result().answerRelevancy(),
                details.result().contextPrecision(),
                details.result().contextRecall()
        );
    }

    private ExperimentType parseExperimentType(String value) {
        if (!StringUtils.hasText(value)) {
            return ExperimentType.RAG;
        }
        return ExperimentType.valueOf(value.trim().toUpperCase());
    }

    private ChunkingStrategy parseChunkingStrategy(String value) {
        if (!StringUtils.hasText(value)) {
            return ChunkingStrategy.SEMANTIC;
        }
        return ChunkingStrategy.valueOf(value.trim().toUpperCase());
    }

    private EmbeddingModel parseEmbeddingModel(String value) {
        if (!StringUtils.hasText(value)) {
            return EmbeddingModel.GEMINI_EMBEDDING_001;
        }
        return EmbeddingModel.valueOf(value.trim().toUpperCase());
    }
}
