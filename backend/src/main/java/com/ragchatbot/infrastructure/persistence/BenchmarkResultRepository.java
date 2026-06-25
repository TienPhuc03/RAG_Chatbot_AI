package com.ragchatbot.infrastructure.persistence;

import com.ragchatbot.application.dto.benchmark.BenchmarkSummaryDto;
import com.ragchatbot.domain.model.BenchmarkResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface BenchmarkResultRepository extends JpaRepository<BenchmarkResult, UUID> {

    /**
     * Tổng hợp kết quả benchmark theo nhóm (chunkingStrategy × embeddingModel × experimentType).
     *
     * Mỗi hàng trả về một BenchmarkSummaryDto chứa trung bình 6 metrics, số lần chạy,
     * và trung bình latency — dùng cho endpoint GET /api/benchmark/results.
     */
    @Query("""
            SELECT new com.ragchatbot.application.dto.benchmark.BenchmarkSummaryDto(
                CAST(b.chunkingStrategy AS string),
                CAST(b.embeddingModel   AS string),
                CAST(b.experimentType   AS string),
                COUNT(b),
                AVG(b.exactMatch),
                AVG(b.f1Score),
                AVG(b.faithfulness),
                AVG(b.answerRelevancy),
                AVG(b.contextPrecision),
                AVG(b.contextRecall),
                AVG(CAST(b.latencyMs AS double))
            )
            FROM BenchmarkResult b
            GROUP BY b.chunkingStrategy, b.embeddingModel, b.experimentType
            ORDER BY b.chunkingStrategy, b.embeddingModel
            """)
    List<BenchmarkSummaryDto> findAverageMetricsByStrategyAndModel();
}

