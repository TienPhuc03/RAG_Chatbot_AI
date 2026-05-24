package com.ragchatbot.infrastructure.persistence;

import com.ragchatbot.domain.model.Document;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
}
