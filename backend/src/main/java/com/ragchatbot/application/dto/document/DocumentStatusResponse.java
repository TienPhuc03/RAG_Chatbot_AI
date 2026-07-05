package com.ragchatbot.application.dto.document;

import com.ragchatbot.domain.enums.DocumentStatus;
import com.ragchatbot.domain.enums.EmbeddingModel;
import java.time.Instant;
import java.util.UUID;

public record DocumentStatusResponse(
        UUID id,
        DocumentStatus status,
        Instant indexedAt,
        String failureReason,
        EmbeddingModel embeddingModel
) {
}
