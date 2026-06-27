package com.ragchatbot.application.dto.benchmark;

/**
 * DTO tổng hợp kết quả benchmark theo nhóm (strategy × embeddingModel).
 *
 * Được trả về từ GET /api/benchmark/results để dashboard hiển thị bảng so sánh.
 *
 * @param chunkingStrategy  Tên chiến lược chunking (FIXED_SIZE / SEMANTIC / HIERARCHICAL)
 * @param embeddingModel    Tên embedding model sử dụng
 * @param experimentType    Loại experiment (RAG / FINETUNE)
 * @param runCount          Tổng số BenchmarkResult trong nhóm này
 * @param avgExactMatch     Trung bình Exact Match
 * @param avgF1Score        Trung bình F1 Score
 * @param avgFaithfulness   Trung bình Faithfulness (RAGAS)
 * @param avgAnswerRelevancy Trung bình Answer Relevancy (RAGAS)
 * @param avgContextPrecision Trung bình Context Precision (RAGAS)
 * @param avgContextRecall  Trung bình Context Recall (RAGAS)
 * @param avgLatencyMs      Trung bình latency mỗi câu hỏi (ms)
 */
public record BenchmarkSummaryDto(
        String chunkingStrategy,
        String embeddingModel,
        String experimentType,
        long runCount,
        Double avgExactMatch,
        Double avgF1Score,
        Double avgFaithfulness,
        Double avgAnswerRelevancy,
        Double avgContextPrecision,
        Double avgContextRecall,
        Double avgLatencyMs
) {}
