package com.ragchatbot.infrastructure.persistence;

import com.ragchatbot.domain.model.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChunkRepository
        extends JpaRepository<Chunk, UUID> {

    List<Chunk> findByDocumentId(UUID documentId);
}