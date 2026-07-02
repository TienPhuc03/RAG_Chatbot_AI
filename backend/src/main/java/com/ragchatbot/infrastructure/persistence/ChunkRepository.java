package com.ragchatbot.infrastructure.persistence;

import com.ragchatbot.domain.model.Chunk;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChunkRepository extends JpaRepository<Chunk, UUID> {

    List<Chunk> findByDocumentId(UUID documentId);

    List<Chunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);

    long countByDocumentId(UUID documentId);

    Optional<Chunk> findTopByDocumentIdOrderByChunkIndexAsc(UUID documentId);
}
