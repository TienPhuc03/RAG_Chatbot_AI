package com.ragchatbot.domain.model;

public record RetrievalMetrics(
    double hitAtK,
    double recallAtK,
    double precisionAtK,
    double reciprocalRank,
    double ndcgAtK,
    int relevantRetrieved,
    int totalRelevant,
    Integer firstRelevantRank
) {}