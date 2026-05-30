package com.ragchatbot.domain.model;

public record EvaluationResult(
        double exactMatch,
        double f1,
        double faithfulness,
        double answerRelevancy,
        double contextPrecision,
        double contextRecall
) {}