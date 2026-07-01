package com.ragchatbot.domain.port;

import java.util.List;
import java.util.UUID;

public interface VectorStoreService {

    void upsert(UUID documentId, List<ChunkDraft> chunks, List<List<Float>> embeddings);

    List<RetrievedContext> search(List<Float> queryEmbedding, int topK, String courseCode, String chapterCode);

    /**
     * Tìm kiếm trong collection cụ thể (dùng cho benchmark với collection động của Gia Bảo).
     * Mặc định delegate sang search() với collection mặc định nếu collectionName null/blank.
     */
    default List<RetrievedContext> search(List<Float> queryEmbedding, int topK,
                                          String courseCode, String chapterCode,
                                          String collectionName) {
        return search(queryEmbedding, topK, courseCode, chapterCode);
    }

    void deleteByDocumentId(UUID documentId);
}
