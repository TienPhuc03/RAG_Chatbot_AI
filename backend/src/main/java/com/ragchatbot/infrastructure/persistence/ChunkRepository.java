package com.ragchatbot.infrastructure.persistence;

import com.ragchatbot.domain.model.Chunk;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChunkRepository extends JpaRepository<Chunk, UUID> {
}
