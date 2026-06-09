package com.ragchatbot.infrastructure.persistence;

import com.ragchatbot.domain.model.Citation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CitationRepository extends JpaRepository<Citation, UUID> {
}