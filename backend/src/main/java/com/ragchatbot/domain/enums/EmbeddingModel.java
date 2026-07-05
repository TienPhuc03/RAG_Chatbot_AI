package com.ragchatbot.domain.enums;

public enum EmbeddingModel {
    MULTILINGUAL_E5_BASE(true),
    TEXT_EMBEDDING_3_SMALL(false),
    PHOBERT_BASE(true),
    BGE_M3(true),
    GEMINI_EMBEDDING_001(true);

    private final boolean allowedForNewRequests;

    EmbeddingModel(boolean allowedForNewRequests) {
        this.allowedForNewRequests = allowedForNewRequests;
    }

    public boolean isAllowedForNewRequests() {
        return allowedForNewRequests;
    }
}
