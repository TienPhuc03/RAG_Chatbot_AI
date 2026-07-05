package com.ragchatbot.infrastructure.persistence;

import com.ragchatbot.domain.model.Chunk;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChunkRepository extends JpaRepository<Chunk, UUID> {

    List<Chunk> findByDocumentId(UUID documentId);

    List<Chunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);

    long countByDocumentId(UUID documentId);

    Optional<Chunk> findTopByDocumentIdOrderByChunkIndexAsc(UUID documentId);

    @Query("""
            SELECT COUNT(DISTINCT c.document.id)
            FROM Chunk c
            WHERE c.chunkingStrategy = :chunkingStrategy
              AND c.embeddingModel = :embeddingModel
              AND c.document.status = com.ragchatbot.domain.enums.DocumentStatus.INDEXED
              AND c.document.conversationSessionId IS NULL
            """)
    long countIndexedPublicDocumentsForBenchmark(
            @Param("chunkingStrategy") ChunkingStrategy chunkingStrategy,
            @Param("embeddingModel") EmbeddingModel embeddingModel
    );

    @Query("""
            SELECT DISTINCT c.embeddingModel
            FROM Chunk c
            WHERE c.document.id = :documentId
            """)
    List<EmbeddingModel> findDistinctEmbeddingModelsByDocumentId(@Param("documentId") UUID documentId);
}
