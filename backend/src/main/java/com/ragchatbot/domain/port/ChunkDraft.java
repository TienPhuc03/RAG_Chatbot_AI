package com.ragchatbot.domain.port;

public record ChunkDraft(int chunkIndex, String content, Integer pageNumber, Integer tokenCount) {
}
