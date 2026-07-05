package com.ragchatbot.application.dto.document;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;

public record DocumentChunkPreviewResponse(
        int chunkIndex,
        Integer pageNumber,
        Integer tokenCount,
        ChunkingStrategy chunkingStrategy,
        EmbeddingModel embeddingModel,
        String content
) {
}
