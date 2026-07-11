package com.ragchatbot.infrastructure.persistence;

import com.ragchatbot.application.dto.benchmark.BenchmarkSummaryDto;
import com.ragchatbot.domain.model.BenchmarkResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface BenchmarkResultRepository extends JpaRepository<BenchmarkResult, UUID> {

    @Query("SELECT new com.ragchatbot.application.dto.benchmark.BenchmarkSummaryDto(" +
           "b.chunkingStrategy, b.embeddingModel, b.experimentType, " +
           "COUNT(b), " +
           "SUM(CASE WHEN b.evaluationFallbackUsed = true THEN 1 ELSE 0 END), " + // fallbackRunCount
           "SUM(CASE WHEN b.evaluationSource = 'ragas' THEN 1 ELSE 0 END), " +      // ragasRunCount (kiểm tra lại tên source này)
           "SUM(CASE WHEN b.evaluationSource = 'ragas-service:gemini' THEN 1 ELSE 0 END), " + // geminiJudgeRunCount
           "SUM(CASE WHEN b.evaluationSource = 'ragas-service:ollama' THEN 1 ELSE 0 END), " + // ollamaJudgeRunCount
           "AVG(b.exactMatch), AVG(b.f1Score), AVG(b.faithfulness), " +
           "AVG(b.answerRelevancy), AVG(b.contextPrecision), AVG(b.contextRecall), " +
           "AVG(CASE WHEN b.retrievalHit = true THEN 1.0 ELSE 0.0 END), " +
           "AVG(b.latencyMs), AVG(b.costUsd)) " +
           "FROM BenchmarkResult b " +
           "GROUP BY b.chunkingStrategy, b.embeddingModel, b.experimentType")
    List<BenchmarkSummaryDto> findAverageMetricsByStrategyAndModel();

    @Query("SELECT new com.ragchatbot.application.dto.benchmark.BenchmarkSummaryDto(" +
           "b.chunkingStrategy, b.embeddingModel, b.experimentType, " +
           "COUNT(b), " +
           "SUM(CASE WHEN b.evaluationFallbackUsed = true THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN b.evaluationSource = 'ragas' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN b.evaluationSource = 'ragas-service:gemini' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN b.evaluationSource = 'ragas-service:ollama' THEN 1 ELSE 0 END), " +
           "AVG(b.exactMatch), AVG(b.f1Score), AVG(b.faithfulness), " +
           "AVG(b.answerRelevancy), AVG(b.contextPrecision), AVG(b.contextRecall), " +
           "AVG(CASE WHEN b.retrievalHit = true THEN 1.0 ELSE 0.0 END), " +
           "AVG(b.latencyMs), AVG(b.costUsd)) " +
           "FROM BenchmarkResult b " +
           "WHERE b.runId = :runId " +
           "GROUP BY b.chunkingStrategy, b.embeddingModel, b.experimentType")
    List<BenchmarkSummaryDto> findFinalMetricsByRunId(@Param("runId") String runId);

    boolean existsByRunIdAndQuestionIdAndConfigKeyAndItemStatus(String runId, String questionId, String configKey, String itemStatus);
}