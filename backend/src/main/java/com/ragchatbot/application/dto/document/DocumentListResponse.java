package com.ragchatbot.application.dto.document;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.DocumentStatus;
import com.ragchatbot.domain.enums.EmbeddingModel;
import java.time.Instant;
import java.util.UUID;

public record DocumentListResponse(
        UUID id,
        String title,
        String sourceFileName,
        String courseCode,
        String courseName,
        String chapterCode,
        String chapterTitle,
        DocumentStatus status,
        String failureReason,
        Long chunkCount,
        ChunkingStrategy latestChunkingStrategy,
        EmbeddingModel embeddingModel,
        Instant indexedAt,
        Instant createdAt
) {
}
