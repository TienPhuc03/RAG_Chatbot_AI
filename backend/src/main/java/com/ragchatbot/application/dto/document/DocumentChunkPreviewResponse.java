package com.ragchatbot.application.dto.document;

import com.ragchatbot.domain.enums.ChunkingStrategy;

public record DocumentChunkPreviewResponse(
        int chunkIndex,
        Integer pageNumber,
        Integer tokenCount,
        ChunkingStrategy chunkingStrategy,
        String content
) {
}
