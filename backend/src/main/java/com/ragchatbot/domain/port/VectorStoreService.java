package com.ragchatbot.domain.port;

import java.util.List;
import java.util.UUID;

public interface VectorStoreService {

    void upsert(UUID documentId, List<ChunkDraft> chunks, List<List<Float>> embeddings);

    List<RetrievedContext> search(
            List<Float> queryEmbedding,
            int topK,
            String courseCode,
            String chapterCode,
            String conversationSessionId
    );

    void deleteByDocumentId(UUID documentId);
}
