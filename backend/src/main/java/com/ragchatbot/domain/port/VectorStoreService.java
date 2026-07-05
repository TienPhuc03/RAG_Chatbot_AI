package com.ragchatbot.domain.port;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import java.util.List;
import java.util.UUID;

public interface VectorStoreService {

    void upsert(
            UUID documentId,
            EmbeddingModel embeddingModel,
            ChunkingStrategy chunkingStrategy,
            List<ChunkDraft> chunks,
            List<List<Float>> embeddings
    );

    List<RetrievedContext> search(
            EmbeddingModel embeddingModel,
            List<Float> queryEmbedding,
            int topK,
            ChunkingStrategy chunkingStrategy,
            String courseCode,
            String chapterCode,
            String conversationSessionId
    );

    void deleteByDocumentId(UUID documentId, EmbeddingModel embeddingModel);
}
