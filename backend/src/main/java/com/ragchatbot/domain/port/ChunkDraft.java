package com.ragchatbot.domain.port;

public record ChunkDraft(
    int chunkIndex,
    String content,
    Integer pageNumber,
    Integer tokenCount,
    Integer parentChunkId
    ) {
        public ChunkDraft(int chunkIndex, String content, Integer pageNumber, Integer tokenCount) {
        this(chunkIndex, content, pageNumber, tokenCount, null);
    }
}
