package com.ragchatbot.application.dto.evaluation;

import java.util.UUID;

public record EvaluationResponse(
        UUID benchmarkResultId,
        boolean fallbackUsed,
        String source,
        long latencyMs,
        double exactMatch,
        double f1Score,
        double faithfulness,
        double answerRelevancy,
        double contextPrecision,
        double contextRecall
) {
}
