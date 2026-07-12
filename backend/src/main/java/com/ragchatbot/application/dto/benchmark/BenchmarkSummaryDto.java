package com.ragchatbot.application.dto.benchmark;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.enums.ExperimentType;

/**
 * DTO tổng hợp kết quả benchmark theo nhóm.
 */
public record BenchmarkSummaryDto(
        ChunkingStrategy chunkingStrategy,
        EmbeddingModel embeddingModel,
        ExperimentType experimentType,

        Long runCount,
        Long fallbackRunCount,
        Long ragasRunCount,
        Long geminiJudgeRunCount,
        Long ollamaJudgeRunCount,

        Double avgExactMatch,
        Double avgF1Score,
        Double avgFaithfulness,
        Double avgAnswerRelevancy,
        Double avgContextPrecision,
        Double avgContextRecall,
        Double retrievalHitRate,
        Double avgLatencyMs,
        Double avgCostUsd
) {
}